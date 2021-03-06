package com.dismu.music.player;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.dismu.exceptions.EmptyPlaylistException;
import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.music.Track;

public class Playlist {
    private int id;
    private ArrayList<Track> tracks;
    private int currentTrackIndex = 0;
    private String name = "Untitled";
    private volatile boolean isRemoved = false;

    public Playlist() {
        tracks = new ArrayList<>();
    }

    private synchronized void fixCurrentTrackIndex() {
        if (currentTrackIndex >= tracks.size()) {
            currentTrackIndex %= tracks.size();
        } else if (currentTrackIndex < 0) {
            currentTrackIndex = tracks.size() - 1;
        }
    }

    private void checkIsEmpty() throws EmptyPlaylistException {
        if (isEmpty()) {
            throw new EmptyPlaylistException();
        }
    }

    public int hashCode() {
        return getId();
    }

    public boolean isEmpty() {
        return tracks.size() == 0;
    }

    public boolean isEnded() {
        return tracks.size() <= currentTrackIndex;
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
        tracks.add(track);
    }

    public Track getCurrentTrack() throws EmptyPlaylistException {
        checkIsEmpty();
        return tracks.get(currentTrackIndex);
    }

    public void next() throws EmptyPlaylistException {
        checkIsEmpty();
        currentTrackIndex++;
        fixCurrentTrackIndex();
    }

    public void prev() throws EmptyPlaylistException {
        checkIsEmpty();
        currentTrackIndex--;
        fixCurrentTrackIndex();
    }

    public void reset() throws EmptyPlaylistException {
        checkIsEmpty();
        currentTrackIndex = 0;
    }

    public void setCurrentTrack(Track track) throws TrackNotFoundException {
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).equals(track)) {
                currentTrackIndex = i;
                return;
            }
        }
        throw new TrackNotFoundException();
    }

    public void setCurrentTrack(int index) throws TrackNotFoundException {
        if (index >= tracks.size()) {
            throw new TrackNotFoundException();
        } else {
            currentTrackIndex = index;
        }
    }

    public void writeToStream(DataOutputStream stream) throws IOException {
        stream.writeUTF(getName());
        stream.writeInt(getId());
        stream.writeInt(tracks.size());
        stream.writeInt(currentTrackIndex);
        for (Track track : tracks) {
            track.writeToStream(stream);
        }
    }

    public static Playlist readFromStream(DataInputStream stream) throws IOException {
        Playlist playlist = new Playlist();
        playlist.setName(stream.readUTF());
        playlist.setId(stream.readInt());
        int trackCount = stream.readInt();
        playlist.currentTrackIndex = stream.readInt();
        for (int i = 0; i < trackCount; i++) {
            Track track = Track.readFromStream(stream);
            playlist.addTrack(track);
        }
        return playlist;
    }

    public boolean isRemoved() {
        return isRemoved;
    }

    public void setRemoved(boolean isRemoved) {
        this.isRemoved = isRemoved;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String toString() {
        return name;
    }
}
