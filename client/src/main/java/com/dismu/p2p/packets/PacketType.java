package com.dismu.p2p.packets;

public class PacketType {
    public static final int PT_REQUEST_SEEDS = 0;
    public static final int PT_REQUEST_SEEDS_RESPONSE = 1;
    public static final int PT_EXIT = 2;
    public static final int PT_START_TRANSACTION = 3;
    public static final int PT_ACCEPT_TRANSACTION = 4;
    public static final int PT_REQUEST_CHUNK = 5;
    public static final int PT_RESPONSE_CHUNK = 6;
    public static final int PT_END_TRANSACTION = 7;
}
