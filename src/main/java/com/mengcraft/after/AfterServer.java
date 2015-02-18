package com.mengcraft.after;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;

import com.mengcraft.after.handler.ServerAcceptHandler;
import com.mengcraft.after.users.UserManager;

public class AfterServer {

	public final AsynchronousServerSocketChannel server;
	private final int port;
	public final UserManager users;

	public AfterServer(int port, UserManager users) throws IOException {
		this.server = AsynchronousServerSocketChannel.open();
		this.port = port;
		this.users = users;
	}

	public void start() throws IOException {
		InetSocketAddress socket = new InetSocketAddress(this.port);
		AsynchronousServerSocketChannel server = this.getServer();
		server.bind(socket);
		server.accept(this, new ServerAcceptHandler());
	}

	public void close() throws IOException {
		this.getServer().close();
	}

	public AsynchronousServerSocketChannel getServer() {
		return this.server;
	}

	public UserManager getUserManager() {
		return this.users;
	}

}
