package com.dismu.p2p.packets.transaction;

import com.dismu.music.Track;
import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.PacketType;

import java.io.*;

public class NewTrackAvailablePacket extends Packet {
    public Track track;
    public String userId;

    public NewTrackAvailablePacket() {
        this.type = PacketType.PT_NEW_TRACK_AVAILABLE;
    }

    @Override
    public void parse() throws IOException {
        DataInputStream is = new DataInputStream(new ByteArrayInputStream(this.data));
        int size = is.readInt();
        byte[] uid_byte = new byte[size];
        is.read(uid_byte);
        this.userId = new String(uid_byte);
        track = Track.readFromStream(is);
    }

    @Override
    public void serialize() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(this.userId.getBytes().length);
        dos.write(this.userId.getBytes());
        track.writeToStream(dos);
        dos.flush();
        bos.flush();
        this.data = bos.toByteArray();
    }
}
