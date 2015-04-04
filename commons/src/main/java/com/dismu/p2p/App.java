package com.dismu.p2p;

import com.dismu.logging.Loggers;
import com.dismu.api.ConnectionAPI;
import com.dismu.api.Seed;
import com.dismu.p2p.client.Client;
import com.dismu.p2p.server.NIOServer;
import com.dismu.p2p.server.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class App {
    private static App instance;
    private boolean hasStarted = false;
    private ArrayList<Client> clients;
    private Server server;
    private String userId;
    private String groupId;
    private String localIP;
    private int port;

    private App() {
    }

    public static App getInstance() {
        if (App.instance == null) {
            App.instance = new App();
        }
        return App.instance;
    }

    public static void main(final String[] args) {
        String userId = "pi";
        int port = 1337;
        String localIP = "";

        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        if (args.length >= 3) {
            userId = args[0];
            port = Integer.valueOf(args[1]);
            localIP = args[2];
        }
        App.getInstance().start(userId, "alpha", localIP, port);
    }

    synchronized public void start(final String userId, final String groupId, String localIP, final int port) {
        if (hasStarted && !localIP.equals(this.localIP)) {
            restart(localIP);
        }
        hasStarted = true;
        clients = new ArrayList<Client>();

        this.userId = userId;
        this.groupId = groupId;
        this.localIP = localIP;
        this.port = port;

        final ConnectionAPI api = new ConnectionAPI();
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    server = new NIOServer(port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    server.start();
                } catch (Exception e) {
                    Loggers.serverLogger.error("", e);
                } finally {
//                    api.unregister(userId);
                }
            }
        });
        serverThread.start();
//        api.register(userId, groupId, localIP, port);
//        Seed[] seeds = api.getNeighbours(userId);
//        System.out.print(seeds.length);
//        for (final Seed s : seeds) {
//            if (s.userId.equals(userId)) {
//                continue;
//            }
//
//            Thread clientThread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    Client client = new Client(s.localIP, s.port, userId);
//                    try {
//                        client.start();
//                        clients.add(client);
//                        client.synchronize();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
//            clientThread.start();
//
//        }
    }

    synchronized public void restart(String localIP) {
        stop();

        this.localIP = localIP;

        start(userId, groupId, this.localIP, port);
    }

    synchronized public void stop() {
        for (Client c : clients) {
            try {
                c.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        clients.clear();
        server.stop();
        final ConnectionAPI api = new ConnectionAPI();
//        api.unregister(userId);
        hasStarted = false;
    }
}
