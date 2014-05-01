package com.dismu.p2p.apiclient;

public interface API {
    public Seed[] getNeighbours(String userId);

    public void register(String userId, String groupId, String localIP, int port);

    public void unregister(String userId);
}
