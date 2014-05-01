package com.dismu.music.storages.events;

import com.dismu.music.player.Track;
import com.dismu.utils.events.Event;

public class TrackStorageEvent implements Event {
    public static final int TRACK_ADDED = 0;
    public static final int TRACK_REMOVED = 1;

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
