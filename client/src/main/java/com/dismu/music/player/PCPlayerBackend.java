package com.dismu.music.player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.Player;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;

class PausablePlayer {

    public final static int NOT_STARTED = 0;
    public final static int PLAYING = 1;
    public final static int PAUSED = 2;
    public final static int FINISHED = 3;

    private final Player player;
    private final Object playerLock = new Object();
    private int playerStatus = NOT_STARTED;

    public PausablePlayer(final InputStream inputStream) throws JavaLayerException {
        this.player = new Player(inputStream);
    }

    public PausablePlayer(final InputStream inputStream, final AudioDevice audioDevice) throws JavaLayerException {
        this.player = new Player(inputStream, audioDevice);
    }

    public void play() throws JavaLayerException {
        synchronized (playerLock) {
            switch (playerStatus) {
                case NOT_STARTED:
                    final Thread t = new Thread() {
                        public void run() {
                            playInternal();
                        }
                    };
//                    t.setDaemon(true);
//                    t.setPriority(Thread.MAX_PRIORITY);
                    playerStatus = PLAYING;
                    t.start();
                    break;
                case PAUSED:
                    resume();
                    break;
                default:
                    break;
            }
        }
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
                if (!player.play(1)) {
                    break;
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

public class PCPlayerBackend extends PlayerBackend {
    private TrackStorage trackStorage;
    private Track currentTrack;
    private File currentTrackFile;
    private FileInputStream currentFileInputStream;
    private PausablePlayer player;
    private int state = PlayerBackend.NOT_PLAYING;
    private int pausedOnFrame = 0;
    private PlaybackListener playbackListener = new PlaybackListener() {
        @Override
        public void playbackFinished(PlaybackEvent event) {
            pausedOnFrame = event.getFrame();
            Loggers.playerLogger.debug("paused at frame {}", pausedOnFrame);
            state = PlayerBackend.PAUSED;
        }

        public void playbackStarted(PlaybackEvent event) {
            state = PlayerBackend.PLAYING;
        }
    };

    public PCPlayerBackend(TrackStorage trackStorage) {
        this.trackStorage = trackStorage;
    }

    public boolean stop() {
        this.player.stop();
        return true;
    }

    public boolean play() {
        try {
            this.player.play();
        } catch (JavaLayerException e) {
            Loggers.playerLogger.error("exception occurred, while playing", e);
        }
        return true;
    }

    public boolean pause() {
        this.player.pause();
        return true;
    }

    public void setTrack(Track track) throws TrackNotFoundException {
        this.currentTrack = track;
        this.currentTrackFile = this.trackStorage.getTrackFile(track);
        this.pausedOnFrame = 0;
        try {
            this.currentFileInputStream = new FileInputStream(this.currentTrackFile);
            this.player = new PausablePlayer(this.currentFileInputStream);
        } catch (FileNotFoundException e) {
            throw new TrackNotFoundException();
        } catch (JavaLayerException e) {
            // TODO: throw exception here
        }
    }

    public boolean isPlaying() {
        return this.player.getState() == PausablePlayer.PLAYING;
    }

}
