package com.mengcraft.after.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.mengcraft.after.users.UserManager;

public class ServerAcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {
	
	private final UserManager users;

	public ServerAcceptHandler(UserManager users) {
		this.users = users;
	}

	@Override
	public void completed(AsynchronousSocketChannel client, AsynchronousServerSocketChannel server) {
		server.accept(server, this);
		try {
			AsynchronousServerSocketChannel channel = AsynchronousServerSocketChannel.open();
			channel.bind(new InetSocketAddress(0));
			new ClientHandler(client, channel, this.users).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void failed(Throwable exc, AsynchronousServerSocketChannel server) {
		try {
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
