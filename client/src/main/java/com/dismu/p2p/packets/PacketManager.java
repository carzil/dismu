package com.dismu.p2p.packets;

import java.util.HashMap;
import com.dismu.p2p.packets.PacketType;
import com.dismu.p2p.utilities.Logging;

public class PacketManager {
    private static HashMap<Integer, Class> packets = new HashMap<Integer, Class>();

    public PacketManager() {
        packets.put(PacketType.PT_REQUEST_SEEDS, RequestSeedsPacket.class);
        packets.put(PacketType.PT_REQUEST_SEEDS_RESPONSE, RequestSeedsResponsePacket.class);
    }

    public Class getPacket(int type) {
        Logging.serverLogger.info("getPacket({})", type);
        return packets.get(type);
    }
}
