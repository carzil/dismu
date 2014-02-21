package com.dismu.p2p.packets;

import java.io.*;

public class StartTransactionPacket extends Packet {
    public String filename = "";

    public StartTransactionPacket() {
        this.type = PacketType.PT_START_TRANSACTION;
    }

    @Override
    public void parse() throws IOException {
        ByteArrayInputStream bis;
        bis = new ByteArrayInputStream(this.data);

        DataInputStream dis = new DataInputStream(bis);
        {
            int len = dis.readInt();
            byte[] fn = new byte[len];
            dis.read(fn);
            this.filename = new String(fn);
        }
    }

    @Override
    public void serialize() throws IOException {
        ByteArrayOutputStream bos;
        bos = new ByteArrayOutputStream();

        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeInt(this.filename.getBytes().length);
        dos.write(this.filename.getBytes());

        dos.flush();
        bos.flush();
        this.data = bos.toByteArray();
    }
}
