package com.mengcraft.after;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ServerAcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {

	@Override
	public void completed(AsynchronousSocketChannel client, AsynchronousServerSocketChannel server) {
		server.accept(server, this);
		try {
			AsynchronousServerSocketChannel channel = AsynchronousServerSocketChannel.open();
			channel.bind(new InetSocketAddress(0));
			new ClientHandler(client, channel).start();
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
