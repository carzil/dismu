package com.dismu.music.queue;

import com.dismu.music.Track;

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