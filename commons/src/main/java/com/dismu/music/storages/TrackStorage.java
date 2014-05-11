package com.dismu.music.storages;

import com.dismu.logging.Loggers;
import com.dismu.music.player.Track;
import com.dismu.music.events.TrackStorageEvent;
import com.dismu.utils.Utils;
import com.dismu.utils.events.Event;
import com.dismu.utils.events.EventListener;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TrackStorage {
    private HashMap<Track, File> tracks = new HashMap<>();
    private HashMap<Integer, Track> trackHashes = new HashMap<>();
    private HashMap<Long, Track> trackFileHashes = new HashMap<>();
    private HashMap<Track, Long> trackFileHashesInv = new HashMap<>();
    private File trackIndex;
    private int maxTrackID = -1;
    private final Object storageLock = new Object();
    private ArrayList<EventListener> listeners = new ArrayList<>();
    private static volatile TrackStorage instance;

    private void notify(Event event) {
        for (EventListener listener : listeners) {
            listener.dispatchEvent(event);
        }
    }

    private File getTrackFolder() {
        return new File(Utils.getAppFolderPath(), "tracks");
    }

    private TrackStorage() {
        try {
            trackIndex = new File(Utils.getAppFolderPath(), "tracks.index");
            parseIndex();
        } catch (IOException e) {
            Loggers.clientLogger.error("exception occurred while parsing track index", e);
        }
    }

    public static TrackStorage getInstance() {
        TrackStorage localInstance = instance;
        if (localInstance == null) {
            synchronized (TrackStorage.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new TrackStorage();
                }
            }
        }
        return localInstance;
    }

    public void readFromStream(DataInputStream stream) throws IOException {
        int tracksCount = stream.readInt();
        Loggers.playerLogger.info("index tracksCount = {}", tracksCount);
        synchronized (storageLock) {
            trackFileHashes.clear();
            trackFileHashesInv.clear();
            tracks.clear();
        }
        for (int i = 0; i < tracksCount; i++) {
            Track track = Track.readFromStream(stream);
            maxTrackID = Math.max(maxTrackID, track.getID());
            String trackFileName = stream.readUTF();
            long fileHash = stream.readLong();
            File trackFile = new File(getTrackFolder(), trackFileName);
            synchronized (storageLock) {
                trackHashes.put(track.hashCode(), track);
                trackFileHashes.put(fileHash, track);
                trackFileHashesInv.put(track, fileHash);
                tracks.put(track, trackFile);
            }
            Loggers.playerLogger.info("read track from index, id={}, hash={}, filename='{}'", track.getID(), fileHash, trackFileName);
        }
        Loggers.playerLogger.info("successfully read track index");
    }

    public void writeToStream(DataOutputStream stream) throws IOException {
        synchronized (stream) {
            stream.writeInt(tracks.size());
            for (Map.Entry<Track, File> entry : tracks.entrySet()) {
                Track track = entry.getKey();
                String trackName = entry.getValue().getName();
                track.writeToStream(stream);
                stream.writeUTF(trackName);
                stream.writeLong(trackFileHashesInv.get(track));
                Loggers.playerLogger.info("track id={}, name='{}' registered in index", track.getID(), trackName);
            }
            Loggers.playerLogger.info("index successfully saved");
        }
    }

    public synchronized void parseIndex() throws IOException {
        if (!trackIndex.exists()) {
            Loggers.playerLogger.info("index doesn't exists");
            trackIndex.createNewFile();
            getTrackFolder().mkdir();
            new DataOutputStream(new FileOutputStream(trackIndex)).writeInt(0);
        }
        Loggers.playerLogger.info("index exists");
        readFromStream(new DataInputStream(new FileInputStream(trackIndex)));
    }

    public synchronized void saveIndex() throws IOException {
        writeToStream(new DataOutputStream(new FileOutputStream(trackIndex)));
    }

    /**
     * Get all tracks registered in track index.
     * @return array of tracks registered in index
     */
    public synchronized Track[] getTracks() {
        return tracks.keySet().toArray(new Track[0]);
    }

    public synchronized boolean isInStorage(File sourceFile) throws IOException {
        Loggers.playerLogger.debug("got file to check, path='{}'", sourceFile.getAbsolutePath());
        long fileHash = Utils.getAdler32FileHash(sourceFile);
        Loggers.playerLogger.debug("file hash = {}", fileHash);
        return trackFileHashes.containsKey(fileHash);
    }

    /**
     * Adds track to track index and copies track file to local storage.
     * @param trackFile track file to add
     * @return track added to storage
     */
    public synchronized Track saveTrack(File trackFile) {
        Loggers.playerLogger.info("got track '{}' for saving", trackFile.getAbsolutePath());
        try {
            long fileHash = Utils.getAdler32FileHash(trackFile);
            if (isInStorage(trackFile)) {
                Loggers.playerLogger.info("track already registered in index");
                return trackFileHashes.get(fileHash);
            } else {
                Track track = Track.fromFile(trackFile);
                maxTrackID++;
                track.setID(maxTrackID);
                File finalTrackFile = new File(getTrackFolder(), track.getPrettifiedFileName());
                Loggers.playerLogger.info("final track name = '{}'", finalTrackFile.getAbsolutePath());
                tracks.put(track, finalTrackFile);
                trackHashes.put(track.hashCode(), track);
                trackFileHashes.put(fileHash, track);
                trackFileHashesInv.put(track, fileHash);
                Utils.copyFile(trackFile, finalTrackFile);
                Loggers.playerLogger.info("track registered in index");
                saveIndex();
                notify(new TrackStorageEvent(TrackStorageEvent.TRACK_ADDED, track));
                return track;
            }
        } catch (IOException e) {
            Loggers.playerLogger.error("cannot save track file", e);
            return null;
        }
    }

    public Track saveTrack(byte[] bytes) {
        try {
            File tmpFile = File.createTempFile("tmp", ".mp3"); // hash here, mb?
            new FileOutputStream(tmpFile).write(bytes);
            return saveTrack(tmpFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Track();
    }

    public synchronized void removeTrack(Track track) {
        try {
            File trackFile = tracks.get(track);
            long hash = Utils.getAdler32FileHash(trackFile);
            trackHashes.remove(track.hashCode());
            trackFileHashes.remove(hash);
            trackFileHashesInv.remove(track);
            tracks.remove(track);
            notify(new TrackStorageEvent(TrackStorageEvent.TRACK_REMOVED, track));
            Loggers.playerLogger.info("removed track id={}, hash={}, filename='{}'", track.getID(), hash, trackFile.getName());
        } catch (IOException e) {
            Loggers.playerLogger.error("exception occurred while removing track", e);
        }
    }

    public synchronized File getTrackFile(Track track) {
        return tracks.get(track);
    }

    public synchronized Track getTrackByHash(int hashCode) {
        return trackHashes.get(hashCode);
    }

    public void close() {
        try {
            saveIndex();
        } catch (IOException e) {
            Loggers.playerLogger.error("cannot save index", e);
        }
    }

    public void addEventListener(EventListener listener) {
        listeners.add(listener);
    }

    public void removeEventListener(EventListener listener) {
        listeners.remove(listener);
    }
}
