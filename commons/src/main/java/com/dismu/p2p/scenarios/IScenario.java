package com.dismu.p2p.scenarios;

import com.dismu.p2p.packets.Packet;

import java.io.IOException;

public interface IScenario {
    boolean isMine(Packet p);
    Packet[] handle(Packet p) throws IOException;
    boolean isFinished();
}
