package com.dismu.music.player;

import java.io.File;
import java.util.LinkedList;
import com.dismu.music.player.Track;

public abstract class TrackStorage {
    public abstract Track[] getTracks();
    public abstract Track saveTrack(File trackFile);
    public abstract File getTrackFile(Track track);
}
