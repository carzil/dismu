package com.dismu.p2p.scenarios;

import com.dismu.music.storages.TrackStorage;
import com.dismu.api.ConnectionAPI;
import com.dismu.api.Seed;
import com.dismu.p2p.client.Client;
import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.transaction.NewTrackAvailablePacket;

import java.io.IOException;

public class NewTrackAvailableDownloadScenario implements IScenario {
    private final TrackStorage storage;

    public NewTrackAvailableDownloadScenario(TrackStorage storage) {
        this.storage = storage;
    }

    @Override
    public boolean isMine(Packet p) {
        return p instanceof NewTrackAvailablePacket;
    }

    @Override
    public Packet[] handle(Packet p) throws IOException {
        final NewTrackAvailablePacket packet = (NewTrackAvailablePacket) p;
        if (storage.getTrackFile(packet.track) != null) {
            return new Packet[0];
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                ConnectionAPI api = new ConnectionAPI();
                Seed[] s = api.getNeighbours(packet.userId);
                Seed seed = null;
                for (Seed curr : s) {
                    if (curr.userId.equals(packet.userId)) {
                        seed = curr;
                        break;
                    }
                }
                if (seed == null) {
                    return;
                }
                Client client = new Client(seed.localIP, seed.port, "", storage);
                try {
                    client.start();
                    client.receiveTrack(packet.track);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
        return new Packet[0];
    }

    @Override
    public boolean isFinished() {
        return false;
    }
}
