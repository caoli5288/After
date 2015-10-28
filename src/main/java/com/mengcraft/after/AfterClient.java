package com.mengcraft.after;

import com.mengcraft.after.channel.ChannelContext;
import com.mengcraft.after.channel.ChannelPipeline;
import com.mengcraft.after.util.LocalBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created on 15-10-27.
 */
public class AfterClient implements ChannelContext {

    private final static Logger LOGGER = Logger.getLogger(AfterClient.class.getSimpleName());

    private final AsynchronousSocketChannel channel;

    private final LocalBuffer reader = new LocalBuffer();
    private final LocalBuffer writer = new LocalBuffer();
    private final LocalBuffer buffer = new LocalBuffer();

    private final ChannelPipeline pipeline = new ChannelPipeline(this);

    public AfterClient() throws IOException {
        this(AsynchronousSocketChannel.open());
    }

    public AfterClient(AsynchronousSocketChannel channel) {
        this.channel = channel;
    }

    public void connect(InetSocketAddress remote, Runnable runnable) {
        channel.connect(remote, this, new ConnectedHandler(runnable));
    }

    public void connect(InetSocketAddress remote) {
        channel.connect(remote);
    }

    public void start(boolean asynchronous) throws ExecutionException, InterruptedException {
        if (asynchronous) {
            // TODO
        } else start();
    }

    public void start() throws ExecutionException, InterruptedException {
        ByteBuffer reader = getReader();
        ByteBuffer buffer = getBuffer();

        while (channel.isOpen()) {
            reader.clear(); // Ready to read data from stream.
            channel.read(reader).get(); // Read with blocking.
            reader.flip();  // Ready to decode data.
            pipeline.decode(reader, buffer);    // Decode and handle data.
        }
    }

    private ByteBuffer getReader() {
        return reader.get();
    }

    private ByteBuffer getWriter() {
        return writer.get();
    }

    private ByteBuffer getBuffer() {
        return buffer.get();
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public ChannelPipeline getPipeline() {
        return pipeline;
    }

    @Override
    public void write(ByteBuffer buffer, Runnable runnable) {
        ByteBuffer writer = getWriter();

        writer.clear(); // Clear the buffer.

        pipeline.encode(buffer, writer);

        writer.flip();  // Ready for writer.

        buffer.clear();
        buffer.put(writer);
        buffer.flip();  // Ready for writer.

        channel.write(buffer, this, new WrittenHandler(buffer, runnable));
    }

    static class ConnectedHandler implements CompletionHandler<Void, ChannelContext> {

        private final Runnable runnable;

        public ConnectedHandler(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void completed(Void v, ChannelContext ctx) {
            if (runnable != null) {
                runnable.run();
            }
        }

        @Override
        public void failed(Throwable exc, ChannelContext ctx) {
            ctx.getLogger().log(Level.WARNING, "Exception on connect remote!", exc);
        }

    }

    static class WrittenHandler implements CompletionHandler<Integer, AfterClient> {

        private final ByteBuffer buffer;
        private final Runnable runnable;

        public WrittenHandler(ByteBuffer buffer, Runnable runnable) {
            this.buffer = buffer;
            this.runnable = runnable;
        }

        @Override
        public void completed(Integer result, AfterClient client) {
            if (buffer.hasRemaining()) {
                client.channel.write(buffer, client, this);
            } else if (runnable != null) {
                runnable.run();
            }
        }

        @Override
        public void failed(Throwable exc, AfterClient client) {
            if (client.channel.isOpen()) try {
                client.close();
            } catch (IOException e) {
                client.getLogger().log(Level.WARNING, "Exception on written handler!", exc);
            }
        }

    }

}
