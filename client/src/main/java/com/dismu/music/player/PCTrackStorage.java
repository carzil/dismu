package com.dismu.music.player;

import java.io.File;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import com.dismu.utils.Utils;
import com.dismu.logging.Loggers;

public class PCTrackStorage implements TrackStorage {
    private HashMap<Track, File> tracks = new HashMap<Track, File>();
    private HashSet<Long> trackHashes = new HashSet<Long>();
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
            File trackFile = new File(getTrackFolder(), trackFileName);
            try {
                this.trackHashes.add(Utils.getAdler32FileHash(trackFile));
            } catch (IOException e) {
                Loggers.playerLogger.error("hash computing failed", e);
                continue;
            }
            Loggers.playerLogger.info("read track from index, id={}, filename='{}'", track.getID(), trackFileName);
            this.tracks.put(track, trackFile);
        }
    }

    private synchronized void saveIndex() throws IOException {
        DataOutputStream index = new DataOutputStream(new FileOutputStream(trackIndex));
        index.writeInt(this.tracks.size());
        for (Map.Entry<Track, File> entry : tracks.entrySet()) {
            Track track = entry.getKey();
            String trackName = entry.getValue().getName();
            track.writeToStream(index);
            index.writeUTF(trackName);
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

    public synchronized Track saveTrack(File trackFile) {
        Loggers.playerLogger.info("got track '{}' for saving", trackFile.getAbsolutePath());
        try {
            Track track = Track.fromMp3File(trackFile);
            long fileHash = Utils.getAdler32FileHash(trackFile);
            Loggers.playerLogger.debug("track hash = {}", fileHash);
            if (trackHashes.contains(fileHash)) {
                Loggers.playerLogger.info("track already registered in index");
                return null;
            } else {
                maxTrackID++;
                track.setID(maxTrackID);
                File finalTrackFile = new File(getTrackFolder(), track.getPrettifiedFileName());
                Loggers.playerLogger.info("final track name = '{}'", finalTrackFile.getAbsolutePath());
                tracks.put(track, finalTrackFile);
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
        tracks.remove(track);
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
