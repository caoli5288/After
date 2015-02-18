package com.mengcraft.after;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.CountDownLatch;

import com.mengcraft.after.handler.ServerAcceptHandler;
import com.mengcraft.after.users.DefaultUserManager;
import com.mengcraft.after.users.UserManager;

public class AfterServer {

	private final CountDownLatch latch = new CountDownLatch(1);
	private final AsynchronousServerSocketChannel server;

	private final int port;
	private final UserManager users;

	public AfterServer(int port, UserManager users) throws IOException {
		this.port = port;
		this.server = AsynchronousServerSocketChannel.open();
		this.users = users;
	}

	public AfterServer() throws IOException {
		this(21, new DefaultUserManager());
	}

	public AfterServer start() throws IOException {
		InetSocketAddress socket = new InetSocketAddress(this.port);
		AsynchronousServerSocketChannel server = this.server;
		server.bind(socket);
		server.accept(server, new ServerAcceptHandler(this.users));
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
			new AfterServer(21, new DefaultUserManager()).start().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
