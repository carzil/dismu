package com.dismu.p2p.packets;

import java.io.*;

public class AcceptTransactionPacket extends Packet {
    public int transactionId = -1;
    public int fileSize = -1;
    public long fileHash = -1;
    public String error = "";

    public AcceptTransactionPacket() {
        this.type = PacketType.PT_ACCEPT_TRANSACTION;
    }

    @Override
    public void parse() throws IOException {
        ByteArrayInputStream bis;
        bis = new ByteArrayInputStream(this.data);

        DataInputStream dis = new DataInputStream(bis);
        this.transactionId = dis.readInt();
        this.fileSize = dis.readInt();

        this.fileHash = dis.readLong();
        int len = dis.readInt();
        byte[] e = new byte[len];
        dis.read(e);
        this.error = new String(e);
    }

    @Override
    public void serialize() throws IOException {
        ByteArrayOutputStream bos;
        bos = new ByteArrayOutputStream();

        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeInt(this.transactionId);
        dos.writeInt(this.fileSize);

        dos.writeLong(this.fileHash);

        dos.writeInt(this.error.getBytes().length);
        dos.write(this.error.getBytes());

        dos.flush();
        bos.flush();
        this.data = bos.toByteArray();
    }
}
