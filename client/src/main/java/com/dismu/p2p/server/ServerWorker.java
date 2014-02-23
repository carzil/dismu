package com.dismu.p2p.server;

import com.dismu.p2p.packets.Packet;
import com.dismu.p2p.packets.node_control.ExitPacket;
import com.dismu.p2p.packets.node_control.RequestSeedsPacket;
import com.dismu.p2p.packets.transaction.StartTransactionPacket;
import com.dismu.p2p.scenarios.RespondFileScenario;
import com.dismu.p2p.scenarios.Scenario;
import com.dismu.p2p.scenarios.SendSeedListScenario;
import com.dismu.p2p.utils.Loggers;
import com.dismu.p2p.utils.PacketSerialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

public class ServerWorker implements Runnable {
    private final Socket clientSocket;

    public ServerWorker(Socket s) {
        this.clientSocket = s;
    }

    @Override
    public void run() {
        Packet packet;
        try {
            OutputStream outputStream = clientSocket.getOutputStream();
            InputStream inputStream = clientSocket.getInputStream();

            LinkedList<Scenario> activeScenarios
                    = new LinkedList<Scenario>();

            while (true) {
                try {
                    packet = PacketSerialize.readPacket(inputStream);
                } catch (SocketException e) {
                    Loggers.serverLogger.info("client disconnected");
                    break;
                }
                if (packet instanceof ExitPacket) {
                    Loggers.serverLogger.info("received ExitPacket. terminating connection.");
                    break;
                }
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
                    }
                    if (activated) {
                        Loggers.serverLogger.info("activated {} scenario", sc.getClass().getName());
                    }
                }
                if (null == sc) {
                    Loggers.serverLogger.warn("got packet for unknown scenario");
                } else {
                    Loggers.serverLogger.info(
                            "{} scenario handled {}",
                            sc.getClass().getName(),
                            packet.getClass().getName()
                    );

                    Packet[] packets = sc.handle(packet);
                    for (Packet sp : packets) {
                        sp.write(outputStream);
                    }

                    if (sc.isFinished()) {
                        activeScenarios.remove(sc);
                    }
                }
            }


            outputStream.close();
            inputStream.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
