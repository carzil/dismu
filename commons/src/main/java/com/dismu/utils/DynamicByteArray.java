package com.dismu.utils;

import com.dismu.logging.Loggers;

import java.util.Arrays;

public class DynamicByteArray {
    private static final double GROWTH_FACTOR = 2.3;
    private int initialSize = 1 << 16;
    private int currentPosition = 0;
    private byte[] byteBuffer;

    public DynamicByteArray(int initialSize) {
        byteBuffer = new byte[initialSize];
        this.initialSize = initialSize;
    }

    public void addBytes(byte[] bytes, int length) {
        ensureCapacity(currentPosition + bytes.length);
        System.arraycopy(bytes, 0, byteBuffer, currentPosition, length);
        currentPosition += length;
    }

    private void ensureCapacity(int size) {
        if (size > byteBuffer.length) {
            resize();
        }
    }

    private void resize() {
        int newSize = (int) GROWTH_FACTOR * byteBuffer.length;
        byteBuffer = Arrays.copyOf(byteBuffer, newSize);
        Loggers.playerLogger.debug("resize called, new size={}", size());
        // This GC-call is needed because when we create new byte buffer and copy, we create a new
        // object in memory, but it's is very memory-expensive because this class is used to store media streams
        System.gc();
    }

    public int size() {
        return byteBuffer.length;
    }

    public int read(byte[] buffer, int position) {
        int readCnt = Math.min(buffer.length, currentPosition - position);
        if (readCnt > 0) {
            System.arraycopy(byteBuffer, position, buffer, 0, readCnt);
        }
        return Math.max(-1, readCnt);
    }

    public void clear() {
        Arrays.fill(byteBuffer, (byte) 0);
        currentPosition = 0;
    }

}
