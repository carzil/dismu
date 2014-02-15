package com.dismu.p2p.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket ss;
    private int port;
    protected boolean isStopped = false;

    public static void main(String[] args) {
        Server server = new Server();
        try {
            server.port = 1775;
            server.ss = new ServerSocket(server.port);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws IOException {
        while (!isStopped()) {
            try {
                Socket socket = this.ss.accept();
                new Thread(new ServerWorker(socket)).start();
            } catch (IOException e) {
                if (isStopped()) {
                    System.out.println("Server Stopped.");
                    return;
                }
                throw new RuntimeException(
                        "Error accepting client connection", e);
            }
        }
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop() {
        this.isStopped = true;
        try {
            this.ss.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }
}
