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
    private HashMap<Track, File> tracks = new HashMap<Track, File>();
    private File trackIndex;
    private int maxTrackID = -1;

    private File getTrackFolder() {
        return new File(Utils.getAppFolderPath(), "tracks");
    }

    private synchronized void parseIndex() throws IOException {
        if (!this.trackIndex.exists()) {
            Loggers.playerLogger.info("index doesn't exists");
            this.trackIndex.createNewFile();
            this.getTrackFolder().mkdir();
            new DataOutputStream(new FileOutputStream(this.trackIndex)).writeInt(0);
        }
        DataInputStream index = new DataInputStream(new FileInputStream(this.trackIndex));
        Loggers.playerLogger.info("index exists");
        int tracksCount = index.readInt();
        Loggers.playerLogger.info("index tracksCount = {}", tracksCount);
        for (int i = 0; i < tracksCount; i++) {
            Track track = Track.readFromStream(index);
            this.maxTrackID = Math.max(this.maxTrackID, track.getID());
            String trackFileName = index.readUTF();
            File trackFile = new File(this.getTrackFolder(), trackFileName);
            Loggers.playerLogger.info("read track from index, id={}, filename='{}'", track.getID(), trackFileName);
            this.tracks.put(track, trackFile);
        }
    }

    private synchronized void saveIndex() throws IOException {
        DataOutputStream index = new DataOutputStream(new FileOutputStream(this.trackIndex));
        index.writeInt(this.tracks.size());
        for (Map.Entry<Track, File> entry : this.tracks.entrySet()) {
            Track track = entry.getKey();
            String trackName = entry.getValue().getName();
            track.writeToStream(index);
            index.writeUTF(trackName);
            Loggers.playerLogger.debug("track id={}, name='{}' registered in index", track.getID(), trackName);
        }
        index.flush();
        index.close();
        Loggers.playerLogger.info("index successfully saved");
    }

    public PCTrackStorage() {
        try {
            this.trackIndex = new File(Utils.getAppFolderPath(), "tracks.index");
            this.parseIndex();
        } catch (IOException e) {
            Loggers.clientLogger.error("impossible error", e);
        }

    }

    public synchronized Track[] getTracks() {
        return this.tracks.keySet().toArray(new Track[0]);
    }

    public synchronized Track saveTrack(File trackFile) {
        File copiedTrackFile = new File(this.getTrackFolder(), trackFile.getName());
        try {
            Utils.copyFile(trackFile, copiedTrackFile);
            this.maxTrackID++;
            Track track = new Track(this.maxTrackID);
            this.tracks.put(track, copiedTrackFile);
            this.saveIndex();
            return track;
        } catch (IOException e) {
            Loggers.playerLogger.error("cannot save track file", e);
        }
        return null;
    }

    public synchronized File getTrackFile(Track track) {
        return this.tracks.get(track);
    }

    public void close() {
        try {
            this.saveIndex();
        } catch (IOException e) {
            Loggers.playerLogger.error("cannot save index", e);
        }
    }
}
