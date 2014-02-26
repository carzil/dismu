package com.dismu.music.player;

import com.dismu.logging.Loggers;

import java.io.File;

public class Player {
    TrackStorage trackStorage;

    public static void main(String[] args) {
        Player player = new Player();
        player.run();
    }

    public Player() {
        this.trackStorage = new PCTrackStorage();
        this.trackStorage.saveTrack(new File("rise_against-prayer_of_the_refugee.mp3"));
        for (Track track : this.trackStorage.getTracks()) {
            Loggers.playerLogger.info("got track from storage, id={}", track.getID());
        }
    }

    public void run() {}
}
