package com.dismu.music.player;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import com.dismu.utils.Utils;
import com.dismu.logging.Loggers;

public class PCTrackStorage extends TrackStorage {
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
            int trackID = index.readInt();
            this.maxTrackID = Math.max(this.maxTrackID, trackID);
            String trackFileName = index.readUTF();
            Track track = new Track(trackID);
            File trackFile = new File(this.getTrackFolder(), trackFileName);
            if (trackFile.exists()) {
                // Should we keep tracks which are in index but isn't in track folder?
                Loggers.playerLogger.info("read track from index, id={}, filename='{}'", trackID, trackFileName);
                this.tracks.put(track, trackFile);
            }
        }
    }

    private synchronized void saveIndex() throws IOException {
        DataOutputStream index = new DataOutputStream(new FileOutputStream(this.trackIndex));
        index.writeInt(this.tracks.size());
        for (Map.Entry<Track, File> entry : this.tracks.entrySet()) {
            Track track = entry.getKey();
            index.writeInt(track.getID());
            index.writeUTF(entry.getValue().getName());
            Loggers.playerLogger.debug("track id={}, name='{}' registered in index", track.getID(), entry.getValue().getName());
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
}
