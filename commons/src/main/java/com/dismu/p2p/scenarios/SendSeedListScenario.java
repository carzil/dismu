package com.dismu.p2p.scenarios;

import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.node_control.RequestSeedsPacket;
import com.dismu.p2p.packets.node_control.RequestSeedsResponsePacket;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class SendSeedListScenario implements IScenario {
    private boolean handled = false;

    public SendSeedListScenario() {
    }

    @Override
    public boolean isMine(Packet p) {
        return p instanceof RequestSeedsPacket;
    }

    @Override
    public Packet[] handle(Packet p) {
        assert (p instanceof RequestSeedsPacket);

        RequestSeedsResponsePacket rp = new RequestSeedsResponsePacket();
        try {
            rp.addresses = new InetAddress[2];
            rp.addresses[0] = InetAddress.getLocalHost();
            rp.addresses[1] = InetAddress.getByName("8.8.8.8");
            this.handled = true;
            return new Packet[]{rp};
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return new Packet[0];
    }

    @Override
    public boolean isFinished() {
        return this.handled;
    }
}
