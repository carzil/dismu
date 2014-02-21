package com.dismu.p2p.packets;

public class PacketType {
    static final int PT_REQUEST_SEEDS = 0;
    static final int PT_REQUEST_SEEDS_RESPONSE = 1;
    static final int PT_EXIT = 2;
    static final int PT_START_TRANSACTION = 3;
    static final int PT_ACCEPT_TRANSACTION = 4;
    static final int PT_REQUEST_CHUNK = 5;
    static final int PT_RESPONSE_CHUNK = 6;
    static final int PT_END_TRANSACTION = 7;
}
