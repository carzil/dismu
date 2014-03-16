package com.dismu.music.player;

import java.io.File;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import com.dismu.utils.Utils;
import com.dismu.logging.Loggers;

public class PCTrackStorage implements TrackStorage {
    private HashMap<Track, File> tracks = new HashMap<>();
    private HashMap<Long, Track> trackHashes = new HashMap<>();
    private File trackIndex;
    private int maxTrackID = -1;

    private File getTrackFolder() {
        return new File(Utils.getAppFolderPath(), "tracks");
    }

    private synchronized void parseIndex() throws IOException {
        if (!trackIndex.exists()) {
            Loggers.playerLogger.info("index doesn't exists");
            trackIndex.createNewFile();
            getTrackFolder().mkdir();
            new DataOutputStream(new FileOutputStream(trackIndex)).writeInt(0);
        }
        DataInputStream index = new DataInputStream(new FileInputStream(trackIndex));
        Loggers.playerLogger.info("index exists");
        int tracksCount = index.readInt();
        Loggers.playerLogger.info("index tracksCount = {}", tracksCount);
        for (int i = 0; i < tracksCount; i++) {
            Track track = Track.readFromStream(index);
            maxTrackID = Math.max(maxTrackID, track.getID());
            String trackFileName = index.readUTF();
            long fileHash = index.readLong();
            File trackFile = new File(getTrackFolder(), trackFileName);
            trackHashes.put(fileHash, track);
            Loggers.playerLogger.info("read track from index, id={}, hash={}, filename='{}'", track.getID(), fileHash, trackFileName);
            tracks.put(track, trackFile);
        }
        Loggers.playerLogger.info("successfully read track index");
    }

    private synchronized void saveIndex() throws IOException {
        DataOutputStream index = new DataOutputStream(new FileOutputStream(trackIndex));
        index.writeInt(tracks.size());
        for (Map.Entry<Track, File> entry : tracks.entrySet()) {
            Track track = entry.getKey();
            String trackName = entry.getValue().getName();
            track.writeToStream(index);
            index.writeUTF(trackName);
            index.writeLong(Utils.getAdler32FileHash(entry.getValue()));
            Loggers.playerLogger.info("track id={}, name='{}' registered in index", track.getID(), trackName);
        }
        index.flush();
        index.close();
        Loggers.playerLogger.info("index successfully saved");
    }

    public PCTrackStorage() {
        try {
            trackIndex = new File(Utils.getAppFolderPath(), "tracks.index");
            parseIndex();
        } catch (IOException e) {
            Loggers.clientLogger.error("exception occurred while parsing track index", e);
        }

    }

    public synchronized Track[] getTracks() {
        return tracks.keySet().toArray(new Track[0]);
    }

    /**
     * Adds track to track index and copies file to local storage
     * @param trackFile track file to add
     * @return Track object added to storage
     */
    public synchronized Track saveTrack(File trackFile) {
        Loggers.playerLogger.info("got track '{}' for saving", trackFile.getAbsolutePath());
        try {
            Track track = Track.fromMp3File(trackFile);
            long fileHash = Utils.getAdler32FileHash(trackFile);
            Loggers.playerLogger.debug("track hash = {}", fileHash);
            if (trackHashes.containsKey(fileHash)) {
                Loggers.playerLogger.info("track already registered in index");
                return trackHashes.get(fileHash);
            } else {
                maxTrackID++;
                track.setID(maxTrackID);
                File finalTrackFile = new File(getTrackFolder(), track.getPrettifiedFileName());
                Loggers.playerLogger.info("final track name = '{}'", finalTrackFile.getAbsolutePath());
                tracks.put(track, finalTrackFile);
                trackHashes.put(fileHash, track);
                Utils.copyFile(trackFile, finalTrackFile);
                Loggers.playerLogger.info("track registered in index");
                saveIndex();
                return track;
            }
        } catch (IOException e) {
            Loggers.playerLogger.error("cannot save track file", e);
        }
        return null;
    }

    public synchronized void removeTrack(Track track) {
        try {
            File trackFile = tracks.get(track);
            long hash = Utils.getAdler32FileHash(trackFile);
            trackHashes.remove(hash);
            tracks.remove(track);
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
}
