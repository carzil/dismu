package com.dismu.p2p.server;

import com.dismu.logging.Loggers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public abstract class Server {
    public Server(int port) {

    }

    public abstract void start();

    public abstract boolean isRunning();

    public abstract void stop();
}
