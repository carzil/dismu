package com.dismu.p2p.packets;

import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.PacketManager;
import com.dismu.p2p.utilities.Logging;

import java.io.*;

public class RequestSeedsPacket extends Packet {
    public int groupId;

    public RequestSeedsPacket() {
        this.type = PacketType.PT_REQUEST_SEEDS;
    }

    public void parse() throws IOException {
        ByteArrayInputStream byteArrayInputStream;
        byteArrayInputStream = new ByteArrayInputStream(this.data);

        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        groupId = dataInputStream.readInt();
    }

    public void serialize() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

        dataOutputStream.writeInt(groupId);
        dataOutputStream.flush();
        outputStream.flush();
        this.data = outputStream.toByteArray();
    }
}