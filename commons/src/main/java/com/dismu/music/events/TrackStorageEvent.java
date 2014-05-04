package com.dismu.music.events;

import com.dismu.music.player.Track;
import com.dismu.utils.events.Event;

public class TrackStorageEvent implements Event {
    public static final int TRACK_ADDED = 0;
    public static final int TRACK_REMOVED = 1;
    public static final int REINDEX_STARTED = 2;
    public static final int REINDEX_FINISHED = 3;

    private Track track;
    private int id;

    public TrackStorageEvent(int id) {
        this.id = id;
    }

    public TrackStorageEvent(int id, Track track) {
        this.id = id;
        this.track = track;
    }

    public int getType() {
        return id;
    }

    public Track getTrack() {
        return track;
    }
}
