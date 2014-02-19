package com.dismu.p2p.packets;

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
