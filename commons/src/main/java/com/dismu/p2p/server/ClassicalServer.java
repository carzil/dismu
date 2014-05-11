package com.dismu.p2p.server;

import com.dismu.logging.Loggers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ClassicalServer extends Server {
    private ServerSocket serverSocket;
    private int port;
    protected boolean isRunning = false;

    public static void main(String[] args) {
        ClassicalServer server = new ClassicalServer(1775);
        try {
            server.start();
        } catch (Exception e) {
            Loggers.serverLogger.error("unhandled exception", e);
        }
    }

    public ClassicalServer(int port) {
        super(port);
        this.port = port;
    }

    public void configureSocket() {
        try {
            this.serverSocket = new ServerSocket(this.port);
            isRunning = true;
        } catch (IOException e) {
            Loggers.serverLogger.error("problem creating ServerSocket instance", e);
        }
    }

    @Override
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
                    new Thread(new ClassicalServerWorker(socket)).start();
                } catch (SocketException e) {
                    Loggers.serverLogger.info("socket exception occurred, all is ok");
                } catch (Exception e) {
                    Loggers.serverLogger.error("unhandled exception occurred while accepting client", e);
                }
            }
        }
    }

    @Override
    public synchronized boolean isRunning() {
       return this.isRunning;
    }

    @Override
    public synchronized void stop() {
        isRunning = false;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            Loggers.serverLogger.error("error closing server", e);
        }
    }
}
