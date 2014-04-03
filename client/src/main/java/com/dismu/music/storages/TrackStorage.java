package com.dismu.music.storages;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.dismu.music.player.Track;
import com.dismu.music.storages.events.TrackStorageEvent;
import com.dismu.utils.Utils;
import com.dismu.logging.Loggers;
import com.dismu.utils.events.Event;
import com.dismu.utils.events.EventListener;

public class TrackStorage {
    private HashMap<Track, File> tracks = new HashMap<>();
    private HashMap<Long, Track> trackHashes = new HashMap<>();
    private File trackIndex;
    private int maxTrackID = -1;
    private final Object storageLock = new Object();
    private ArrayList<EventListener> listeners = new ArrayList<>();
    private static volatile TrackStorage instance;
    private long checkHash;

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
        checkHash = stream.readLong();
        Loggers.playerLogger.info("index tracksCount={}", tracksCount);
        Loggers.playerLogger.info("track index checkHash={}", checkHash);
        synchronized (storageLock) {
            trackHashes.clear();
            tracks.clear();
        }
        for (int i = 0; i < tracksCount; i++) {
            Track track = Track.readFromStream(stream);
            maxTrackID = Math.max(maxTrackID, track.getID());
            String trackFileName = stream.readUTF();
            long fileHash = stream.readLong();
            File trackFile = new File(getTrackFolder(), trackFileName);
            synchronized (storageLock) {
                trackHashes.put(fileHash, track);
                tracks.put(track, trackFile);
            }
            Loggers.playerLogger.info("read track from index, id={}, hash={}, filename='{}'", track.getID(), fileHash, trackFileName);
        }
        Loggers.playerLogger.info("successfully read track index");
    }

    public void writeToStream(DataOutputStream stream) throws IOException {
        // TODO: synchronize stream
        synchronized (stream) {
            stream.writeInt(tracks.size());
            stream.writeLong(computeCheckHash());
            for (Map.Entry<Track, File> entry : tracks.entrySet()) {
                Track track = entry.getKey();
                String trackName = entry.getValue().getName();
                track.writeToStream(stream);
                stream.writeUTF(trackName);
                stream.writeLong(Utils.getAdler32FileHash(entry.getValue()));
                Loggers.playerLogger.info("track id={}, name='{}' registered in index", track.getID(), trackName);
            }
            Loggers.playerLogger.info("index successfully saved");
        }
    }

    private long computeCheckHash() {
        long hash = 0;
        for (Map.Entry<Track, File> entry : tracks.entrySet()) {
            hash ^= entry.getKey().hashCode() ^ entry.getValue().getName().hashCode();
        }
        return hash;
    }

    public synchronized void parseIndex() throws IOException {
        if (!trackIndex.exists()) {
            Loggers.playerLogger.info("index doesn't exists");
            trackIndex.createNewFile();
            getTrackFolder().mkdir();
            new DataOutputStream(new FileOutputStream(trackIndex)).writeInt(0);
        }
        Loggers.playerLogger.info("index exists");
        try {
            readFromStream(new DataInputStream(new FileInputStream(trackIndex)));
        } catch (EOFException | UTFDataFormatException e) {
            Loggers.playerLogger.info("corrupted track index, re-indexing");
            reindex();
        }
        if (isCorrupted()) {
            Loggers.playerLogger.info("got corrupted track index, re-indexing");
            reindex();
        }
    }

    public synchronized void saveIndex() throws IOException {
        writeToStream(new DataOutputStream(new FileOutputStream(trackIndex)));
    }

    public synchronized boolean isCorrupted() {
        long hash = computeCheckHash();
        if (hash != checkHash) {
            Loggers.playerLogger.info("checkHash index mismatch: {} != {}", hash, checkHash);
        }
        return hash != checkHash;
    }

    public synchronized void clear() {
        trackHashes.clear();
        tracks.clear();
        maxTrackID = -1;
    }

    public synchronized void reindex() {
        clear();
        for (File file : getTrackFolder().listFiles()) {
            saveTrack(file, false);
        }
        try {
            saveIndex();
        } catch (IOException e) {
            Loggers.playerLogger.error("error while reindexing", e);
        }
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
        return trackHashes.containsKey(fileHash);
    }

    /**
     * Adds track to track index and copies track file to local storage.
     * @param trackFile track file to add
     * @param commit is true, saves index after adding track
     * @return track added to storage
     */
    public synchronized Track saveTrack(File trackFile, boolean commit) {
        Loggers.playerLogger.info("got track '{}' for saving", trackFile.getAbsolutePath());
        try {
            long fileHash = Utils.getAdler32FileHash(trackFile);
            if (isInStorage(trackFile)) {
                Loggers.playerLogger.info("track already registered in index");
                return trackHashes.get(fileHash);
            } else {
                Track track = Track.fromMp3File(trackFile);
                maxTrackID++;
                track.setID(maxTrackID);
                File finalTrackFile = new File(getTrackFolder(), track.getPrettifiedFileName());
                Loggers.playerLogger.info("final track name = '{}'", finalTrackFile.getAbsolutePath());
                tracks.put(track, finalTrackFile);
                trackHashes.put(fileHash, track);
                if (!trackFile.getAbsolutePath().equals(finalTrackFile.getAbsolutePath())) {
                    Utils.copyFile(trackFile, finalTrackFile);
                }
                Loggers.playerLogger.info("track registered in index");
                if (commit) {
                    saveIndex();
                }
                notify(new TrackStorageEvent(TrackStorageEvent.TRACK_ADDED, track));
                return track;
            }
        } catch (IOException e) {
            Loggers.playerLogger.error("cannot save track file", e);
            return null;
        }
    }

    public Track saveTrack(File file) {
        return saveTrack(file, true);
    }

    public Track saveTrack(byte[] bytes) {
        try {
            File tmpFile = File.createTempFile("tmp", ".mp3"); // hash here, mb?
            new FileOutputStream(tmpFile).write(bytes);
            return saveTrack(tmpFile);
        } catch (IOException e) {

        }
        return new Track();
    }

    public synchronized void removeTrack(Track track) {
        try {
            File trackFile = tracks.get(track);
            long hash = Utils.getAdler32FileHash(trackFile);
            trackHashes.remove(hash);
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
