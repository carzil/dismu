package com.dismu.p2p.scenarios;

import com.dismu.logging.Loggers;
import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.transaction.AcceptTransactionPacket;
import com.dismu.p2p.packets.transaction.EndTransactionPacket;
import com.dismu.p2p.packets.transaction.RequestChunkPacket;
import com.dismu.p2p.packets.transaction.ResponseChunkPacket;
import com.dismu.utils.Utils;

import java.io.*;

public class RequestFileScenario extends Scenario {
    private static final int ST_WAITING_FOR_ACCEPT = 0;
    private static final int ST_WAITING_FOR_CHUNKS = 1;
    private static final int ST_FINISHED = 2;

    private static final int BLOCK_SIZE = 256*1024;

    private int state = ST_WAITING_FOR_ACCEPT;
    private int transactionId = -1;

    private long fileHash = -1;
    private int fileSize = -1;

    private int guaranteedGot = -1;
    private OutputStream data_os = null;
    public File data_file = null;

    @Override
    public boolean isMine(Packet p) {
        if (p instanceof AcceptTransactionPacket && this.state == ST_WAITING_FOR_ACCEPT) {
            return true;
        }

        if (p instanceof ResponseChunkPacket && this.state == ST_WAITING_FOR_CHUNKS) {
            if (((ResponseChunkPacket) p).transactionId == this.transactionId) {
                return true;
            }
        }

        return false;
    }

    private RequestChunkPacket generateChunkRequestPacket(int offset, int count) {
        RequestChunkPacket result = new RequestChunkPacket();
        result.transactionId = this.transactionId;
        result.offset = offset;
        result.count = Math.min(count, fileSize - offset);
        return result;
    }

    @Override
    public Packet[] handle(Packet p) throws IOException {
        if (p instanceof AcceptTransactionPacket) {
            assert(this.state == ST_WAITING_FOR_ACCEPT);
            this.state = ST_WAITING_FOR_CHUNKS;
            AcceptTransactionPacket packet = (AcceptTransactionPacket) p;

            if (!packet.error.isEmpty()) {
                Loggers.clientLogger.error("Error while accepting: {}", packet.error);
                this.state = ST_FINISHED;
                throw new IOException(packet.error);
                //return new Packet[0];
            }

            this.transactionId = packet.transactionId;
            this.fileHash = packet.fileHash;
            this.fileSize = packet.fileSize;

            this.data_file = File.createTempFile("dismu-"+Integer.valueOf(this.transactionId).toString(), null, Utils.getAppFolderPath());
            this.data_file.deleteOnExit();
            this.data_os = new FileOutputStream(this.data_file);
            this.guaranteedGot = 0;

            Loggers.clientLogger.info(
                    "Accepted file request: transactionId = {}, size = {}, hash = {}",
                    this.transactionId, this.fileSize, this.fileHash
            );

            RequestChunkPacket request = generateChunkRequestPacket(this.guaranteedGot, BLOCK_SIZE);

            Loggers.clientLogger.info(
                    "Requesting new chunk: transactionId = {}, offset = {}, count = {}",
                    this.transactionId, request.offset, request.count
            );

            return new Packet[]{request};
        }

        if (p instanceof ResponseChunkPacket) {
            assert(this.state == ST_WAITING_FOR_CHUNKS);
            ResponseChunkPacket packet = (ResponseChunkPacket) p;
            assert(packet.transactionId == this.transactionId);

            Loggers.clientLogger.info(
                    "Got new chunk: transactionId = {}, offset = {}, count = {}, hash = {}",
                    packet.transactionId, packet.offset, packet.count, packet.chunkHash
            );

            if (packet.offset != this.guaranteedGot) {
                RequestChunkPacket request = generateChunkRequestPacket(this.guaranteedGot, BLOCK_SIZE);
                Loggers.clientLogger.info(
                        "Wrong chunk sent, resending request: transactionId = {}, offset = {}, count = {}",
                        this.transactionId, request.offset, request.count
                );
                return new Packet[]{request};
            }

            if (!packet.checkHash()) {
                RequestChunkPacket request = generateChunkRequestPacket(this.guaranteedGot, BLOCK_SIZE);
                Loggers.clientLogger.info(
                        "Chunk hash check failed, resending request: transactionId = {}, offset = {}, count = {}",
                        this.transactionId, request.offset, request.count
                );
                return new Packet[]{request};
            }

            this.data_os.write(packet.chunk);

            this.guaranteedGot += packet.count;

            Loggers.clientLogger.info(
                    "Processed chunk: transactionId = {}, offset = {}, count = {}",
                    this.transactionId, packet.offset, packet.count
            );

            if (this.guaranteedGot >= this.fileSize) {
                this.state = ST_FINISHED;
                Loggers.clientLogger.info(
                        "Finished receiving file: transactionId = {}",
                        this.transactionId
                );
                this.data_os.flush();
                this.data_os.close();
                EndTransactionPacket response = new EndTransactionPacket();
                response.transactionId = this.transactionId;
                return new Packet[]{response};
            }

            RequestChunkPacket request = generateChunkRequestPacket(this.guaranteedGot, BLOCK_SIZE);
            Loggers.clientLogger.info(
                    "Requesting new chunk: transactionId = {}, offset = {}, count = {}",
                    this.transactionId, request.offset, request.count
            );

            return new Packet[]{request};
        }

        return new Packet[0];
    }

    @Override
    public boolean isFinished() {
        return this.state == ST_FINISHED;
    }
}
