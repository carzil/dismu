package com.dismu.p2p.scenarios;

import com.dismu.music.player.PCTrackStorage;
import com.dismu.music.player.Track;
import com.dismu.music.player.TrackStorage;
import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.transaction.*;
import com.dismu.p2p.utils.TransactionIdPool;
import com.dismu.utils.MediaUtils;
import com.dismu.utils.Utils;

import java.io.*;
import java.util.ArrayList;

public class RespondFileScenario extends Scenario {
    private static final int ST_WAITING_FOR_START = 0;
    private static final int ST_WAITING_FOR_CHUNKS = 1;
    private static final int ST_FINISHED = 2;

    private int state = ST_WAITING_FOR_START;
    private int transactionId = -1;

    private InputStream stream;
    private int size;

    @Override
    public boolean isMine(Packet p) {
        if (p instanceof StartTransactionPacket && this.state == ST_WAITING_FOR_START) {
            return true;
        }

        if (p instanceof RequestChunkPacket && this.state == ST_WAITING_FOR_CHUNKS) {
            if (((RequestChunkPacket) p).transactionId == this.transactionId) {
                return true;
            }
        }

        if (p instanceof EndTransactionPacket && this.state == ST_WAITING_FOR_CHUNKS) {
            if (((EndTransactionPacket) p).transactionId == this.transactionId) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Packet[] handle(Packet p) {
        if (p instanceof StartTransactionPacket) {
            assert(this.state == ST_WAITING_FOR_START);
            this.state = ST_WAITING_FOR_CHUNKS;
            StartTransactionPacket packet = (StartTransactionPacket) p;

            this.transactionId = TransactionIdPool.getNext();

            AcceptTransactionPacket response = new AcceptTransactionPacket();
            response.transactionId = this.transactionId;

            TrackStorage ts = new PCTrackStorage();
            Track[] tracks = ts.getTracks();

            if (packet.filename.equals("tracklist")) {
                byte[] tracklist = MediaUtils.TrackListToByteArray(tracks);
                stream = new ByteArrayInputStream(tracklist);
                response.fileSize = tracklist.length;
                this.size = tracklist.length;
                try {
                    response.fileHash =
                            Utils.getAdler32StreamHash(new ByteArrayInputStream(tracklist));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (packet.filename.startsWith("tracks/")) {
                packet.filename = packet.filename.replaceFirst("tracks/", "");
                String[] exploded = packet.filename.split("/");
                if (exploded.length != 3) {
                    response.error = "400";
                    return new Packet[]{response};
                }

                Track track = null;
                for (Track curr : tracks) {
                    if (!curr.getTrackArtist().equals(exploded[0])) {
                        continue;
                    }
                    if (!curr.getTrackName().equals(exploded[1])) {
                        continue;
                    }
                    if (!curr.getTrackAlbum().equals(exploded[2])) {
                        continue;
                    }
                    track = curr;
                }
                if (track == null) {
                    response.error = "404";
                    return new Packet[]{response};
                } else {
                    File file = ts.getTrackFile(track);
                    try {
                        response.fileHash = Utils.getAdler32FileHash(file);
                        response.fileSize = (int) file.length();
                        this.size = (int) file.length();
                        this.stream = new BufferedInputStream(new FileInputStream(file));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            response.error = "";

            return new Packet[]{response};
        }

        if (p instanceof RequestChunkPacket) {
            assert(this.state == ST_WAITING_FOR_CHUNKS);
            RequestChunkPacket packet = (RequestChunkPacket) p;
            assert(packet.transactionId == this.transactionId);

            ResponseChunkPacket response = new ResponseChunkPacket();
            response.transactionId = this.transactionId;

            ArrayList<Byte> sb = new ArrayList<>();
            try {
                for (int i = packet.offset; i < Math.min(packet.count+packet.offset, this.size); ++i) {
                    int a = this.stream.read();
                    if (a == -1) {
                        break;
                    }
                    sb.add((byte) a);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            response.chunk = new byte[sb.size()];
            for (int i = 0; i < sb.size(); ++i) {
                response.chunk[i] = sb.get(i);
            }
            response.offset = packet.offset;
            response.count = sb.size();

            response.computeHash();
            return new Packet[]{response};
        }

        if (p instanceof EndTransactionPacket) {
            assert(this.state != ST_FINISHED);
            this.state = ST_FINISHED;
            return new Packet[0];
        }

        return new Packet[0];
    }

    @Override
    public boolean isFinished() {
        return this.state == ST_FINISHED;
    }
}
