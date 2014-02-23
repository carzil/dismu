package com.dismu.p2p.packets.transaction;

import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.PacketType;

import java.io.*;

public class EndTransactionPacket extends Packet {
    public int transactionId = -1;

    public EndTransactionPacket() {
        this.type = PacketType.PT_END_TRANSACTION;
    }

    @Override
    public void parse() throws IOException {
        ByteArrayInputStream bis;
        bis = new ByteArrayInputStream(this.data);

        DataInputStream dis = new DataInputStream(bis);
        this.transactionId = dis.readInt();
    }

    @Override
    public void serialize() throws IOException {
        ByteArrayOutputStream bos;
        bos = new ByteArrayOutputStream();

        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeInt(this.transactionId);

        dos.flush();
        bos.flush();
        this.data = bos.toByteArray();
    }
}
