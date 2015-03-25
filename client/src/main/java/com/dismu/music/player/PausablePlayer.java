package com.dismu.music.player;

import com.dismu.logging.Loggers;
import com.dismu.music.events.PlayerEvent;
import com.dismu.utils.events.Event;
import com.dismu.utils.events.EventListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.sound.sampled.*;

public class PausablePlayer {
    public final static int NOT_STARTED = 0;
    public final static int PLAYING = 1;
    public final static int PAUSED = 2;
    public final static int FINISHED = 3;
    public final static int FULL_STOP = 4;

    private volatile int playerStatus;
    private int bufferSize = 4096 * 4;
    private SourceDataLine playerLine;
    private AudioInputStream currentStream;
    private Thread playerThread;

    private ArrayList<EventListener> listeners = new ArrayList<>();
    private final Object playerLock;

    public PausablePlayer() {
        playerStatus = NOT_STARTED;
        playerLock = new Object();
        playerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                playerInternal();
            }
        });
        playerThread.setName("playerInternalThread");
        playerThread.start();
    }

    public void loadInputStream(InputStream inputStream) {
        synchronized (playerLock) {
            if (playerLine != null) {
                playerLine.stop();
                playerLine.close();
                Loggers.playerLogger.info("line stopped");
            }
            try {
                if (currentStream != null) {
                    currentStream.close();
                }
            } catch (IOException e) {
                Loggers.playerLogger.error("i/o error", e);
            }
            try {
                currentStream = AudioSystem.getAudioInputStream(inputStream);
                AudioFormat decodedFormat = getDecodedFormat(currentStream.getFormat());
                playerLine = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, decodedFormat));
                currentStream = AudioSystem.getAudioInputStream(decodedFormat, currentStream);
                Loggers.playerLogger.debug("current stream = {}", currentStream);
                playerLine.open(decodedFormat, bufferSize);
                playerLine.start();
                Loggers.playerLogger.info("line opened and started");
            } catch (UnsupportedAudioFileException e) {
                Loggers.playerLogger.error("unsupported format", e);
                return;
            } catch (IOException e) {
                Loggers.playerLogger.error("i/o exception", e);
                return;
            } catch (LineUnavailableException e) {
                Loggers.playerLogger.error("cannot create line", e);
                return;
            }
            playerLock.notifyAll();
            Loggers.playerLogger.debug("set new track");
        }
    }

    private AudioFormat getDecodedFormat(AudioFormat baseFormat) {
        float rate = baseFormat.getSampleRate();
        int channels = baseFormat.getChannels();
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, channels, channels * 2, rate, false);
    }

    private void notify(int eventType) {
        Event event = new PlayerEvent(eventType);
        for (EventListener listener : listeners) {
            listener.dispatchEvent(event);
        }
    }

    public long getPosition() {
        if (playerLine == null) {
            return 0;
        }
        return playerLine.getMicrosecondPosition();
    }

    public void addEventListener(EventListener listener) {
        listeners.add(listener);
    }

    public void removeEventListener(EventListener listener) {
        listeners.remove(listener);
    }

    public boolean play() {
        synchronized (playerLock) {
            if (playerLine == null) {
                Loggers.playerLogger.error("play on non-initialized player");
                return false;
            }
            playerStatus = PLAYING;
            playerLock.notifyAll();
            notify(PlayerEvent.PLAYING);
            return playerStatus == PLAYING;
        }
    }

    public void seek(double st) {
        st *= 1000.0;
        synchronized (playerLock) {
            if (playerLine == null) {
                Loggers.playerLogger.error("pause on non-initialized player");
                return;
            }
            playerLine.notifyAll();
        }
    }

    public boolean pause() {
        synchronized (playerLock) {
            if (playerLine == null) {
                Loggers.playerLogger.error("play on non-initialized player");
                return false;
            }
            playerStatus = PAUSED;
            playerLock.notifyAll();
            notify(PlayerEvent.PAUSED);
            return playerStatus == PAUSED;
        }
    }

    public void stop() {
        synchronized (playerLock) {
            if (playerLine != null) {
                playerLine.stop();
                playerLine.close();
            }
            playerStatus = FINISHED;
            playerLock.notifyAll();
            notify(PlayerEvent.STOPPED);
        }
    }

    public void close() {
        synchronized (playerLock) {
            playerStatus = FULL_STOP;
            if (playerLine != null) {
                playerLine.stop();
                playerLine.close();
            }
            playerLock.notifyAll();
        }
    }

    private void playerInternal() {
        byte[] data = new byte[bufferSize];
        int readBytes = 0, writtenBytes = 0;
        while (playerStatus != FULL_STOP) {
            try {
                if (playerStatus == PLAYING) {
                    try {
                        readBytes = currentStream.read(data, 0, data.length);
                    } catch (IOException e) {
                        Loggers.playerLogger.error("read error", e);
                        playerLine.drain();
                        stop();
                    }
                    if (readBytes == -1) {
                        playerLine.drain();
                        playerLine.stop();
                        playerLine.close();
                        notify(PlayerEvent.FINISHED);
                    } else if (readBytes > 0) {
                        writtenBytes = playerLine.write(data, 0, readBytes);
                        notify(PlayerEvent.FRAME_PLAYED);
                    }
                }
                Thread.sleep(10);
            } catch (Exception e) {
                Loggers.playerLogger.error("exception in playerInternal", e);
            }
        }
    }

    public int getState() {
        return playerStatus;
    }
}
