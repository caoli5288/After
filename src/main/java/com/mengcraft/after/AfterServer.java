package com.mengcraft.after;

import com.mengcraft.after.channel.ChannelInitializer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AfterServer implements AfterInterface {

    private final static Logger LOGGER = Logger.getLogger(AfterServer.class.getSimpleName());
    private final AsynchronousServerSocketChannel channel;

    private ChannelInitializer initializer;

    public AfterServer() throws IOException {
        this.channel = AsynchronousServerSocketChannel.open();
    }

    public AsynchronousServerSocketChannel bind(int port) throws IOException {
        return bind(new InetSocketAddress(port));
    }

    public AsynchronousServerSocketChannel bind(SocketAddress socket) throws IOException {
        return channel.bind(socket);
    }

    public void start(boolean asynchronous) throws ExecutionException, InterruptedException, IOException {
        if (asynchronous) {
            channel.accept(this, AcceptLoopHandler.INSTANCE);
        } else start();
    }

    public void start() throws ExecutionException, InterruptedException, IOException {
        while (channel.isOpen()) {
            initializer.initialize(new AfterClient(channel.accept().get()));
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    public AfterServer setSubChannelInitializer(ChannelInitializer initializer) {
        synchronized (this) {
            this.initializer = initializer;
        }
        return this;
    }

    static class AcceptLoopHandler implements CompletionHandler<AsynchronousSocketChannel, AfterServer> {

        public static final AcceptLoopHandler INSTANCE = new AcceptLoopHandler();

        @Override
        public void completed(AsynchronousSocketChannel client, AfterServer server) {
            if (server.channel.isOpen()) {
                server.channel.accept(server, INSTANCE);
            }
            server.initializer.initialize(new AfterClient(client));
        }

        @Override
        public void failed(Throwable cause, AfterServer server) {
            server.getLogger().log(Level.WARNING, "Exception on accept connection!", cause);
        }

    }

}
