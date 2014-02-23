package com.dismu.p2p.client;

import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.node_control.ExitPacket;
import com.dismu.p2p.packets.node_control.RequestSeedsPacket;
import com.dismu.p2p.packets.node_control.RequestSeedsResponsePacket;
import com.dismu.p2p.packets.transaction.StartTransactionPacket;
import com.dismu.p2p.scenarios.RequestFileScenario;
import com.dismu.p2p.utils.Loggers;
import com.dismu.p2p.utils.PacketSerialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

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

        StartTransactionPacket stp = new StartTransactionPacket();
        stp.filename = "oh";
        stp.write(os);

        RequestFileScenario rfc = new RequestFileScenario();
        Packet packet;
        while (true) {
            try {
                packet = PacketSerialize.readPacket(in);
            } catch (SocketException e) {
                Loggers.clientLogger.error("server disconnected");
                break;
            }

            Packet[] packets = rfc.handle(packet);
            for (Packet sp : packets) {
                sp.write(os);
            }

            if (rfc.isFinished()) {
                break;
            }
        }

        String file = new String(rfc.data);
        Loggers.clientLogger.info("Received new file: {}", file);

        ExitPacket ep = new ExitPacket();
        ep.write(os);

        os.close();
        in.close();
        socket.close();
    }
}
