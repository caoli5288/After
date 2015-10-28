package com.mengcraft.after.channel;

import com.mengcraft.after.util.LocalBuffer;

import java.nio.ByteBuffer;

import static java.nio.ByteBuffer.allocate;

/**
 * Created on 15-10-28.
 */
public class ChannelPipeline {

    private final LocalBuffer buffer;
    private final ChannelContext context;

    private Node head;
    private Node tail;

    public ChannelPipeline(ChannelContext context) {
        this.context = context;
        this.buffer = new LocalBuffer();
    }

    public synchronized ChannelPipeline addHead(ChannelHandler handler) {
        setHead(new Node(handler).setNext(head));
        if (getTail() == null) {
            setTail(getHead());
        }
        return this;
    }

    public synchronized ChannelPipeline addTail(ChannelHandler handler) {
        setTail(new Node(handler).setLast(tail));
        if (getHead() == null) {
            setHead(getTail());
        }
        return this;
    }

    public Node getHead() {
        return head;
    }

    public Node getTail() {
        return tail;
    }

    public ByteBuffer getBuffer() {
        return buffer.get();
    }

    private void setHead(Node head) {
        this.head = head;
    }

    private void setTail(Node tail) {
        this.tail = tail;
    }

    public ByteBuffer encode(ByteBuffer in, ByteBuffer output) {
        ByteBuffer buffer = getBuffer();

        buffer.clear(); // Ready for write data.
        buffer.put(in);
        buffer.flip();  // Ready for read data.

        output.clear();

        for (Node node = tail; node != null; node = node.last) {
            node.handler.encode(buffer, output);

            output.flip();
            buffer.clear();
            buffer.put(output);
            buffer.flip();
            output.clear();
        }

        return output.put(buffer);
    }

    public ByteBuffer decode(ByteBuffer in, ByteBuffer output) {
        ByteBuffer buffer = getBuffer();

        buffer.clear();
        buffer.put(in);
        buffer.flip();  // Ready for read data.

        output.clear();

        for (Node node = head; node != null; node = node.next) {
            node.handler.decode(buffer, output);

            output.flip();
            buffer.clear();
            buffer.put(output);
            buffer.flip();
            output.clear();

            node.handler.handle(buffer.asReadOnlyBuffer(), context);
        }

        return output.put(buffer);
    }

    private static class Node {

        private final ChannelHandler handler;

        private Node last;
        private Node next;

        public Node(ChannelHandler handler) {
            this.handler = handler;
        }

        public ChannelHandler getHandler() {
            return handler;
        }

        public Node getLast() {
            return last;
        }

        public Node setLast(Node last) {
            synchronized (this) {
                this.last = last;
            }
            return this;
        }

        public Node getNext() {
            return next;
        }

        public Node setNext(Node next) {
            synchronized (this) {
                this.next = next;
            }
            return this;
        }

    }

}
