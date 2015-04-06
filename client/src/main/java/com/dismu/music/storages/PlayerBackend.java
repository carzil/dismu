package com.dismu.music.storages;

import java.io.*;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.player.PausablePlayer;
import com.dismu.music.core.Track;
import com.dismu.utils.events.EventListener;

public class PlayerBackend {
    private final TrackStorage storage;
    private Track currentTrack;
    private File currentTrackFile;
    private InputStream currentInputStream;
    private PausablePlayer player = new PausablePlayer();
    private static volatile PlayerBackend instance;

    public PlayerBackend(TrackStorage storage) {
        this.storage = storage;
    }

    public long getPosition() {
        return player.getPosition();
    }

    public boolean stop() {
        player.stop();
        currentTrack = null;
        return true;
    }

    public void addEventListener(EventListener listener) {
        player.addEventListener(listener);
    }

    public void removeEventListener(EventListener listener) {
        player.removeEventListener(listener);
    }

    /**
     * starting playing track, last indicated by setTrack(); async method
     */
    public boolean play() {
        player.play();
        return true;
    }

    public boolean pause() {
        return player.pause();
    }

    public boolean setMicrosecondsPosition(long mSeconds) {
        player.setMicrosecondsPosition(mSeconds);
        return true;
    }

    public boolean setFramePosition(int frame) {
        player.setFramePosition(frame);
        return true;
    }

    public void close() {
        player.close();
    }

    public void setTrack(Track track) throws TrackNotFoundException {
        currentTrack = track;
        currentTrackFile = storage.getTrackFile(track);
        Loggers.playerLogger.info("TrackStorage yielded file '{}'", currentTrackFile);
        try {
            currentInputStream = new BufferedInputStream(new FileInputStream(currentTrackFile));
            player.loadInputStream(currentInputStream);
        } catch (FileNotFoundException e) {
            throw new TrackNotFoundException();
        } catch (Exception e) {
            // TODO: throw exception here
        }
    }

    public boolean isPlaying() {
        return player.getState() == PausablePlayer.PLAYING;
    }

    public boolean isPaused() {
        return player.getState() == PausablePlayer.PAUSED;
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }

    public TrackStorage getStorage() {
        return storage;
    }
}