package com.dismu.p2p.scenarios;

import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.RequestSeedsResponsePacket;

import java.net.InetAddress;

public class GetSeedListScenario extends Scenario {
    public InetAddress[] addresses = null;
    private boolean handled = false;

    @Override
    public boolean isMine(Packet p) {
        return p instanceof RequestSeedsResponsePacket;
    }

    @Override
    public Packet[] handle(Packet p) {
        assert (p instanceof RequestSeedsResponsePacket);

        this.addresses =
                ((RequestSeedsResponsePacket) p).addresses;

        this.handled = true;

        return new Packet[0];
    }

    @Override
    public boolean isFinished() {
        return handled;
    }

    synchronized InetAddress[] getAddresses() {
        return this.addresses;
    }
}
