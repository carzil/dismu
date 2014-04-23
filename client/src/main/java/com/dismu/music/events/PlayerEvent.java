package com.dismu.music.events;

import com.dismu.utils.events.Event;

public class PlayerEvent implements Event {
    public static final int PLAYING = 0;
    public static final int STOPPED = 1;
    public static final int PAUSED = 2;
    public static final int FINISHED = 3;
    public static final int FRAME_PLAYED = 4;

    private int eventType;

    public PlayerEvent(int eventType) {
        this.eventType = eventType;
    }

    public int getType() {
        return eventType;
    }
}
