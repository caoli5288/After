package com.mengcraft.after;

import java.io.IOException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ConnectHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {

	@Override
	public void completed(AsynchronousSocketChannel client, AsynchronousServerSocketChannel server) {
		server.accept(server, this);
		new ClientHandler(client).motd();
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
