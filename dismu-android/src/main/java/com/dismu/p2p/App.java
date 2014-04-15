package com.dismu.p2p;

import com.dismu.logging.Loggers;
import com.dismu.p2p.apiclient.API;
import com.dismu.p2p.apiclient.APIImpl;
import com.dismu.p2p.apiclient.Seed;
import com.dismu.p2p.client.Client;
import com.dismu.p2p.server.Server;

import java.io.IOException;
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
        final String userId = args[0];
        final int port = Integer.valueOf(args[1]);

        App.getInstance().start(userId, "alpha", args[2], port);
    }

    synchronized public void start(final String userId, final String groupId, String localIP, final int port) {
        if (hasStarted) {
            return;
        }
        hasStarted = true;
        clients = new ArrayList<Client>();

        this.userId = userId;
        this.groupId = groupId;
        this.localIP = localIP;
        this.port = port;

        final API api = new APIImpl();
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                server = new Server(port);
                try {
                    server.start();
                } catch (Exception e) {
                    Loggers.serverLogger.error("", e);
                } finally {
                    api.unregister(userId);
                }
            }
        });
        serverThread.start();
        api.register(userId, groupId, localIP, port);
        Seed[] seeds = api.getNeighbours(userId);
        System.out.print(seeds.length);
        for (final Seed s : seeds) {
            if (s.userId.equals(userId)) {
                continue;
            }

            Thread clientThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Client client = new Client(s.localIP, s.port, userId);
                    try {
                        client.start();
                        clients.add(client);
                        client.synchronize();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            clientThread.start();

        }
    }

    synchronized public void restart() {
        for (Client c : clients) {
            try {
                c.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        clients.clear();
        server.stop();
        final API api = new APIImpl();
        api.unregister(userId);
        hasStarted = false;

        start(userId, groupId, localIP, port);
    }
}
