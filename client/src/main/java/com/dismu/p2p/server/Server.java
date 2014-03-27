package com.dismu.p2p.server;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

import com.dismu.logging.Loggers;

public class Server {
    private ServerSocket serverSocket;
    private int port;
    protected boolean isStopped = false;

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
            while (!isStopped()) {
                try {
                    Socket socket = this.serverSocket.accept();
                    Loggers.serverLogger.info("new client accepted");
                    new Thread(new ServerWorker(socket)).start();
                } catch (Exception e) {
                    Loggers.serverLogger.error("unhandled exception occurred, while accepting client", e);
                }
            }
        }
    }

    private synchronized boolean isStopped() {
       return this.isStopped;
    }

    public synchronized void stop() {
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }
}
