package com.dismu.music.player;


import java.io.File;

public class Track {
    private int trackID = -1;

    public Track() {}

    public Track(int trackID) {
        this.trackID = trackID;
    }

    public int getID() {
        return this.trackID;
    }
}
