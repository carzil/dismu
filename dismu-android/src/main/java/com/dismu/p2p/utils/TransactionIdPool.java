package com.dismu.p2p.utils;

public class TransactionIdPool {
    private static int currentId = 0;
    public synchronized static int getNext() {
        return currentId++;
    }
}
