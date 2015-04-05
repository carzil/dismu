package com.dismu.music.storages;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.dismu.music.core.Track;
import com.dismu.music.events.TrackStorageEvent;
import com.dismu.utils.Utils;
import com.dismu.logging.Loggers;
import com.dismu.utils.events.Event;
import com.dismu.utils.events.EventListener;

public class TrackStorage {
    private HashMap<Track, File> tracks = new HashMap<>();
    private HashMap<Integer, Track> trackHashes = new HashMap<>();
    private HashMap<Long, Track> trackFileHashes = new HashMap<>();
    private HashMap<Track, Long> trackFileHashesInv = new HashMap<>();
    private File trackIndex;
    private final Lock storageLock = new ReentrantLock();
    private ArrayList<EventListener> listeners = new ArrayList<>();
    private static volatile TrackStorage instance;
    private long checkHash;
    private ExecutorService pool = Executors.newSingleThreadExecutor();

    private void notify(final Event event) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                for (EventListener listener : listeners) {
                    listener.dispatchEvent(event);
                }
            }
        };
        pool.submit(task);
    }

    private File getTrackFolder() {
        File trackFolder = new File(Utils.getAppFolderPath(), "tracks");
        if (!trackFolder.exists()) {
            trackFolder.mkdirs();
        }
        return trackFolder;
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
        Loggers.playerLogger.debug("track index checkHash={}", checkHash);
        storageLock.lock();
        try {
            clear();
            for (int i = 0; i < tracksCount; i++) {
                Track track = Track.readFromStream(stream);
                String trackPath = stream.readUTF();
                long fileHash = stream.readLong();
                File trackFile = new File(trackPath);
                if (trackFile.exists()) {
                    Loggers.playerLogger.debug("file '{}' exists", trackPath);
                    tracks.put(track, trackFile);
                    trackHashes.put(track.hashCode(), track);
                    trackFileHashes.put(fileHash, track);
                    trackFileHashesInv.put(track, fileHash);
                } else {
                    Loggers.playerLogger.warn("file '{}' doesn't exists", trackPath);
                }
                Loggers.playerLogger.debug("read track from index, hash={}, path='{}'", fileHash, trackPath);
            }
        } finally {
            storageLock.unlock();
        }
        Loggers.playerLogger.info("successfully read track index");
    }

    public void writeToStream(DataOutputStream stream) throws IOException {
        storageLock.lock();
        try {
            stream.writeInt(tracks.size());
            stream.writeLong(computeCheckHash());
            for (Map.Entry<Track, File> entry : tracks.entrySet()) {
                Track track = entry.getKey();
                String trackPath = entry.getValue().getAbsolutePath();
                track.writeToStream(stream);
                stream.writeUTF(trackPath);
                stream.writeLong(trackFileHashesInv.get(track));
                Loggers.playerLogger.info("track {} registered in index (filesystem path '{}')", track, trackPath);
            }
        } finally {
            storageLock.unlock();
        }
        Loggers.playerLogger.info("index successfully saved");
    }

    private long computeCheckHash() {
        storageLock.lock();
        try {
            long hash = 0;
            for (Map.Entry<Track, File> entry : tracks.entrySet()) {
                hash ^= entry.getKey().hashCode() ^ entry.getValue().getName().hashCode();
            }
            return hash;
        } finally {
            storageLock.unlock();
        }
    }

    public void parseIndex() throws IOException {
        if (!trackIndex.exists()) {
            Loggers.playerLogger.info("index doesn't exists");
            trackIndex.createNewFile();
            getTrackFolder().mkdir();
            new DataOutputStream(new FileOutputStream(trackIndex)).writeInt(0);
        }
        Loggers.playerLogger.info("index exists");
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Utils.readStreamToBytes(new BufferedInputStream(new FileInputStream(trackIndex))));
            readFromStream(new DataInputStream(byteArrayInputStream));
        } catch (EOFException | UTFDataFormatException e) {
            Loggers.playerLogger.error("corrupted track index, need re-indexing", e);
        }
    }

    public void saveIndex() throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(trackIndex)));
        writeToStream(dataOutputStream);
        dataOutputStream.flush();
        dataOutputStream.close();
    }

    public boolean isCorrupted() {
        long hash = computeCheckHash();
        if (hash != checkHash) {
            Loggers.playerLogger.info("checkHash index mismatch: {} != {}", hash, checkHash);
        }
        return hash != checkHash;
    }

    public void clear() {
        storageLock.lock();
        try {
            trackHashes.clear();
            tracks.clear();
            trackFileHashes.clear();
            trackFileHashesInv.clear();
        } finally {
            storageLock.unlock();
        }
    }

    /**
     * Get all tracks registered in track index.
     * @return array of tracks registered in index
     */
    public Track[] getTracks() {
        return tracks.keySet().toArray(new Track[0]);
    }

    public boolean isFileInStorage(File sourceFile) throws IOException {
        Loggers.playerLogger.debug("got file to check, path='{}'", sourceFile.getAbsolutePath());
        long fileHash = Utils.getFileHash64(sourceFile);
        Loggers.playerLogger.debug("file hash = {}", fileHash);
        return isFileInStorage(fileHash);
    }

    public boolean isFileInStorage(long hash) {
        Loggers.playerLogger.debug("got hash to check, hash={}", hash);
        return trackFileHashes.containsKey(hash);
    }

    /**
     * Adds track to track index and copies track file to local storage.
     * @param trackFile track file to add
     * @param commit is true, saves index after adding track
     * @return track added to storage
     */
    public Track saveTrack(File trackFile, boolean commit) {
        Loggers.playerLogger.info("got track '{}' for saving", trackFile.getAbsolutePath());
        try {
            // TODO: we need to check for hash collisions
            long fileHash = Utils.getFileHash64(trackFile);
            if (isFileInStorage(fileHash)) {
                Loggers.playerLogger.info("track already registered in index");
                return trackFileHashes.get(fileHash);
            } else {
                Track track = Track.fromFile(trackFile);
                if (track != null) {
                    Loggers.playerLogger.info("processing track {}", track);
                    Loggers.playerLogger.info("track path '{}'", trackFile.getAbsolutePath());
                    storageLock.lock();
                    try {
                        tracks.put(track, trackFile);
                        trackHashes.put(track.hashCode(), track);
                        trackFileHashes.put(fileHash, track);
                        trackFileHashesInv.put(track, fileHash);
                    } finally {
                        storageLock.unlock();
                    }

                    if (commit) {
                        saveIndex();
                    }

                    Loggers.playerLogger.info("track registered in index");
                    notify(new TrackStorageEvent(TrackStorageEvent.TRACK_ADDED, track));
                    return track;
                } else {
                    Loggers.playerLogger.debug("cannot read track from file '{}'", trackFile.getAbsolutePath());
                    return null;
                }
            }
        } catch (IOException e) {
            Loggers.playerLogger.error("cannot save track file", e);
            return null;
        }
    }

    public void commit() throws IOException {
        saveIndex();
    }

    public Track saveTrack(File file) {
        return saveTrack(file, true);
    }

    public void removeTrack(Track track) {
        if (!tracks.containsKey(track)) {
            Loggers.playerLogger.info("track already deleted");
            return;
        }
        storageLock.lock();
        try {
            tracks.remove(track);
            trackHashes.remove(track.hashCode());
            long hash = trackFileHashesInv.get(track);
            trackFileHashes.remove(hash);
            trackFileHashesInv.remove(track);
        } finally {
            storageLock.unlock();
        }
        notify(new TrackStorageEvent(TrackStorageEvent.TRACK_REMOVED, track));
        Loggers.playerLogger.info("removed track {}", track);
    }

    public File getTrackFile(Track track) {
        return tracks.get(track);
    }

    public Track getTrackByFileHash(long hash) {
        return trackFileHashes.get(hash);
    }

    public Track getTrackByHash(int hash) {
        return trackHashes.get(hash);
    }

    public void close() {
        try {
            saveIndex();
        } catch (IOException e) {
            Loggers.playerLogger.error("cannot save index", e);
        } finally {
            pool.shutdown();
        }
    }

    public void addEventListener(EventListener listener) {
        listeners.add(listener);
    }

    public void removeEventListener(EventListener listener) {
        listeners.remove(listener);
    }

    public int size() {
        return tracks.size();
    }
}