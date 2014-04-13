package com.dismu.p2p;

import com.dismu.logging.Loggers;
import com.dismu.p2p.apiclient.API;
import com.dismu.p2p.apiclient.APIImpl;
import com.dismu.p2p.apiclient.Seed;
import com.dismu.p2p.client.Client;
import com.dismu.p2p.server.Server;

import java.io.IOException;

public class App {
    public static void main(final String[] args) {
        final String userId = args[0];
        final int port = Integer.valueOf(args[1]);

        start(userId, args[2], port);
    }

    public static void start(final String userId, String localIP, final int port) {
        final API api = new APIImpl();
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Server server = new Server(port);
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
        api.register(userId, "alpha", localIP, port);
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
                        client.synchronize();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            clientThread.start();
        }
    }
}
