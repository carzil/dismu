package com.dismu.p2p.packets;

import com.dismu.p2p.packets.Packet;

import java.io.*;


public class RequestSeedsPacket extends Packet {
    final int PT_REQUEST_SEEDS = 0;

    public int groupId;

    public RequestSeedsPacket() {
        this.type = PT_REQUEST_SEEDS;
    }

    @Override
    public boolean isMine() {
        return this.type == PT_REQUEST_SEEDS;
    }

    @Override
    public void parse() {
        try {
            ByteArrayInputStream bis;
            bis = new ByteArrayInputStream(this.data);

            DataInputStream dis = new DataInputStream(bis);
            groupId = dis.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void serialize() {
        try {
            ByteArrayOutputStream bos;
            bos = new ByteArrayOutputStream();

            DataOutputStream dos = new DataOutputStream(bos);

            dos.writeInt(groupId);
            dos.flush();
            bos.flush();
            this.data = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
