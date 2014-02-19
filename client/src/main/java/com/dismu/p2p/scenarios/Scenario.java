package com.dismu.p2p.scenarios;

import com.dismu.p2p.packets.Packet;

public abstract class Scenario {
    public abstract boolean isMine(Packet p);

    public abstract Packet[] handle(Packet p);

    public abstract boolean isFinished();
}
