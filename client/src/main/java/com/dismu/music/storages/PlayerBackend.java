package com.dismu.music.storages;

import com.dismu.exceptions.TrackNotFoundException;
import com.dismu.music.player.Track;
import com.dismu.utils.events.EventListener;

public interface PlayerBackend {
    public void setTrack(Track track) throws TrackNotFoundException;
    public boolean play();
    public boolean stop();
    public boolean pause();
    public boolean isPlaying();
    public boolean seek(double seconds);
    public Track getCurrentTrack();
    public void addEventListener(EventListener listener);
    public void removeEventListener(EventListener listener);
    public void close();
}
