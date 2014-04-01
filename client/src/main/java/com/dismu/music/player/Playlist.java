package com.dismu.music.player;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.dismu.exceptions.EmptyPlaylistException;

public class Playlist {
    private ArrayList<Track> tracks;
    private int currentTrackIndex = 0;
    private boolean isCycled = false;
    private String name = "Untitled";

    public Playlist() {
        this.tracks = new ArrayList<Track>();
    }

    private void fixCurrentTrackIndex() {
        if (this.currentTrackIndex >= this.tracks.size()) {
            this.currentTrackIndex %= this.tracks.size();
        }
    }

    private void checkIsEmpty() throws EmptyPlaylistException {
        if (this.isEmpty()) {
            throw new EmptyPlaylistException();
        }
    }

    public boolean isCycled() {
        return this.isCycled;
    }

    public boolean isEmpty() {
        return this.tracks.size() == 0;
    }

    public boolean isEnded() {
        return tracks.size() <= currentTrackIndex;
    }

    public void setCycled(boolean isCycled) {
        this.isCycled = isCycled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Track> getTracks() {
        return tracks;
    }

    public int getTrackCount() {
        return tracks.size();
    }

    public void addTrack(Track track) {
        this.tracks.add(track);
    }

    public Track getCurrentTrack() throws EmptyPlaylistException {
        this.checkIsEmpty();
        return this.tracks.get(this.currentTrackIndex);
    }

    public void next() throws EmptyPlaylistException {
        this.checkIsEmpty();
        this.currentTrackIndex++;
        this.fixCurrentTrackIndex();
    }

    public void prev() throws EmptyPlaylistException {
        this.checkIsEmpty();
        this.currentTrackIndex--;
        this.fixCurrentTrackIndex();
    }

    public void reset() throws EmptyPlaylistException {
        this.checkIsEmpty();
        currentTrackIndex = 0;
    }

    public void writeToStream(DataOutputStream stream) throws IOException {
        stream.writeUTF(this.getName());
        stream.writeBoolean(this.isCycled());
        stream.writeInt(this.tracks.size());
        for (Iterator<Track> it = this.tracks.iterator(); it.hasNext();) {
            Track track = it.next();
            track.writeToStream(stream);
        }
    }

    public static Playlist readFromStream(DataInputStream stream) throws IOException {
        Playlist playlist = new Playlist();
        playlist.setName(stream.readUTF());
        playlist.setCycled(stream.readBoolean());
        int trackCount = stream.readInt();
        for (int i = 0; i < trackCount; i++) {
            Track track = Track.readFromStream(stream);
            playlist.addTrack(track);
        }
        return playlist;
    }
}
