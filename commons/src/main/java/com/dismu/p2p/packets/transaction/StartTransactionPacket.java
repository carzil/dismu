package com.dismu.p2p.packets.transaction;

import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.PacketType;
import com.dismu.p2p.scenarios.TransactionTypes;

import java.io.*;

public class StartTransactionPacket extends Packet {
    private int transactionType = -1;
    private int trackHash = -1;

    public StartTransactionPacket() {
        super();
    }

    private StartTransactionPacket(int type) {
        super();
        this.type = PacketType.PT_START_TRANSACTION;
        this.transactionType = type;
    }

    @Override
    public void parse() throws IOException {
        ByteArrayInputStream bis;
        bis = new ByteArrayInputStream(this.data);

        DataInputStream dis = new DataInputStream(bis);

        this.transactionType = dis.readInt();
        if (transactionType == TransactionTypes.GET_TRACK) {
            this.trackHash = dis.readInt();
        }
    }

    @Override
    public void serialize() throws IOException {
        ByteArrayOutputStream bos;
        bos = new ByteArrayOutputStream();

        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeInt(transactionType);
        if (transactionType == TransactionTypes.GET_TRACK) {
            dos.writeInt(trackHash);
        }

        dos.flush();
        bos.flush();
        this.data = bos.toByteArray();
    }

    public int getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(int transactionType) {
        this.transactionType = transactionType;
    }

    public int getTrackHash() {
        return trackHash;
    }

    public void setTrackHash(int trackHash) {
        this.trackHash = trackHash;
    }

    public static StartTransactionPacket createGetTrack(int trackHash) {
        StartTransactionPacket p = new StartTransactionPacket(TransactionTypes.GET_TRACK);
        p.setTrackHash(trackHash);
        return p;
    }

    public static StartTransactionPacket createGetTrackList() {
        return new StartTransactionPacket(TransactionTypes.GET_TRACK_LIST);
    }
}
