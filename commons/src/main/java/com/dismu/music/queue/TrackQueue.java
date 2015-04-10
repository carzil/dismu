package com.dismu.music.queue;

import com.dismu.logging.Loggers;
import com.dismu.music.Track;

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
    private volatile TrackQueueEntry prevFirst = null;
    private volatile TrackQueueEntry first = null;
    private volatile TrackQueueEntry last = null;

    public TrackQueue() {
    }

    /**
     * Adds track to the end of queue.
     * @param item track to add
     */
    public void pushBack(Track item) {
        if (first == null) {
            Loggers.uiLogger.debug("pushBack first");
            last = new TrackQueueEntry(item);
            first = last;
            if (prevFirst != null) {
                first.setPrev(prevFirst);
                prevFirst.setNext(first);
            }
            prevFirst = null;
        } else {
            Loggers.uiLogger.debug("pushBack last");
            TrackQueueEntry newEntry = new TrackQueueEntry(item);
            last.setNext(newEntry);
            newEntry.setPrev(last);
            last = newEntry;
        }
    }

    /**
     * Inserts track after specified element.
     * @param prev entry to add after
     * @param item track to add
     */
    public void insertAfter(TrackQueueEntry prev, Track item) {
        TrackQueueEntry newEntry = new TrackQueueEntry(item);
        TrackQueueEntry next = prev.getNext();
        prev.setNext(newEntry);
        newEntry.setNext(next);
        newEntry.setPrev(prev);
        if (next != null) {
            next.setPrev(newEntry);
        }
    }

    /**
     * Returns {@link com.dismu.music.queue.TrackQueueEntry} at the top of queue
     * @return element at top of queue or null, if queue os empty
     */
    public TrackQueueEntry peek() {
        return first;
    }

    /**
     * Selects next of top as new top. If null is next, lasts current top.
     */
    public void popFirst() {
        if (first == null) {
            return;
        }
        TrackQueueEntry next = first.getNext();
        if (next == null) {
            prevFirst = first;
            last = null;
        }
        first = next;
    }

    public void restoreFirst() {
        if (first == null) {
            if (prevFirst == null) {
                return;
            } else {
                first = prevFirst;
                prevFirst = null;
                return;
            }
        }
        TrackQueueEntry prev = first.getPrev();
        if (prev == null) {
            return;
        }
        first = prev;
    }

    @Override
    public Iterator<TrackQueueEntry> iterator() {
        return new QueueIterator(first);
    }
}
