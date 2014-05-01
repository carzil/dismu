package com.dismu.p2p.packets.node_control;

import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.PacketType;

import java.io.IOException;

public class ExitPacket extends Packet {
    public ExitPacket() {
        this.type = PacketType.PT_EXIT;
    }

    @Override
    public void parse() throws IOException {

    }

    @Override
    public void serialize() throws IOException {

    }
}
