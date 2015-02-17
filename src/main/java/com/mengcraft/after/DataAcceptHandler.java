package com.mengcraft.after;

import java.io.File;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class DataAcceptHandler implements CompletionHandler<AsynchronousSocketChannel, ClientHandler> {

	private File file;
	private int act;

	public DataAcceptHandler(File file, int act) {
		this.file = file;
		this.act = act;
	}

	@Override
	public void completed(AsynchronousSocketChannel result, ClientHandler attachment) {
		new DataHandler(attachment, result, this.file, this.act).start();
	}

	@Override
	public void failed(Throwable exc, ClientHandler attachment) {
		exc.printStackTrace();
	}

}
