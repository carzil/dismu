package com.dismu.music.player;

import com.dismu.logging.Loggers;
import com.dismu.music.Equalizer;
import com.dismu.music.events.PlayerEvent;
import com.dismu.utils.DynamicByteArray;
import com.dismu.utils.Utils;
import com.dismu.utils.events.Event;
import com.dismu.utils.events.EventListener;
import davaguine.jeq.core.EqualizerInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.sound.sampled.*;

public class PausablePlayer {
    public final static int NOT_STARTED = 0;
    public final static int PLAYING = 1;
    public final static int PAUSED = 2;
    public final static int FINISHED = 3;
    public final static int FULL_STOP = 4;

    private volatile int playerStatus;
    private int bufferSize = 4 * 4096;
    private volatile SourceDataLine playerLine;
    private volatile AudioInputStream currentStream;
    private volatile int currentPosition = 0;
    private Thread playerThread;
    private byte[] workBuf = new byte[bufferSize];
    private ByteArrayInputStream workBufStreamWrapper = new ByteArrayInputStream(workBuf);
    private EqualizerInputStream equalizerInputStream;

    private Lock playerLock = new ReentrantLock();
    private ArrayList<EventListener> listeners = new ArrayList<>();
    private volatile boolean isBuffering = false;
    private DynamicByteArray byteArray = new DynamicByteArray(50 * Utils.MEGABYTE);

    public PausablePlayer() {
        playerStatus = NOT_STARTED;
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
                equalizerInputStream = new EqualizerInputStream(workBufStreamWrapper,
                        decodedFormat.getSampleRate(), decodedFormat.getChannels(),
                        decodedFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED),
                        decodedFormat.getSampleSizeInBits(), decodedFormat.isBigEndian(), Equalizer.BANDS);
                Loggers.playerLogger.info("line opened and started");
                Loggers.playerLogger.info("channels count = {}", decodedFormat.getChannels());
                Loggers.playerLogger.info("sample rate = {}", decodedFormat.getSampleRate());
                Loggers.playerLogger.info("sample size = {}", decodedFormat.getSampleSizeInBits());
                byteArray.clear();
                startBuffering();
                currentPosition = 0;
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

    private void startBuffering() {
        Utils.runThread(new Runnable() {
            @Override
            public void run() {
                isBuffering = true;
                byte[] buffer = new byte[1 << 13];
                int readBytes;
                try {
                    Loggers.playerLogger.debug("started buffering");
                    while ((readBytes = currentStream.read(buffer, 0, buffer.length)) != -1) {
                        byteArray.addBytes(buffer, readBytes);
                    }
                    Loggers.playerLogger.debug("stream buffered");
                } catch (IOException e) {
                    Loggers.playerLogger.error("cannot buffer stream", e);
                    throw new RuntimeException(e);
                } finally {
                    isBuffering = false;
                }
            }
        });
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
        return convertFramesToMicroseconds(getFramePosition());
    }

    public void addEventListener(EventListener listener) {
        listeners.add(listener);
    }

    public void removeEventListener(EventListener listener) {
        listeners.remove(listener);
    }

    public boolean play() {
        playerLock.lock();
        if (playerLine == null) {
            Loggers.playerLogger.error("play on non-initialized player");
            return false;
        }
        playerStatus = PLAYING;
        playerLock.unlock();
        notify(PlayerEvent.PLAYING);
        return playerStatus == PLAYING;
    }

    private long convertFramesToMicroseconds(long frames) {
        return frames * 1000 / (long) playerLine.getFormat().getFrameRate();
    }

    private int convertMicrosecondsToFrames(long m) {
        return (int) Math.ceil(m * playerLine.getFormat().getFrameRate() / 1000);
    }

    public void setMicrosecondsPosition(long position) {
        if (playerLine == null) {
            return;
        }
        int frame = convertMicrosecondsToFrames(position);
        setFramePosition(frame);
    }

    public void setFramePosition(int frame) {
        playerLock.lock();
        currentPosition = frame * playerLine.getFormat().getFrameSize();
        playerLock.unlock();
    }

    public int getFramePosition() {
        return currentPosition / playerLine.getFormat().getFrameSize();
    }

    public boolean pause() {
        playerLock.lock();
        if (playerLine == null) {
            Loggers.playerLogger.error("play on non-initialized player");
            return false;
        }
        playerStatus = PAUSED;
        playerLock.unlock();
        return playerStatus == PAUSED;
    }

    public void stop() {
        playerLock.lock();
        currentPosition = 0;
        playerStatus = FINISHED;
        playerLock.unlock();
        notify(PlayerEvent.STOPPED);
    }

    public void close() {
        playerLock.lock();
        playerStatus = FULL_STOP;
        if (playerLine != null) {
            playerLine.stop();
            playerLine.close();
        }
        playerLock.unlock();
    }

    private void playerInternal() {
        byte[] data = new byte[bufferSize];
        int readBytes = 0, writtenBytes = 0;
        boolean pauseNotified = false;
        while (playerStatus != FULL_STOP) {
            if (playerStatus == PLAYING) {
                pauseNotified = false;
                // need for bytes available to read at currentPosition
                while ((readBytes = byteArray.read(workBuf, currentPosition)) == 0 && isBuffering) {
                    Thread.yield();
                }
                equalizerInputStream.getControls().setPreampValue(0, Equalizer.getPreampValue());
                equalizerInputStream.getControls().setPreampValue(1, Equalizer.getPreampValue());
                for (int i = 0; i < Equalizer.BANDS; i++) {
                    equalizerInputStream.getControls().setBandValue(i, 0, Equalizer.getBandValue(i));
                    equalizerInputStream.getControls().setBandValue(i, 1, Equalizer.getBandValue(i));
                }
                workBufStreamWrapper.reset();
                if (Equalizer.isEnabled()) {
                    try {
                        equalizerInputStream.read(data);
                    } catch (IOException e) {
                        Loggers.playerLogger.error("error", e);
                    }
                } else {
                    System.arraycopy(workBuf, 0, data, 0, readBytes);
                }
                Thread.yield();
                currentPosition = currentPosition + readBytes;
                if (readBytes == 0 && !isBuffering) {
                    playerLine.drain();
                    playerLine.stop();
                    playerLine.close();
                    notify(PlayerEvent.FINISHED);
                } else if (readBytes > 0) {
                    writtenBytes = playerLine.write(data, 0, readBytes);
                    notify(PlayerEvent.FRAME_PLAYED);
                }
                Thread.yield();
            } else {
                if (playerStatus == PAUSED && !pauseNotified) {
                    notify(PlayerEvent.PAUSED);
                    pauseNotified = true;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    public int getState() {
        return playerStatus;
    }
}
