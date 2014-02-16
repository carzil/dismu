package com.dismu.p2p.packets;

import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.PacketType;

import java.io.*;
import java.net.InetAddress;

public class RequestSeedsResponsePacket extends Packet {
    public int type = PacketType.PT_REQUEST_SEEDS_RESPONSE;

    public InetAddress[] addresses;

    public RequestSeedsResponsePacket() {
        this.type = PacketType.PT_REQUEST_SEEDS_RESPONSE;
    }

    @Override
    public void parse() throws IOException {
        ByteArrayInputStream bis;
        bis = new ByteArrayInputStream(this.data);

        DataInputStream dis = new DataInputStream(bis);
        int len = dis.readInt();
        byte[] s = new byte[len];
        dis.read(s);
        String str = new String(s);
        String[] raw_addr = str.split("\n");
        this.addresses = new InetAddress[raw_addr.length];
        for (int i = 0; i < raw_addr.length; ++i) {
            this.addresses[i] =
                    InetAddress.getByName(raw_addr[i]);
        }
    }

    @Override
    public void serialize() throws IOException {
        ByteArrayOutputStream bos;
        bos = new ByteArrayOutputStream();

        DataOutputStream dos = new DataOutputStream(bos);

        StringBuilder str = new StringBuilder();
        for (int i = 0; i < this.addresses.length; ++i) {
            str.append(this.addresses[i].getHostAddress());
            str.append('\n');
        }
        byte[] res = str.toString().getBytes();
        dos.writeInt(res.length);
        dos.write(res);
        dos.flush();
        bos.flush();
        this.data = bos.toByteArray();
    }
}
