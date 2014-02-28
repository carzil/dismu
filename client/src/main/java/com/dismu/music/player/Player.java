package com.dismu.music.player;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.logging.Loggers;

import java.io.File;

public class Player {
    TrackStorage trackStorage;
    PlayerBackend playerBackend;

    public static void main(String[] args) {
        Player player = new Player();
        player.run();
    }

    public Player() {
        this.trackStorage = new PCTrackStorage();
        this.playerBackend = new PCPlayerBackend(this.trackStorage);
    }

    public void run() {
//        this.trackStorage.saveTrack(new File("rise_against-prayer_of_the_refugee.mp3"));
        for (Track track : this.trackStorage.getTracks()) {
            try {
                this.playerBackend.setTrack(track);
            } catch (TrackNotFoundException e) {
                Loggers.playerLogger.info("track not found, id={}", track.getID(), e);
            }
            this.playerBackend.play();
        }
    }
}
