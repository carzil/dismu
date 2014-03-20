package com.dismu.p2p.client;

import com.dismu.music.player.Track;
import com.dismu.p2p.packets.node_control.ExitPacket;
import com.dismu.p2p.utils.TransactionHelper;
import com.dismu.utils.MediaUtils;
import com.dismu.utils.Utils;

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

        TransactionHelper helper = new TransactionHelper(os, in);
        InputStream is = helper.receiveFile("tracklist");
        byte[] tracks_bytes = Utils.readStreamToBytes(is);

        Track[] tracks = MediaUtils.ByteArrayToTrackList(tracks_bytes);

        // 1. ???? # TODO
        // 2. PROFIT

        ExitPacket ep = new ExitPacket();
        ep.write(os);

        os.close();
        in.close();
        socket.close();
    }

}
