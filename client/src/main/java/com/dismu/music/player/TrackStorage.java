package com.dismu.music.player;

import java.io.File;
import com.dismu.music.player.Track;

public interface TrackStorage {
    public Track[] getTracks();
    public Track saveTrack(File trackFile);
    public File getTrackFile(Track track);
    public void close();
}
