package com.dismu.p2p.packets;

import com.dismu.logging.Loggers;
import com.dismu.p2p.packets.node_control.ExitPacket;
import com.dismu.p2p.packets.node_control.RequestSeedsPacket;
import com.dismu.p2p.packets.node_control.RequestSeedsResponsePacket;
import com.dismu.p2p.packets.transaction.*;

import java.util.HashMap;

public class PacketManager {
    private static HashMap<Integer, Class> packets = new HashMap<>();

    public PacketManager() {
        packets.put(PacketType.PT_REQUEST_SEEDS, RequestSeedsPacket.class);
        packets.put(PacketType.PT_REQUEST_SEEDS_RESPONSE, RequestSeedsResponsePacket.class);
        packets.put(PacketType.PT_EXIT, ExitPacket.class);

        packets.put(PacketType.PT_START_TRANSACTION, StartTransactionPacket.class);
        packets.put(PacketType.PT_ACCEPT_TRANSACTION, AcceptTransactionPacket.class);
        packets.put(PacketType.PT_REQUEST_CHUNK, RequestChunkPacket.class);
        packets.put(PacketType.PT_RESPONSE_CHUNK, ResponseChunkPacket.class);
        packets.put(PacketType.PT_END_TRANSACTION, EndTransactionPacket.class);

        packets.put(PacketType.PT_NEW_TRACK_AVAILABLE, NewTrackAvailablePacket.class);
    }

    public Class getPacket(int type) {
        Loggers.packetLogger.debug("getPacket({})", type);
        return packets.get(type);
    }
}
