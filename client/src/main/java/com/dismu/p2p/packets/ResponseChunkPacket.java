package com.dismu.p2p.packets;

import java.io.*;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

public class ResponseChunkPacket extends Packet {
    public int transactionId = -1;
    public int offset = -1, count = -1;
    public long chunkHash = -1;
    public byte[] chunk = new byte[0];

    public ResponseChunkPacket() {
        this.type = PacketType.PT_RESPONSE_CHUNK;
    }

    public void computeHash() {
        Checksum checksum = new Adler32();
        checksum.update(this.chunk, 0, this.chunk.length);
        this.chunkHash = checksum.getValue();
    }

    public boolean checkHash() {
        Checksum checksum = new Adler32();
        checksum.update(this.chunk, 0, this.chunk.length);

        long seen = checksum.getValue();
        long expected = this.chunkHash;

        return seen == expected;
    }

    @Override
    public void parse() throws IOException {
        ByteArrayInputStream bis;
        bis = new ByteArrayInputStream(this.data);

        DataInputStream dis = new DataInputStream(bis);
        this.transactionId = dis.readInt();
        this.offset = dis.readInt();
        this.count = dis.readInt();

        this.chunkHash = dis.readLong();

        {
            int len = dis.readInt();
            this.chunk = new byte[len];
            dis.read(this.chunk);
        }
    }

    @Override
    public void serialize() throws IOException {
        ByteArrayOutputStream bos;
        bos = new ByteArrayOutputStream();

        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeInt(this.transactionId);
        dos.writeInt(this.offset);
        dos.writeInt(this.count);

        dos.writeLong(this.chunkHash);

        dos.writeInt(this.chunk.length);
        dos.write(this.chunk);

        dos.flush();
        bos.flush();
        this.data = bos.toByteArray();
    }
}
