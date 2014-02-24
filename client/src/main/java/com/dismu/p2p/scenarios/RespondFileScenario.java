package com.dismu.p2p.scenarios;

import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.transaction.*;
import com.dismu.logging.Loggers;
import com.dismu.p2p.utils.TransactionIdPool;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.zip.Adler32;

public class RespondFileScenario extends Scenario {
    private static final int ST_WAITING_FOR_START = 0;
    private static final int ST_WAITING_FOR_CHUNKS = 1;
    private static final int ST_FINISHED = 2;

    private int state = ST_WAITING_FOR_START;
    private int transactionId = -1;

    private byte[] sample_data = new byte[6 * (1 << 21)];

    public RespondFileScenario() {
        try {
            FileInputStream fis = new FileInputStream("rise_against-prayer_of_the_refugee.mp3");
            fis.read(this.sample_data);
        } catch (Exception e) {
            Loggers.serverLogger.error("oops", e);
        }
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

            Adler32 adler32 = new Adler32();
            adler32.update(sample_data, 0, sample_data.length);

            Loggers.serverLogger.info("{}", sample_data.length);

            response.fileHash = adler32.getValue();
            response.fileSize = sample_data.length;
            response.error = "";

            return new Packet[]{response};
        }

        if (p instanceof RequestChunkPacket) {
            assert(this.state == ST_WAITING_FOR_CHUNKS);
            RequestChunkPacket packet = (RequestChunkPacket) p;
            assert(packet.transactionId == this.transactionId);

            ResponseChunkPacket response = new ResponseChunkPacket();
            response.transactionId = this.transactionId;

            ArrayList<Byte> sb = new ArrayList<Byte>();
            for (int i = packet.offset; i < Math.min(packet.count+packet.offset, sample_data.length); ++i) {
                sb.add(sample_data[i]);
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
