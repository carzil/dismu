package com.dismu.p2p.packets;

import com.dismu.p2p.utils.Loggers;

import java.util.HashMap;

public class PacketManager {
    private static HashMap<Integer, Class> packets = new HashMap<Integer, Class>();

    public PacketManager() {
        packets.put(PacketType.PT_REQUEST_SEEDS, RequestSeedsPacket.class);
        packets.put(PacketType.PT_REQUEST_SEEDS_RESPONSE, RequestSeedsResponsePacket.class);
        packets.put(PacketType.PT_EXIT, ExitPacket.class);
    }

    public Class getPacket(int type) {
        Loggers.serverLogger.info("getPacket({})", type);
        return packets.get(type);
    }
}
