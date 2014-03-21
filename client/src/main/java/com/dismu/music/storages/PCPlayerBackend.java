package com.dismu.music.storages;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import com.dismu.music.player.PausablePlayer;
import com.dismu.music.player.Track;
import com.dismu.utils.events.EventListener;
import javazoom.jl.decoder.JavaLayerException;

public class PCPlayerBackend implements PlayerBackend {
    private TrackStorage trackStorage;
    private Track currentTrack;
    private File currentTrackFile;
    private FileInputStream currentFileInputStream;
    private PausablePlayer player = new PausablePlayer();

    public PCPlayerBackend(TrackStorage trackStorage) {
        this.trackStorage = trackStorage;
    }

    public boolean stop() {
        player.stop();
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
        try {
            player.play();
        } catch (JavaLayerException e) {
            Loggers.playerLogger.error("exception occurred while playing track id={}", currentTrack.getID(), e);
            return false;
        }
        return true;
    }

    public boolean pause() {
        return player.pause();
    }

    public boolean seek(double seconds) {
        player.seek(seconds);
        return true;
    }

    public void close() {
        player.close();
    }

    public void setTrack(Track track) throws TrackNotFoundException {
        currentTrack = track;
        currentTrackFile = trackStorage.getTrackFile(track);
        try {
            currentFileInputStream = new FileInputStream(currentTrackFile);
            player.setInputStream(currentFileInputStream);
        } catch (FileNotFoundException e) {
            throw new TrackNotFoundException();
        } catch (JavaLayerException e) {
            // TODO: throw exception here
        }
    }

    public synchronized boolean isPlaying() {
        return player.getState() == PausablePlayer.PLAYING;
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }
}