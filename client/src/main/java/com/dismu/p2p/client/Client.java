package com.dismu.p2p.client;

import com.dismu.logging.Loggers;
import com.dismu.music.player.Track;
import com.dismu.p2p.packets.node_control.ExitPacket;
import com.dismu.p2p.packets.transaction.NewTrackAvailablePacket;
import com.dismu.p2p.utils.SyncManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Client {
    private InetAddress address;
    private int port;
    private Socket socket = null;
    private OutputStream os = null;
    private InputStream in = null;
    private String userId;
    private final static int TIMEOUT = 10 * 1000; // in ms
    private boolean isConnected = false;

    public static void main(String[] args) throws IOException {
        Client client = new Client(InetAddress.getLocalHost(), 1775, "a");
        try {
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Client(InetAddress addr, int port, String userId) {
        this.address = addr;
        this.port = port;
        this.userId = userId;
    }

    public void start() throws IOException {
        socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(address, port), TIMEOUT);
            isConnected = true;
        } catch (SocketTimeoutException e) {
            Loggers.clientLogger.info("connection timeout, address='{}', port='{}'", address, port);
            return;
        }
        os = socket.getOutputStream();
        in = socket.getInputStream();
    }

    public void stop() throws IOException {
        ExitPacket ep = new ExitPacket();
        ep.write(os);

        os.close();
        in.close();
        socket.close();
    }

    public void synchronize() throws IOException {
        SyncManager sm = new SyncManager();
        sm.synchronize(in, os);
    }

    public void receiveTrack(Track track) throws IOException {
        SyncManager sm = new SyncManager();
        sm.receiveTrack(track, in, os);
    }

    public void emitNewTrackEvent(Track track) throws IOException {
        NewTrackAvailablePacket packet = new NewTrackAvailablePacket();
        packet.userId = this.userId;
        packet.track = track;
        packet.write(os);
        os.flush();
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public boolean isConnected() {
        return isConnected;
    }
}
