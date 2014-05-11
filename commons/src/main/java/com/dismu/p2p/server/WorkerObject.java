package com.dismu.p2p.server;

import com.dismu.logging.Loggers;
import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.PacketManager;
import com.dismu.p2p.packets.node_control.RequestSeedsPacket;
import com.dismu.p2p.packets.transaction.NewTrackAvailablePacket;
import com.dismu.p2p.packets.transaction.StartTransactionPacket;
import com.dismu.p2p.scenarios.NewTrackAvailableDownloadScenario;
import com.dismu.p2p.scenarios.RespondFileScenario;
import com.dismu.p2p.scenarios.Scenario;
import com.dismu.p2p.scenarios.SendSeedListScenario;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class WorkerObject {
    private final int INT_SIZE = 4;

    private final int ST_NOPE = 0;
    private final int ST_READ_TYPE = 1;
    private final int ST_READ_SIZE = 2;

    int state = ST_NOPE;
    int readCount = 0;

    PacketManager packetManager = new PacketManager();

    Queue<Packet> inPacketQueue = new LinkedList<>();
    Packet currentPacket;

    Queue<Byte> partiallyRead = new LinkedList<>();
    Queue<ByteBuffer> partiallyWrote = new LinkedList<>();

    LinkedList<Scenario> activeScenarios = new LinkedList<>();

    public void parsePackets(byte[] array, int numRead) throws IOException {
        for (int i = 0; i < numRead; ++i) {
            partiallyRead.add(array[i]);
        }

        boolean progressed = true;
        while (progressed) {
            progressed = false;

            if (partiallyRead.size() >= INT_SIZE && state == ST_NOPE) {
                progressed = true;
                byte[] type = new byte[4];
                for (int i = 0; i < 4; ++i) {
                    type[i] = partiallyRead.remove();
                }
                InputStream inputStream = new ByteArrayInputStream(type);
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                int packetType = dataInputStream.readInt();
                try {
                    currentPacket = (Packet) packetManager.getPacket(packetType).newInstance();
                } catch (IllegalAccessException e) {

                } catch (InstantiationException e) {

                }
                state = ST_READ_TYPE;
            }
            if (partiallyRead.size() >= INT_SIZE && state == ST_READ_TYPE) {
                progressed = true;
                byte[] type = new byte[4];
                for (int i = 0; i < 4; ++i) {
                    type[i] = partiallyRead.remove();
                }
                InputStream inputStream = new ByteArrayInputStream(type);
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                int size = dataInputStream.readInt();
                currentPacket.data = new byte[size];
                readCount = 0;
                state = ST_READ_SIZE;
                if (size == 0) {
                    state = ST_NOPE;
                }
            }
            if (partiallyRead.size() > 0 && state == ST_READ_SIZE) {
                progressed = true;
                while (!partiallyRead.isEmpty() && readCount < currentPacket.data.length) {
                    currentPacket.data[readCount++] = partiallyRead.remove();
                }
                if (readCount == currentPacket.data.length) {
                    currentPacket.parse();
                    inPacketQueue.add(currentPacket);
                    currentPacket = null;
                    state = ST_NOPE;
                }
            }
        }

        while (!inPacketQueue.isEmpty()) {
            Packet packet = inPacketQueue.remove();
            Scenario sc = null;
            for (Scenario s : activeScenarios) {
                if (s.isMine(packet)) {
                    sc = s;
                }
            }
            if (null == sc) {
                boolean activated = false;
                if (packet instanceof RequestSeedsPacket) {
                    sc = new SendSeedListScenario();
                    activeScenarios.add(sc);
                    activated = true;
                } else if (packet instanceof StartTransactionPacket) {
                    sc = new RespondFileScenario();
                    activeScenarios.add(sc);
                    activated = true;
                } else if (packet instanceof NewTrackAvailablePacket) {
                    sc = new NewTrackAvailableDownloadScenario();
                }
                if (activated) {
                    Loggers.serverLogger.info("activated {}", sc.getClass().getSimpleName());
                }
            }
            if (null == sc) {
                Loggers.serverLogger.warn("got packet for unknown scenario");
            } else {
                Loggers.serverLogger.info(
                        "{} scenario handled {}",
                        sc.getClass().getSimpleName(),
                        packet.getClass().getSimpleName()
                );

                Packet[] packets = sc.handle(packet);
                for (Packet sp : packets) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    sp.write(bos);
                    byte[] data = bos.toByteArray();
                    partiallyWrote.add(ByteBuffer.wrap(data));
                }

                if (sc.isFinished()) {
                    activeScenarios.remove(sc);
                }
            }
        }
    }

    public void writePackets(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        while (!partiallyWrote.isEmpty()) {
            ByteBuffer buf = partiallyWrote.element();
            try {
                socketChannel.write(buf);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (buf.remaining() > 0) {
                break;
            }
            partiallyWrote.remove();
        }
        if (partiallyWrote.isEmpty()) {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    public boolean needWriting() {
        return !partiallyWrote.isEmpty();
    }
}
