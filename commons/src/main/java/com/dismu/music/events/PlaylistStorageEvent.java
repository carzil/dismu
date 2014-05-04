package com.dismu.music.events;

import com.dismu.music.player.Playlist;
import com.dismu.utils.events.Event;

public class PlaylistStorageEvent implements Event {
    public static final int PLAYLIST_ADDED = 0;
    public static final int PLAYLIST_REMOVED = 1;

    private int id;
    private Playlist playlist;

    public PlaylistStorageEvent(int id) {
        this.id = id;
    }

    public PlaylistStorageEvent(int id, Playlist playlist) {
        this.playlist = playlist;
        this.id = id;
    }

    public int getType() {
        return id;
    }

    public Playlist getPlaylist() {
        return playlist;
    }
}
