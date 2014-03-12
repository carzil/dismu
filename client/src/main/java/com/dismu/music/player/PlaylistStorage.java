package com.dismu.music.player;

public interface PlaylistStorage {
    public void addPlaylist(Playlist playlist);
    public void removePlaylist(Playlist playlist);
    public Playlist[] getAllPlaylist();
    public void close();
}
