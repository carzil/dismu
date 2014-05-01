package com.dismu.music.storages;

import java.io.*;
import java.util.ArrayList;

import com.dismu.logging.Loggers;
import com.dismu.music.player.Playlist;
import com.dismu.music.storages.events.PlaylistStorageEvent;
import com.dismu.music.storages.events.TrackStorageEvent;
import com.dismu.utils.Utils;
import com.dismu.utils.events.Event;
import com.dismu.utils.events.EventListener;

public class PlaylistStorage {
    private ArrayList<Playlist> playlists;
    private ArrayList<EventListener> listeners = new ArrayList<>();;
    private static volatile PlaylistStorage instance;

    private PlaylistStorage() {
        playlists = new ArrayList<Playlist>();
        load();
    }

    public static PlaylistStorage getInstance() {
        PlaylistStorage localInstance = instance;
        if (localInstance == null) {
            synchronized (PlaylistStorage.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new PlaylistStorage();
                }
            }
        }
        return localInstance;
    }

    public synchronized boolean containsPlaylist(Playlist playlist) {
        for (Playlist p : playlists) {
            if (p.equals(playlist)) {
                return true;
            }
        }
        return false;
    }


    public synchronized void addPlaylist(Playlist playlist) {
        playlists.add(playlist);
        save();
        notify(new PlaylistStorageEvent(PlaylistStorageEvent.PLAYLIST_ADDED, playlist));
    }

    public synchronized void removePlaylist(Playlist playlist) {
        playlists.remove(playlist);
        save();
        notify(new PlaylistStorageEvent(PlaylistStorageEvent.PLAYLIST_REMOVED, playlist));
    }

    public Playlist[] getPlaylists() {
        return playlists.toArray(new Playlist[0]);
    }

    public void close() {
        this.save();
    }

    private File getIndexFile() {
        return new File(Utils.getAppFolderPath(), "playlists.index");
    }

    public synchronized void save() {
        Loggers.playerLogger.info("saving playlist index");
        File indexFile = getIndexFile();
        DataOutputStream index = null;
        if (!indexFile.exists()) {
            Loggers.playerLogger.info("playlist index doesn't exists");
            try {
                indexFile.createNewFile();
                Loggers.playerLogger.info("successfully created new playlist index file");
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
                Loggers.playerLogger.info("saved playlist, name='{}', track count={}", playlist.getName(), playlist.getTrackCount());
            }
        } catch (IOException e) {
            Loggers.playerLogger.error("cannot write to playlist index file", e);
        }
        Loggers.playerLogger.info("index successfully saved");
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
            Loggers.playerLogger.info("playlistCount = {}", playlistCount);
            for (int i = 0; i < playlistCount; i++) {
                Playlist playlist = Playlist.readFromStream(index);
                this.playlists.add(playlist);
                Loggers.playerLogger.info("read playlist, name='{}', track count={}", playlist.getName(), playlist.getTrackCount());
            }
        } catch (IOException e) {
            Loggers.playerLogger.error("cannot read from playlist index", e);
            return;
        }
        Loggers.playerLogger.info("playlist index read successfully");
    }

    public void addEventListener(EventListener listener) {
        listeners.add(listener);
    }

    public void removeEventListener(EventListener listener) {
        listeners.remove(listener);
    }

    private void notify(Event event) {
        for (EventListener listener : listeners) {
            listener.dispatchEvent(event);
        }
    }
}
