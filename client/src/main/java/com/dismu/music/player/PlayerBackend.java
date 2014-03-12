package com.dismu.music.player;

import com.dismu.exceptions.TrackNotFoundException;

public interface PlayerBackend {
    public void setTrack(Track track) throws TrackNotFoundException;
    public boolean play();
    public boolean stop();
    public boolean pause();
    public boolean isPlaying();
    public void close();
}
