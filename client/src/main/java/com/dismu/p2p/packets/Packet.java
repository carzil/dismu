package com.dismu.p2p.packets;

import com.dismu.p2p.utils.Loggers;

import java.io.*;

public abstract class Packet {
    public int type = -1;

    public byte[] data;

    public void read(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        int size = dataInputStream.readInt();
        Loggers.serverLogger.info("read new packet, size = {}, type = {}", size, this.type);
        this.data = new byte[size];
        dataInputStream.read(this.data);
        this.parse();
    }

    public void write(OutputStream outputStream) throws IOException {
        this.serialize();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeInt(this.type);
        dataOutputStream.writeInt(this.data.length);
        dataOutputStream.write(this.data);

        Loggers.serverLogger.info("wrote new packet, size = {}, type = {}", this.data.length, this.type);

        dataOutputStream.flush();
        outputStream.flush();
    }

    public boolean isMine(int type) {
        return this.type == type;
    };

    public abstract void parse() throws IOException;
    public abstract void serialize() throws IOException;
}
