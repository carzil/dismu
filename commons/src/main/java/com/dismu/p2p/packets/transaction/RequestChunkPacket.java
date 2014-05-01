package com.dismu.p2p.packets.transaction;

import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.PacketType;

import java.io.*;

public class RequestChunkPacket extends Packet {
    public int transactionId = -1;
    public int offset = -1, count = -1;

    public RequestChunkPacket() {
        this.type = PacketType.PT_REQUEST_CHUNK;
    }

    @Override
    public void parse() throws IOException {
        ByteArrayInputStream bis;
        bis = new ByteArrayInputStream(this.data);

        DataInputStream dis = new DataInputStream(bis);
        this.transactionId = dis.readInt();
        this.offset = dis.readInt();
        this.count = dis.readInt();
    }

    @Override
    public void serialize() throws IOException {
        ByteArrayOutputStream bos;
        bos = new ByteArrayOutputStream();

        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeInt(this.transactionId);
        dos.writeInt(this.offset);
        dos.writeInt(this.count);

        dos.flush();
        bos.flush();
        this.data = bos.toByteArray();
    }
}
