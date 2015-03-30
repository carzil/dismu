package com.dismu.utils;

import com.dismu.logging.Loggers;

import java.util.Arrays;

public class DynamicByteArray {
    private final static double GROW_FACTOR = 1.5;
    private final static int INITIAL_SIZE = 1 << 16;
    private int currentPosition = 0;
    private byte[] byteBuffer;

    public DynamicByteArray() {
        byteBuffer = new byte[INITIAL_SIZE];
    }

    public void addBytes(byte[] bytes, int length) {
        if (currentPosition + bytes.length >= byteBuffer.length) {
            resize();
        }
        System.arraycopy(bytes, 0, byteBuffer, currentPosition, length);
        currentPosition += length;
    }

    private void resize() {
        int newSize = (int) (GROW_FACTOR * byteBuffer.length);
        byteBuffer = Arrays.copyOf(byteBuffer, newSize);
    }

    public byte[] getBuffer() {
        return byteBuffer;
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
        byteBuffer = new byte[INITIAL_SIZE];
        currentPosition = 0;
    }

}
