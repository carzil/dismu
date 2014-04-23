package com.dismu.p2p.apiclient;

import java.net.InetAddress;

public class Seed {
    public String userId, groupId;
    public InetAddress localIP, remoteIP;
    public int port;

    public boolean equals(Object other) {
        if (other instanceof Seed) {
            Seed seed = (Seed)other;
            return seed.userId.equals(userId) && seed.groupId.equals(groupId);
        } else {
            return false;
        }
    }
}
