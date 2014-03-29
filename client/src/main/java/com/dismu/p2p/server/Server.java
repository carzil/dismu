package com.dismu.p2p.server;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;

import com.dismu.logging.Loggers;

public class Server {
    private ServerSocket serverSocket;
    private int port;
    protected boolean isRunning = false;

    public static void main(String[] args) {
        Server server = new Server(1775);
        try {
            server.start();
        } catch (Exception e) {
            Loggers.serverLogger.error("unhandled exception", e);
        }
    }

    public Server(int port) {
        this.port = port;
    }

    public void configureSocket() {
        try {
            this.serverSocket = new ServerSocket(this.port);
        } catch (IOException e) {
            Loggers.serverLogger.error("problem creating ServerSocket instance", e);
        }
    }

    public void start() {
        configureSocket();
        if (serverSocket == null) {
            Loggers.serverLogger.info("failed to start sever");
        } else {
            Loggers.serverLogger.info("server started");
            while (isRunning) {
                try {
                    Socket socket = serverSocket.accept();
                    Loggers.serverLogger.info("new client accepted");
                    new Thread(new ServerWorker(socket)).start();
                } catch (SocketException e) {
                    Loggers.serverLogger.info("socket exception occurred, all is ok");
                } catch (Exception e) {
                    Loggers.serverLogger.error("unhandled exception occurred while accepting client", e);
                }
            }
        }
    }

    private synchronized boolean isRunning() {
       return this.isRunning;
    }

    public synchronized void stop() {
        isRunning = false;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            Loggers.serverLogger.error("error closing server", e);
        }
    }
}
