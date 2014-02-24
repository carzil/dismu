package com.dismu.music.player;

import com.dismu.logging.Loggers;

import java.io.File;

public class Player {
    TrackStorage trackStorage;

    public static void main(String[] args) {
        Player player = new Player();
        player.loop();
    }

    public Player() {
        this.trackStorage = new PCTrackStorage();
        Track track = this.trackStorage.saveTrack(new File("rise_against-prayer_of_the_refugee.mp3"));
        Loggers.playerLogger.info("got track, id={}", track.getID());
    }

    public void loop() {
//        while (true) {
//
//        }
    }
}
