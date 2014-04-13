package com.dismu.p2p.utils;

import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.PacketManager;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;


public class PacketSerialize {
    private static PacketManager packetManager = new PacketManager();

    public static Packet readPacket(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        int packetType = dataInputStream.readInt();
        try {
            Packet packet = (Packet)packetManager.getPacket(packetType).newInstance();
            packet.read(inputStream);
            return packet;
        } catch (IllegalAccessException e) {

        } catch (InstantiationException e) {

        }
    return null;
    }
}
