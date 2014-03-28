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
        final API api = new APIImpl();
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Server server = new Server(Integer.valueOf(args[1]));
                try {
                    server.start();
                } catch (Exception e) {
                    Loggers.serverLogger.error("", e);
                } finally {
                    api.unregister(args[0]);
                }
            }
        });
        serverThread.start();
        api.register(args[0], "alpha", Integer.valueOf(args[1]));
        Seed[] seeds = api.getNeighbours(args[0]);
        System.out.print(seeds.length);
        for (final Seed s : seeds) {
            if (s.userId.equals(args[0])) {
                continue;
            }

            Thread clientThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Client client = new Client(s.localIP, s.port, args[0]);
                    try {
                        client.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            clientThread.start();
        }
    }
}
