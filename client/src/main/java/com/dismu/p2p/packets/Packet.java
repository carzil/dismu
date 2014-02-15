package com.dismu.p2p.packets;

import java.io.*;

public class Packet {
    public int type = -1;
    public byte[] data = null;

    public void read(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        this.type = dis.readInt();
        int size = dis.readInt();
        this.data = new byte[size];
        dis.read(this.data);
        this.parse();
    }

    public void write(OutputStream os) throws IOException {
        this.serialize();
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt(this.type);
        dos.writeInt(this.data.length);
        dos.write(this.data);

        dos.flush();
        os.flush();
    }

    public boolean isMine() {
        return false;
    }

    public void parse() {

    }

    public void serialize() {

    }
}
