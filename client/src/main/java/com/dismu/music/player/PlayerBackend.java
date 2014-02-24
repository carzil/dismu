package com.dismu.music.player;

import com.dismu.music.player.Track;

public abstract class PlayerBackend {
    public abstract boolean setTrack(Track track);
    public abstract boolean play();
    public abstract boolean stop();
}
