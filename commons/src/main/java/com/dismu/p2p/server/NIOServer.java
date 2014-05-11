package com.dismu.p2p.server;

import com.dismu.logging.Loggers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

public class NIOServer extends Server {
    private int port;
    private InetAddress hostAddress;

    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private boolean isRunning = false;

    public NIOServer(int port) throws IOException {
        super(port);

        try {
            this.hostAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.port = port;
        this.selector = initSelector();
    }

    private Selector initSelector() throws IOException {
        Selector socketSelector = SelectorProvider.provider().openSelector();

        this.serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        InetSocketAddress isa = new InetSocketAddress(this.hostAddress, this.port);
        serverChannel.socket().bind(isa);

        serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
        return socketSelector;
    }

    @Override
    public void start() {
        isRunning = true;

        while (true) {
            try {
                selector.select();

                Iterator it = this.selector.selectedKeys().iterator();

                while (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();
                    it.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        accept(key);
                    } else if (key.isWritable()) {
                        write(key);
                    } else if (key.isReadable()) {
                        read(key);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void write(SelectionKey key) throws IOException{
        ((WorkerObject)key.attachment()).writePackets(key);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        readBuffer.clear();

        int numRead;
        try {
            numRead = socketChannel.read(this.readBuffer);
        } catch (IOException e) {
            e.printStackTrace();
            key.cancel();
            socketChannel.close();
            Loggers.serverLogger.info("Closed client connection");
            return;
        }

        if (numRead == -1) {
            key.channel().close();
            key.cancel();
            Loggers.serverLogger.info("Closed client connection");
            return;
        }

        if (key.attachment() == null) {
            key.attach(new WorkerObject());
        }
        WorkerObject workerObject = (WorkerObject) key.attachment();
        processData(socketChannel, this.readBuffer.array(), numRead, workerObject);
        if (workerObject.needWriting()) {
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void processData(SocketChannel socketChannel, byte[] array, int numRead, WorkerObject attachment) {
        try {
            attachment.parsePackets(array, numRead);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();
        Socket socket = socketChannel.socket();
        socketChannel.configureBlocking(false);

        socketChannel.register(this.selector, SelectionKey.OP_READ);
        Loggers.serverLogger.info("New client");
    }

    @Override
    public synchronized boolean isRunning() {
        return isRunning;
    }

    @Override
    public void stop() {
        isRunning = false;
    }
}
