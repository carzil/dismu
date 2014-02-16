package com.dismu.p2p.server;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

import com.dismu.p2p.utilities.Logging;
import com.dismu.p2p.server.ServerWorker;

public class Server {
    private ServerSocket serverSocket;
    private int port;
    protected boolean isStopped = false;

    public static void main(String[] args) {
        Server server = new Server(1775);
        try {
            server.start();
        } catch (Exception e) {
            Logging.serverLogger.error("unhandled exception", e);
        }
    }

    public Server(int port) {
        this.port = port;
    }

    public void configureSocket() throws IOException {
        this.serverSocket = new ServerSocket(this.port);
    }

    public void start() throws IOException {
        configureSocket();
        Logging.serverLogger.info("server started");
        while (!isStopped()) {
            try {
                Socket socket = this.serverSocket.accept();
                Logging.serverLogger.info("new client accepted");
                new Thread(new ServerWorker(socket)).start();
            } catch (Exception e) {
                Logging.serverLogger.error("unhandled exception occurred, while accepting client", e);
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
