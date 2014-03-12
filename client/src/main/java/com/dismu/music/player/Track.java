package com.dismu.music.player;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Track {
    private int trackID = -1;

    public Track() {}

    public Track(int trackID) {
        this.trackID = trackID;
    }

    public int getID() {
        return this.trackID;
    }

    public void writeToStream(DataOutputStream stream) throws IOException {
        stream.writeInt(this.trackID);
    }

    public static Track readFromStream(DataInputStream stream) throws IOException {
        return new Track(stream.readInt());
    }
}
