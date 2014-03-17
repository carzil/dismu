package com.dismu.music.player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;
import javazoom.jl.decoder.JavaLayerException;

public class PCPlayerBackend implements PlayerBackend {
    private TrackStorage trackStorage;
    private Track currentTrack;
    private File currentTrackFile;
    private FileInputStream currentFileInputStream;
    private PausablePlayer player;

    public PCPlayerBackend(TrackStorage trackStorage) {
        this.trackStorage = trackStorage;
    }

    public boolean stop() {
        this.player.stop();
        return true;
    }

    /**
     * starting playing track, last indicated by setTrack(); async method
     */
    public boolean play() {
        try {
            this.player.play();
        } catch (JavaLayerException e) {
            Loggers.playerLogger.error("exception occurred while playing track id={}", currentTrack.getID(), e);
            return false;
        }
        return true;
    }

    public boolean pause() {
        return this.player.pause();
    }

    public boolean seek(double seconds) {
        this.player.seek(seconds);
        return true;
    }

    public void close() {
        this.stop();
    }

    public void setTrack(Track track) throws TrackNotFoundException {
        this.currentTrack = track;
        this.currentTrackFile = this.trackStorage.getTrackFile(track);
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