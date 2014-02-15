package com.dismu.p2p.utilities;

import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.RequestSeedsPacket;

import java.io.IOException;
import java.io.InputStream;


public class PacketSerialize {
    public static Packet readPacket(InputStream is) throws IOException {
        Class[] cl = new Class[1];
        cl[0] = RequestSeedsPacket.class;

        Packet packet = new Packet();
        packet.read(is);

        for (Class cPacket : cl) {
            try {
                Packet p = (Packet) cPacket.newInstance();
                p.data = packet.data;
                p.type = packet.type;

                if (p.isMine()) {
                    p.parse();
                    return p;
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return packet;
    }
}
