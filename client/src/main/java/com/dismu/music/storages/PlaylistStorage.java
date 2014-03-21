package com.dismu.music.storages;

import com.dismu.music.player.Playlist;

public interface PlaylistStorage {
    public void addPlaylist(Playlist playlist);
    public void removePlaylist(Playlist playlist);
    public Playlist[] getAllPlaylist();
    public void close();
}
