package com.mengcraft.after;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;

public class DataHandler implements CompletionHandler<Integer, Integer> {

	public final static int ACT_LIST = 0;
	public final static int ACT_TAKE = 1;
	public final static int ACT_PUSH = 2;

	public final static int PUSH_READ_DONE = 0;
	public final static int PUSH_WRITE_DONE = 1;
	public final static int TAKE_READ_DONE = 2;
	public final static int TAKE_WRITE_DONE = 3;
	public final static int LIST_WRITE_DONE = 4;

	private final ClientHandler handle;
	private final AsynchronousSocketChannel socket;
	private final File file;
	private final int act;

	private final ByteBuffer buffer = ByteBuffer.allocate(8192);

	private AsynchronousFileChannel channel;
	private long position;
	private Process process;

	public DataHandler(ClientHandler client, AsynchronousSocketChannel socket, File file, int act) {
		this.handle = client;
		this.socket = socket;
		this.file = file;
		this.act = act;
	}

	public void start() {
		switch (this.act) {
		case ACT_LIST:
			list();
			break;
		case ACT_TAKE:
			take();
			break;
		case ACT_PUSH:
			push();
			break;
		}
	}

	private void push() {
		if (this.channel != null) {
			// DO NOTHING
		} else {
			channel(StandardOpenOption.READ);
		}
		this.buffer.clear();
		this.channel.read(this.buffer, this.position, PUSH_READ_DONE, this);
	}

	private void take() {
		if (this.channel != null) {
			// DO NOTHING
		} else {
			channel(new OpenOption[] { StandardOpenOption.WRITE, StandardOpenOption.CREATE });
		}
		this.buffer.clear();
		this.socket.read(this.buffer, TAKE_READ_DONE, this);
	}

	private void channel(OpenOption... option) {
		try {
			this.channel = AsynchronousFileChannel.open(this.file.toPath(), option);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void list() {
		if (this.process != null) {
			// DO NOTHING
		} else {
			process();
		}
		byte[] bs = new byte[8192];
		InputStream in = this.process.getInputStream();
		try {
			int i = in.read(bs);
			if (i < 0) {
				done();
			}
			else {
				this.buffer.clear();
				this.buffer.put(bs, 0, i);
				this.buffer.flip();
				this.socket.write(this.buffer, LIST_WRITE_DONE, this);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void process() {
		String[] commands = new String[] { "ls", "-l", this.file.getPath() };
		ProcessBuilder build = new ProcessBuilder(commands);
		try {
			this.process = build.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void done() {
		this.handle.write(Response.FILE_STATUS_DONE);
		try {
			close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void close() throws IOException {
		this.socket.close();
		if (this.channel != null) {
			this.channel.close();
		}
	}

	@Override
	public void completed(Integer result, Integer attachment) {
		switch (attachment) {
		case PUSH_READ_DONE:
			push(result);
			break;
		case PUSH_WRITE_DONE:
			push();
			break;
		case TAKE_READ_DONE:
			take(result);
			break;
		case TAKE_WRITE_DONE:
			take();
			break;
		case LIST_WRITE_DONE:
			list();
			break;
		}
	}

	private void take(int result) {
		if (result < 0) {
			done();
		} else {
			long position = this.position;
			this.buffer.flip();
			this.position += this.buffer.remaining();
			this.channel.write(this.buffer, position, TAKE_WRITE_DONE, this);
		}
	}

	private void push(int result) {
		if (result < 0) {
			done();
		} else {
			this.buffer.flip();
			this.position += this.buffer.remaining();
			this.socket.write(this.buffer, PUSH_WRITE_DONE, this);
		}
	}

	@Override
	public void failed(Throwable exc, Integer attachment) {
		this.handle.write(Response.FILE_ACT_NOT_TAKEN);
		try {
			close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		exc.printStackTrace();
	}

}
