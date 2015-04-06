package com.dismu.p2p.utils;

import com.dismu.logging.Loggers;
import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.transaction.StartTransactionPacket;
import com.dismu.p2p.scenarios.transactions.RequestFileScenario;

import java.io.*;
import java.net.SocketException;

public class TransactionHelper {
    private OutputStream os;
    private InputStream in;

    public TransactionHelper(OutputStream os, InputStream in) {
        this.os = os;
        this.in = in;
    }

    public InputStream startTransaction(StartTransactionPacket stp) throws IOException {
        stp.write(os);

        RequestFileScenario rfc = new RequestFileScenario();
        Packet packet;
        while (true) {
            try {
                packet = PacketSerialize.readPacket(in);
            } catch (SocketException e) {
                Loggers.clientLogger.error("server disconnected");
                break;
            }

            Packet[] packets = rfc.handle(packet);
            for (Packet sp : packets) {
                sp.write(os);
            }

            if (rfc.isFinished()) {
                break;
            }
        }

        Loggers.clientLogger.info("Received new file");
        return new BufferedInputStream(new FileInputStream(rfc.data_file));
    }
}
