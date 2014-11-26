package com.dismu.music.core.queue;

import com.dismu.music.core.Track;

import java.util.Iterator;

class QueueIterator implements Iterator<TrackQueueEntry> {
    private TrackQueueEntry current = null;

    public QueueIterator(TrackQueueEntry first) {
        current = first;
    }

    @Override
    public boolean hasNext() {
        return current != null;
    }

    @Override
    public TrackQueueEntry next() {
        TrackQueueEntry entry = current;
        current = current.getNext();
        return entry;
    }

    @Override
    public void remove() {

    }
}

public class TrackQueue implements Iterable<TrackQueueEntry> {
    TrackQueueEntry first = null;
    TrackQueueEntry last = null;

    public TrackQueue() {
    }

    public void pushBack(Track item) {
        if (last == null) {
            last = new TrackQueueEntry(item);
            first = last;
        } else {
            TrackQueueEntry newEntry = new TrackQueueEntry(item);
            last.setNext(newEntry);
            last = newEntry;
        }
    }

    public void insertAfter(TrackQueueEntry prev, Track item) {
        TrackQueueEntry newEntry = new TrackQueueEntry(item);
        TrackQueueEntry next = prev.getNext();
        prev.setNext(newEntry);
        newEntry.setNext(next);
        newEntry.setPrev(prev);
        next.setPrev(newEntry);
    }


    @Override
    public Iterator<TrackQueueEntry> iterator() {
        return new QueueIterator(first);
    }
}
