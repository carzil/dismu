package com.dismu.p2p.server;

import com.dismu.p2p.packets.PacketManager;
import com.dismu.p2p.utilities.PacketSerialize;
import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.RequestSeedsPacket;
import com.dismu.p2p.packets.RequestSeedsResponsePacket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ServerWorker implements Runnable {
    private final Socket clientSocket;

    public ServerWorker(Socket s) {
        this.clientSocket = s;
    }

    @Override
    public void run() {
        try {
            OutputStream outputStream = clientSocket.getOutputStream();
            InputStream inputStream = clientSocket.getInputStream();

            Packet packet = PacketSerialize.readPacket(inputStream);

            if (packet instanceof RequestSeedsPacket) {
                RequestSeedsResponsePacket rp = new RequestSeedsResponsePacket();
                rp.addresses = new InetAddress[2];
                rp.addresses[0] = InetAddress.getLocalHost();
                rp.addresses[1] = InetAddress.getByName("8.8.8.8");
                rp.write(outputStream);
            }

            outputStream.close();
            inputStream.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
