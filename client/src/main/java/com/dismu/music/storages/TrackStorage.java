package com.dismu.music.storages;

import java.io.File;
import java.io.IOException;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.music.player.Track;

public interface TrackStorage {
    public Track[] getTracks();
    public Track saveTrack(File trackFile);
    public void removeTrack(Track track);
    public File getTrackFile(Track track);
    public void close();
}
