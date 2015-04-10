package com.dismu.p2p;

import com.dismu.api.ConnectionAPI;
import com.dismu.api.Seed;
import com.dismu.logging.Loggers;
import com.dismu.music.Track;
import com.dismu.music.storages.TrackStorage;
import com.dismu.p2p.client.Client;
import com.dismu.p2p.server.NIOServer;
import com.dismu.p2p.server.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

public class Peer {
    private final String userId;
    private final String groupId;
    private final int serverPort;
    private TrackStorage storage;
    private Server server;
    public ArrayList<Client> clients = new ArrayList<>();
    private HashMap<Seed, Client> seedsTable = new HashMap<>();

    public Peer(String userId, String groupId, int serverPort, TrackStorage storage) {
        this.userId = userId;
        this.groupId = groupId;
        this.storage = storage;
        this.serverPort = serverPort;
    }

    public void updateSeeds() {
        final String userId = getUserId();
        final ConnectionAPI api = new ConnectionAPI();
        Seed[] seeds = api.getNeighbours(getGroupId());
        Loggers.p2pLogger.info("found {} seed(s)", seeds.length);
        Loggers.p2pLogger.info("userID={}", userId);
        for (final Seed s : seeds) {
            if (!seedsTable.containsKey(s)) {
                if (s.userId.equals(userId)) {
                    continue;
                }

                Loggers.p2pLogger.info("got new seed {}", s);

                final Client client = new Client(s.localIP, s.port, userId, storage);
                seedsTable.put(s, client);
                Thread clientThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            client.start();
                            if (client.isConnected()) {
                                clients.add(client);
                                client.synchronize();
                            }
                        } catch (IOException e) {
                            Loggers.uiLogger.error("error in client", e);
                        }
                    }
                });
                clientThread.start();
            }
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getGroupId() {
        return groupId;
    }

    public TrackStorage getStorage() {
        return storage;
    }

    public void setStorage(TrackStorage storage) {
        this.storage = storage;
    }

    public void start() {
        final ConnectionAPI api = new ConnectionAPI();
        final String userId = getUserId();

        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    server = new NIOServer(serverPort, storage);
                } catch (IOException e) {
                    Loggers.p2pLogger.error("starting server failed", e);
                    throw new RuntimeException(e);
                }
                try {
                    Loggers.p2pLogger.info("starting server at port={}", serverPort);
                    server.start();
                } finally {
                    api.unregister(userId);
                }
            }
        });
        serverThread.start();
        String localIP = "";
        try {
            localIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            Loggers.p2pLogger.debug("cannot resolve local host IP");
            Loggers.p2pLogger.debug("we will not register new seed");
        }

        api.register(userId, getGroupId(), localIP, serverPort);
        initClients();
    }

    public void stop() {
        ConnectionAPI api = new ConnectionAPI();
        api.unregister(userId);
        stopClients();
        server.stop();
    }

    private void initClients() {
        updateSeeds();
    }

    public void newTrackAvailable(Track track) {
        initClients();
        for (Client cl : clients) {
            try {
                cl.emitNewTrackEvent(track);
            } catch (IOException e) {
                Loggers.clientLogger.error("error while emitting new package", e);
            }
        }
    }

    private void stopClients() {
        int cnt = 0;
        for (Client client : clients) {
            try {
                client.stop();
                cnt++;
            } catch (IOException e) {
                Loggers.uiLogger.error("error while stopping client", e);
            }
        }
        Loggers.uiLogger.info("closed {} clients", cnt);
    }

    public void startSync() {
        updateSeeds();
        for (Client client : clients) {
            try {
                client.synchronize();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
