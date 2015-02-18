package com.mengcraft.after.handler;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.mengcraft.after.AfterServer;

public class ServerAcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AfterServer> {

	@Override
	public void completed(AsynchronousSocketChannel client, AfterServer handle) {
		handle.getServer().accept(handle, this);
		try {
			new ClientHandler(handle, client).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void failed(Throwable exc, AfterServer server) {
		try {
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		exc.printStackTrace();
	}

}
