package com.dismu.p2p.server;

import com.dismu.logging.Loggers;
import com.dismu.music.storages.TrackStorage;

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
    private final TrackStorage storage;

    public NIOServer(int port, TrackStorage storage) throws IOException {
        super(port);

        this.storage = storage;

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
        Loggers.p2pLogger.debug("server started");

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
                Loggers.p2pLogger.error("error in server", e);
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
            Loggers.p2pLogger.error("error while reading", e);
            key.cancel();
            socketChannel.close();
            Loggers.serverLogger.info("closed client connection");
            return;
        }

        if (numRead == -1) {
            key.channel().close();
            key.cancel();
            Loggers.serverLogger.info("closed client connection, numRead == -1");
            return;
        }

        if (key.attachment() == null) {
            key.attach(new WorkerObject(storage));
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
        Loggers.serverLogger.info("new client, socket={}", socket);
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
