package com.mengcraft.after;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.CountDownLatch;

public class AfterServer {

	public final static UserManager USERS = new UserManager();
	private final CountDownLatch latch = new CountDownLatch(1);
	private final AsynchronousServerSocketChannel server;

	private int port;

	public AfterServer(int port) throws IOException {
		this.port = port;
		this.server = AsynchronousServerSocketChannel.open();
	}

	public AfterServer() throws IOException {
		this(21);
	}

	public AfterServer start() throws IOException {
		InetSocketAddress socket = new InetSocketAddress(this.port);
		AsynchronousServerSocketChannel server = this.server;
		server.bind(socket);
		server.accept(server, new ServerAcceptHandler());
		return this;
	}

	public void sync() throws InterruptedException {
		this.latch.await();
	}

	public void close() throws IOException {
		this.server.close();
		this.latch.countDown();
	}

	public static void main(String[] args) {
		try {
			new AfterServer(21).start().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
