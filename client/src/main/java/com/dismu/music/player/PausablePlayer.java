package com.dismu.music.player;

import com.dismu.logging.Loggers;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;

import java.io.InputStream;
import java.io.InterruptedIOException;

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
//        Loggers.playerLogger.debug("{}", 1000.0 / decoder.getOutputFrequency());
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

            SampleBuffer output = (SampleBuffer)decoder.decodeFrame(header, bitstream);

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
    public final static int SEEKING = 4;
    public final static int SOUGHT = 5;

    private final MyPlayer player;
    private final Object playerLock = new Object();
    private int playerStatus = NOT_STARTED;
    private double seekTo = 0;

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

    public void play() throws JavaLayerException {
        Loggers.playerLogger.debug("play status={}", playerStatus);
        synchronized (playerLock) {
            switch (playerStatus) {
                case PAUSED:
                    resume();
                    break;
                default:
                    playerStatus = PLAYING;
                    break;
            }
        }
    }

    public void seek(double st) {
        int lastStatus = playerStatus;
        synchronized (playerLock) {
            playerStatus = SEEKING;
            seekTo = st * 1000;
            playerLock.notifyAll();
        }
        Loggers.playerLogger.debug("{}", playerStatus);
        while (playerStatus != SOUGHT) {
        }
        Loggers.playerLogger.debug("{}", playerStatus);
    }

    public boolean pause() {
        synchronized (playerLock) {
            if (playerStatus == PLAYING) {
                playerStatus = PAUSED;
            }
            return playerStatus == PAUSED;
        }
    }

    public boolean resume() {
        synchronized (playerLock) {
            if (playerStatus == PAUSED) {
                playerStatus = PLAYING;
                playerLock.notifyAll();
            }
            return playerStatus == PLAYING;
        }
    }

    public void stop() {
        synchronized (playerLock) {
            playerStatus = FINISHED;
            playerLock.notifyAll();
        }
    }

    private void playInternal() {
        while (playerStatus != FINISHED) {
            try {
                if (playerStatus == PLAYING) {
                    if (!player.play(1)) {
                        playerStatus = FINISHED;
                    }
                } else if (playerStatus == SEEKING) {
                    if (player.getPosition() >= seekTo) {
                        playerStatus = SOUGHT;
                    } else {
                        player.skipFrame();
                    }
                }
            } catch (final JavaLayerException e) {
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
        close();
    }

    public void close() {
        synchronized (playerLock) {
            playerStatus = FINISHED;
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