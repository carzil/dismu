package com.dismu.music.player;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

import com.dismu.logging.Loggers;
import com.dismu.music.player.Playlist;
import com.dismu.utils.Utils;

public class PCPlaylistStorage implements PlaylistStorage {
    private ArrayList<Playlist> playlists;

    public PCPlaylistStorage() {
        this.playlists = new ArrayList<Playlist>();
        this.load();
    }

    public synchronized void addPlaylist(Playlist playlist) {
        this.playlists.add(playlist);
        this.save();
    }

    public synchronized void removePlaylist(Playlist playlist) {
        this.playlists.remove(playlist);
        this.save();
    }

    public Playlist[] getAllPlaylist() {
        return this.playlists.toArray(new Playlist[0]);
    }

    public void close() {
        this.save();
    }

    private File getIndexFile() {
        return new File(Utils.getAppFolderPath(), "playlists.index");
    }

    private synchronized void save() {
        Loggers.playerLogger.debug("saving playlist index");
        File indexFile = getIndexFile();
        DataOutputStream index = null;
        if (!indexFile.exists()) {
            Loggers.playerLogger.debug("playlist index doesn't exists");
            try {
                indexFile.createNewFile();
                Loggers.playerLogger.debug("successfully created new playlist index file");
            } catch (IOException e) {
                Loggers.playerLogger.error("cannot create playlist index file", e);
                return;
            }
        }
        try {
            index = new DataOutputStream(new FileOutputStream(indexFile));
        } catch (FileNotFoundException e) {
            Loggers.playerLogger.error("playlist index doesn't exists", e);
            return;
        }
        try {
            index.writeInt(this.playlists.size());
            for (Playlist playlist : this.playlists) {
                playlist.writeToStream(index);
                Loggers.playerLogger.debug("saved playlist, name='{}'", playlist.getName());
            }
        } catch (IOException e) {
            Loggers.playerLogger.error("cannot write to playlist index file", e);
        }
        Loggers.playerLogger.debug("index successfully saved");
    }

    private synchronized void load() {
        File indexFile = getIndexFile();
        DataInputStream index = null;
        try {
            index = new DataInputStream(new FileInputStream(indexFile));
        } catch (FileNotFoundException e) {
            Loggers.playerLogger.info("playlist index doesn't exists");
            return;
        }
        try {
            int playlistCount = index.readInt();
            Loggers.playerLogger.debug("playlistCount = {}", playlistCount);
            for (int i = 0; i < playlistCount; i++) {
                Playlist playlist = Playlist.readFromStream(index);
                this.playlists.add(playlist);
                Loggers.playerLogger.debug("read playlist '{}'", playlist.getName());
            }
        } catch (IOException e) {
            Loggers.playerLogger.error("cannot read from playlist index", e);
            return;
        }
        Loggers.playerLogger.info("playlist index read successfully");
    }
}
