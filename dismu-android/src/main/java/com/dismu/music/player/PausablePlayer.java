package com.dismu.music.player;

import com.dismu.logging.Loggers;
import com.dismu.utils.events.Event;
import com.dismu.utils.events.EventListener;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;

import java.awt.event.ActionListener;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayList;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javax.sound.sampled.Clip;

class MyPlayer {
    private Bitstream bitstream;
    private Decoder decoder;
    private AudioDevice audio;
    private boolean closed = false;
    private boolean complete = false;
    private boolean isInited = false;
    private int lastPosition = 0;
    private int framesConsumed = 0;

    public MyPlayer(InputStream stream) throws JavaLayerException {
        this(stream, null);
    }

    public MyPlayer(InputStream stream, AudioDevice device) throws JavaLayerException {
        bitstream = new Bitstream(stream);
        decoder = new Decoder();

        if (device != null) {
            audio = device;
        } else {
            FactoryRegistry r = FactoryRegistry.systemRegistry();
            audio = r.createAudioDevice();
        }
        audio.open(decoder);
    }

    public boolean play(int frames) throws JavaLayerException {
        boolean ret = true;

        while (frames-- > 0 && ret) {
            ret = decodeFrame();
        }

        if (!ret) {
            if (audio != null) {
                audio.flush();
                synchronized (this) {
                    complete = !closed;
                    close();
                }
            }
        }
        return ret;
    }

    public synchronized void close() {
        if (audio != null) {
            closed = true;
            audio.close();
            lastPosition = audio.getPosition();
            try {
                bitstream.close();
            } catch (BitstreamException ex) {

            }
            audio = null;
        }
    }

    public synchronized boolean isComplete() {
        return complete;
    }

    public double getPosition() {
        // where we can find sample per frame constant?
//        Loggers.playerLogger.debug("{}", decoder.getOutputFrequency());
        return framesConsumed * (1000.0 / decoder.getOutputFrequency()) * 1152.0;
    }

    public boolean decodeFrame() throws JavaLayerException {
        try {
            if (audio == null)
                return false;

            Header header = bitstream.readFrame();

            if (header == null) {
                return false;
            }

            SampleBuffer output = (SampleBuffer)decoder.decodeFrame(header, bitstream);
            // frames per second = output.getBufferLength() / output.getChannelCount()
            isInited = true;

            synchronized (this) {
                if (audio != null) {
                    audio.write(output.getBuffer(), 0, output.getBufferLength());
                }
            }

            bitstream.closeFrame();
            framesConsumed++;
        } catch (RuntimeException ex) {
            throw new JavaLayerException("Exception decoding audio frame", ex);
        }
        return true;
    }

    public boolean skipFrame() throws JavaLayerException {
        try {
            if (audio == null)
                return false;

            Header header = bitstream.readFrame();

            if (header == null) {
                return false;
            }

            if (!isInited) {
                decoder.decodeFrame(header, bitstream);
                isInited = true;
            }

            bitstream.closeFrame();
            framesConsumed++;
        } catch (RuntimeException ex) {
            throw new JavaLayerException("Exception decoding audio frame", ex);
        }
        return true;
    }
}

public class PausablePlayer {
    public final static int NOT_STARTED = 0;
    public final static int PLAYING = 1;
    public final static int PAUSED = 2;
    public final static int FINISHED = 3;
    public final static int FULL_STOP = 4;

    private MyPlayer player;
    private final Object playerLock = new Object();
    private int playerStatus = NOT_STARTED;

    private ArrayList<EventListener> listeners = new ArrayList<>();

    public PausablePlayer(final InputStream inputStream) throws JavaLayerException {
        this.player = new MyPlayer(inputStream);
        final Thread t = new Thread() {
            public void run() {
                playInternal();
            }
        };
        t.start();
    }

    public PausablePlayer(final InputStream inputStream, final AudioDevice audioDevice) throws JavaLayerException {
        this.player = new MyPlayer(inputStream, audioDevice);
        final Thread t = new Thread() {
            public void run() {
                playInternal();
            }
        };
        t.start();
    }

    public PausablePlayer() {
        final Thread t = new Thread() {
            public void run() {
                playInternal();
            }
        };
        t.start();
    }

    public void setInputStream(InputStream inputStream) throws JavaLayerException {
        this.player = new MyPlayer(inputStream);
    }

    private void notify(int eventType) {
        Event event = new PlayerEvent(eventType);
        for (EventListener listener : listeners) {
            listener.dispatchEvent(event);
        }
    }

    public void addEventListener(EventListener listener) {
        listeners.add(listener);
    }

    public void removeEventListener(EventListener listener) {
        listeners.remove(listener);
    }

    public void play() throws JavaLayerException {
        Loggers.playerLogger.debug("play status={}", playerStatus);
        synchronized (playerLock) {
            playerStatus = PLAYING;
            playerLock.notifyAll();
            notify(PlayerEvent.PLAYING);
        }
    }

    public void seek(double st) {
        synchronized (playerLock) {
            while (Double.isNaN(player.getPosition()) || player.getPosition() <= st * 1000.0) {
                try {
                    player.skipFrame();
                } catch (JavaLayerException e) {
                    Loggers.playerLogger.error("exception occurred while seeking", e);
                    return;
                }
            }
            playerLock.notifyAll();
        }
    }

    public boolean pause() {
        synchronized (playerLock) {
            playerStatus = PAUSED;
            playerLock.notifyAll();
            notify(PlayerEvent.PAUSED);
            return playerStatus == PAUSED;
        }
    }

    public void stop() {
        if (player != null) {
            synchronized (playerLock) {
                player.close();
                playerStatus = FINISHED;
                playerLock.notifyAll();
                notify(PlayerEvent.STOPPED);
            }
        }
    }

    private void playInternal() {
        while (playerStatus != FULL_STOP) {
            try {
                if (playerStatus == PLAYING) {
                    if (!player.play(1)) {
                        playerStatus = FINISHED;
                        notify(PlayerEvent.STOPPED);
                    }
                }
            } catch (JavaLayerException e) {
                Loggers.playerLogger.error("JLayer error", e);
                break;
            }

            synchronized (playerLock) {
                while (playerStatus == PAUSED) {
                    try {
                        playerLock.wait();
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
            }
        }
        Loggers.playerLogger.debug("finished player");
        close();
    }

    public void close() {
        synchronized (playerLock) {
            playerStatus = FULL_STOP;
        }
        try {
            player.close();
        } catch (final Exception e) {
        }
    }

    public int getState() {
        return this.playerStatus;
    }
}