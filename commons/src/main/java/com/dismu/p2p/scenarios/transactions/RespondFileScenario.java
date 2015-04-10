package com.dismu.p2p.scenarios.transactions;

import com.dismu.logging.Loggers;
import com.dismu.music.storages.TrackStorage;
import com.dismu.music.Track;
import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.transaction.*;
import com.dismu.p2p.scenarios.IScenario;
import com.dismu.p2p.scenarios.TransactionTypes;
import com.dismu.p2p.utils.TransactionIdPool;
import com.dismu.utils.MediaUtils;
import com.dismu.utils.Utils;

import java.io.*;
import java.util.Collection;

public class RespondFileScenario implements IScenario {
    private static final int CHUNK_SIZE = 8192;
    private static final int ST_WAITING_FOR_START = 0;
    private static final int ST_WAITING_FOR_CHUNKS = 1;
    private static final int ST_FINISHED = 2;

    private int state = ST_WAITING_FOR_START;

    private final TrackStorage storage;
    private int transactionId = -1;

    private InputStream stream;
    private int size;
    private long lastPos;

    public RespondFileScenario(TrackStorage storage) {
        this.storage = storage;
    }

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

            Collection<Track> tracks = storage.getTracks();

            if (packet.getTransactionType() == TransactionTypes.GET_TRACK_LIST) {
                byte[] trackListToByteArray = MediaUtils.trackListToByteArray(tracks);
                stream = new ByteArrayInputStream(trackListToByteArray);
                response.fileSize = trackListToByteArray.length;
                this.size = trackListToByteArray.length;
                response.fileHash = Utils.getByteArrayHash(trackListToByteArray);
                this.lastPos = 0;
            } else if (packet.getTransactionType() == TransactionTypes.GET_TRACK) {
                Loggers.p2pLogger.debug("start GET_TRACK transaction, track hash = '{}'", packet.getTrackHash());

                Track track = storage.getTrackByHash(packet.getTrackHash());

                if (track == null) {
                    response.error = "404";
                    return new Packet[]{response};
                } else {
                    File file = storage.getTrackFile(track);
                    try {
                        response.fileHash = Utils.getFileHash64(file);
                        response.fileSize = (int) file.length();
                        this.size = (int) file.length();
                        this.stream = new BufferedInputStream(new FileInputStream(file));
                        this.lastPos = 0;
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
            Loggers.p2pLogger.debug("got RequestChunkPacket, offset = {}, count = {}", packet.offset, packet.count);

            ResponseChunkPacket response = new ResponseChunkPacket();
            response.transactionId = this.transactionId;

            try {
                if (this.lastPos > packet.offset) {
                    this.stream.reset();
                    this.stream.skip(packet.offset);
                    this.lastPos = packet.offset;
                } else if (this.lastPos < packet.offset) {
                    this.stream.skip(packet.offset - this.lastPos);
                }
                response.chunk = new byte[packet.count];
                int readCount = stream.read(response.chunk, 0, packet.count);
                lastPos += readCount;
                response.offset = packet.offset;
                response.count = readCount;

                response.computeHash();
                return new Packet[] {response};
            } catch (IOException e) {
                Loggers.p2pLogger.error("i/o error while sending chunk", e);
                return new Packet[0];
            }
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
