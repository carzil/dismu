package com.dismu.music.player;

import java.io.*;
import java.net.MalformedURLException;
import javax.media.Player;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.CannotRealizeException;
import javax.media.Controller;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;

public class PCPlayerBackend implements PlayerBackend {
    private TrackStorage trackStorage;
    private Track currentTrack;
    private File currentTrackFile;
    private Player player;

    public PCPlayerBackend(TrackStorage trackStorage) {
        this.trackStorage = trackStorage;
    }

    public boolean stop() {
        this.player.stop();
        return true;
    }

    public boolean play() {
        this.player.start();
        return true;
    }

    public boolean pause() {
        return this.stop();
    }

    public void close() {
        this.stop();
    }

    public void setTrack(Track track) throws TrackNotFoundException {
        this.currentTrack = track;
        this.currentTrackFile = this.trackStorage.getTrackFile(track);
        try {
            this.player = Manager.createRealizedPlayer(this.currentTrackFile.toURI().toURL());
        } catch (IOException e) {
            throw new TrackNotFoundException();
        } catch (NoPlayerException e) {
            throw new TrackNotFoundException();
        } catch (CannotRealizeException e) {
            throw new TrackNotFoundException();
        }
    }

    public boolean isPlaying() {
        return this.player.getState() == Controller.Started;
    }

}
