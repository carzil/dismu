package com.dismu.p2p.scenarios;

import com.dismu.p2p.packets.Packet;

import java.io.IOException;

public abstract class Scenario {
    public abstract boolean isMine(Packet p);

    public abstract Packet[] handle(Packet p) throws IOException;

    public abstract boolean isFinished();
}
