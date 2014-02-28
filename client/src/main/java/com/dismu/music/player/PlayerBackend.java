package com.dismu.music.player;

import com.dismu.exceptions.TrackNotFoundException;

public abstract class PlayerBackend {
    final static int NOT_PLAYING = 0;
    final static int PAUSED = 1;
    final static int PLAYING = 2;

    public abstract void setTrack(Track track) throws TrackNotFoundException;
    public abstract boolean play();
    public abstract boolean stop();
    public abstract boolean pause();
    public abstract boolean isPlaying();
}
