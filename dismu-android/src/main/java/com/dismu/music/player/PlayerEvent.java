package com.dismu.music.player;

import com.dismu.utils.events.Event;

public class PlayerEvent implements Event {
    public static final int PLAYING = 0;
    public static final int STOPPED = 1;
    public static final int PAUSED = 2;

    private int eventType;

    public PlayerEvent(int eventType) {
        this.eventType = eventType;
    }

    public int getType() {
        return eventType;
    }
}
