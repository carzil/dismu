package com.dismu.p2p;

import com.dismu.logging.Loggers;
import com.dismu.music.storages.TrackStorage;
import com.dismu.p2p.client.Client;
import com.dismu.p2p.server.NIOServer;
import com.dismu.p2p.server.Server;
import com.dismu.utils.PlatformUtils;
import com.dismu.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

public class P2PTest {
    public static void main(String[] args) throws IOException {
        if (args[0].equals("server")) {
            TrackStorage storage1 = new TrackStorage(new File("testStorages/storage1/"));
            final Server server = new NIOServer(1337, storage1);
            Utils.runThread(new Runnable() {
                @Override
                public void run() {
                    server.start();
                }
            });
        } else {
            Utils.setPlatformUtils(new PlatformUtils() {
                @Override
                public File getAppFolderPath() {
                    return new File("testStorages/storage2/");
                }
            });
            TrackStorage storage2 = new TrackStorage(new File("testStorages/storage2/"));
            Client client = new Client(InetAddress.getLocalHost(), 1337, "ababab", storage2);
            client.start();
            client.synchronize();
            storage2.close();
        }
    }
}
