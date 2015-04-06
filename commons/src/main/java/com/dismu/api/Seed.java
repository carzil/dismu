package com.dismu.api;

import java.net.InetAddress;

public class Seed {
    public String userId, groupId;
    public InetAddress localIP, remoteIP;
    public int port;

    public String toString() {
        return "Seed[userId='" + userId + "', groupId='" + groupId + "', localIP='" + localIP + "', port=" + port + "]";
    }
}
