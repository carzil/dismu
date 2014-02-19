package com.dismu.p2p.client;

import com.dismu.p2p.packets.ExitPacket;
import com.dismu.p2p.packets.RequestSeedsPacket;
import com.dismu.p2p.packets.RequestSeedsResponsePacket;
import com.dismu.p2p.utils.PacketSerialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Client {
    private InetAddress address;
    private int port;
    private Socket socket;

    public static void main(String[] args) throws IOException {
        Client client = new Client(InetAddress.getLocalHost(), 1775);
        try {
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Client(InetAddress addr, int port) {
        this.address = addr;
        this.port = port;
    }

    public void start() throws IOException {
        socket = new Socket(address, port);
        OutputStream os = socket.getOutputStream();
        InputStream in = socket.getInputStream();

        for (int i = 0; i < 2; ++i) {
            RequestSeedsPacket rsp = new RequestSeedsPacket();
            rsp.groupId = 1;
            rsp.write(os);

            RequestSeedsResponsePacket rsrp;
            rsrp = (RequestSeedsResponsePacket) PacketSerialize.readPacket(in);
        }

        ExitPacket ep = new ExitPacket();
        ep.write(os);

        os.close();
        in.close();
        socket.close();
    }
}
