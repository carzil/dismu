package com.dismu.p2p.server;

import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.utilities.PacketSerialize;
import com.dismu.p2p.packets.RequestSeedsPacket;
import com.dismu.p2p.packets.RequestSeedsResponsePacket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ServerWorker implements Runnable {
    private Socket socket;

    public ServerWorker(Socket s) {
        this.socket = s;
    }

    @Override
    public void run() {
        try {
            OutputStream os = this.socket.getOutputStream();
            InputStream is = this.socket.getInputStream();

            Packet packet = null;
            packet = PacketSerialize.readPacket(is);
            if (packet instanceof RequestSeedsPacket) {
                RequestSeedsResponsePacket rp = new RequestSeedsResponsePacket();
                rp.addresses = new InetAddress[2];
                rp.addresses[0] = InetAddress.getLocalHost();
                rp.addresses[1] = InetAddress.getByName("8.8.8.8");
                rp.write(os);
            }

            os.close();
            is.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
