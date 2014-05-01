package com.dismu.p2p.apiclient;

import java.net.InetAddress;

public class Seed {
    public String userId, groupId;
    public InetAddress localIP, remoteIP;
    public int port;

    public boolean equals(Object other) {
        if (other instanceof Seed) {
            Seed seed = (Seed)other;
            return seed.userId.equals(userId) && seed.groupId.equals(groupId) && seed.localIP.equals(localIP) && seed.remoteIP.equals(remoteIP) && seed.port == port;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return userId.hashCode() ^ groupId.hashCode() ^ localIP.hashCode() ^ remoteIP.hashCode() ^ port;
    }

    public String toString() {
        return String.format("Seed[localIP=%s, remoteIP=%s, userID=%s, groupID=%s, port=%d, hashCode=%s]", localIP, remoteIP, userId, groupId, port, hashCode());
    }
}
