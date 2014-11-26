package com.dismu.music.core.queue;

import com.dismu.music.core.Track;

public class TrackQueueEntry {
    private Track item;
    private TrackQueueEntry next = null;
    private TrackQueueEntry prev = null;

    public TrackQueueEntry() {

    }

    public TrackQueueEntry(Track item) {
        this.item = item;
    }

    public Track getItem() {
        return item;
    }

    public void setItem(Track item) {
        this.item = item;
    }

    public TrackQueueEntry getNext() {
        return next;
    }

    public void setNext(TrackQueueEntry next) {
        this.next = next;
    }

    public TrackQueueEntry getPrev() {
        return prev;
    }

    public void setPrev(TrackQueueEntry prev) {
        this.prev = prev;
    }
}