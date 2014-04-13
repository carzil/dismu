package com.dismu.music.player;

import com.dismu.logging.Loggers;
import com.dismu.utils.events.Event;
import com.dismu.utils.events.EventListener;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;

import java.io.InputStream;
import java.util.ArrayList;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

class MyPlayer {
    private Bitstream bitstream;
    private Decoder decoder;
    private AudioDevice audio;
    private boolean closed = false;
    private boolean complete = false;
    private boolean isInited = false;
    private int lastPosition = 0;
    private int currentFrame = -1;
    private ArrayList<SampleBuffer> frames = new ArrayList<>();


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
//        return currentFrame * (1000.0 / decoder.getOutputFrequency()) * 1152.0;
        return audio.getPosition();
    }

    public boolean playFrame() throws JavaLayerException {
        try {
            SampleBuffer buffer = nextFrame();
            if (buffer == null) {
                audio.flush();
                return false;
            }
            synchronized (this) {
                if (audio != null) {
                    audio.write(buffer.getBuffer(), 0, buffer.getBufferLength());
                }
            }
        } catch (RuntimeException ex) {
            throw new JavaLayerException("Exception decoding audio frame", ex);
        }
        return true;
    }

    public SampleBuffer nextFrame() throws JavaLayerException {
        currentFrame++;
        if (currentFrame + 1 >= frames.size()) {
            return readFrame();
        } else {
            return frames.get(currentFrame);
        }
    }

    public SampleBuffer readFrame() throws JavaLayerException {
        try {
            if (audio == null) {
                return null;
            }

            Header header = bitstream.readFrame();

            if (header == null) {
                return null;
            }

            SampleBuffer output = (SampleBuffer)decoder.decodeFrame(header, bitstream);
            frames.add(output);
            isInited = true;
            bitstream.closeFrame();
            currentFrame++;
            return output;
        } catch (RuntimeException ex) {
            throw new JavaLayerException("Exception decoding audio frame", ex);
        }
    }

    public SampleBuffer prevFrame() {
        if (currentFrame > 0) {
            currentFrame--;
            return frames.get(currentFrame);
        }
        return null;
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

    public double getPosition() {
        return player.getPosition();
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
        st *= 1000.0;
        synchronized (playerLock) {
            if (Double.isNaN(player.getPosition()) || player.getPosition() < st) {
                while (Double.isNaN(player.getPosition()) || player.getPosition() < st) {
                    try {
                        player.nextFrame();
                    } catch (JavaLayerException e) {
                        Loggers.playerLogger.error("exception occurred while seeking", e);
                        return;
                    }
                }
            } else {
                Loggers.playerLogger.info("failed");
//                while (player.getPosition() > st) {
//                    player.prevFrame();
//                }
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
                    if (!player.playFrame()) {
                        playerStatus = FINISHED;
                        notify(PlayerEvent.FINISHED);
                    }
                    notify(PlayerEvent.FRAME_PLAYED);
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