package com.mengcraft.after;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ClientHandler implements CompletionHandler<Integer, Object> {

	private final static TimeUnit SECONDS = TimeUnit.SECONDS;
	private final static File DIR_ANONYMOUS = new File("dir/anonymous");

	private final static Object WRITE_DONE = new Object();
	private final static Object READ_DONE = new Object();
	private final static Object LOGIN_DONE = new Object();
	private final static Object WAIT_USER = new Object();
	private final static Object WAIT_PASS = new Object();
	private final static Object TYPE_IMAGE = new Object();
	private final static Object TYPE_ASCII = new Object();

	private final LineFrameDecoder decoder = new LineFrameDecoder();
	private final AsynchronousSocketChannel client;
	private final ByteBuffer reader = ByteBuffer.allocate(1024);
	private final ByteBuffer writer = ByteBuffer.allocate(1024);
	private final UserManager users = AfterServer.USERS;

	private Object state = WAIT_USER;
	private Object type = TYPE_ASCII;
	private File root = DIR_ANONYMOUS;
	private File dir = DIR_ANONYMOUS;
	private String name = "anonymous";

	public ClientHandler(AsynchronousSocketChannel client) {
		this.client = client;
	}

	@Override
	public void completed(Integer i, Object stats) {
		if (stats != READ_DONE) {
			// DO NOTHING
		} else {
			check();
		}
	}

	@Override
	public void failed(Throwable exc, Object attachment) {
		close();
	}

	private void check() {
		ByteBuffer buffer = this.reader;
		buffer.flip();
		int i = buffer.remaining();
		if (i > 0) {
			byte[] bs = new byte[i];
			buffer.get(bs);
			List<String> list = this.decoder.decode(bs);
			check(list);
		}
		buffer.clear();
		this.client.read(this.reader, 300, SECONDS, READ_DONE, this);
	}

	private void check(List<String> list) {
		int i = list.size();
		if (i > 0) {
			handle(list);
		}
	}

	private void handle(List<String> list) {
		for (String cmd : list) {
			handle(cmd);
		}
	}

	public void motd() {
		write(Response.SERVICE_READY);
	}

	private void handle(String command) {
		String[] cmd = command.split(" ");
		String request = cmd[0];
		switch (request) {
		case "USER":
			user(cmd);
			break;
		case "PASS":
			pass(cmd);
			break;
		case "QUIT":
			write(Response.GOOD_BYE);
			break;
		case "SYST":
			write(Response.SYS_INFO);
			break;
		case "CWD":
			cwd(cmd);
			break;
		case "PWD":
			write("257 \"" + this.dir.getPath() + "\" is current directory\r\n");
			break;
		case "TYPE":
			type(cmd);
			break;
		case "CDUP":
			cdup(cmd);
			break;
		case "MKD":
			mkd(cmd);
			break;
		default:
			write(Response.CMD_NOT_IMPL);
			break;
		}
	}

	private void mkd(String[] cmd) {
		if (this.state != LOGIN_DONE) {
			write(Response.USER_NOT_LOGGED);
		} else if (cmd.length != 2) {
			write(Response.CMD_ERROR);
		} else {
			mkd(cmd[1]);
		}
	}

	private void mkd(String string) {
		File file = new File(this.dir, string);
		if (file.isDirectory()) {
			write(Response.FILE_ACT_ERROR);
		} else if (file.getParentFile().isDirectory()) {
			file.mkdir();
			write("257 \"" + string + "\" created");
		} else {
			write(Response.FILE_ACT_ERROR);
		}
	}

	private void cdup(String[] cmd) {
		if (cmd.length > 1) {
			write(Response.CMD_ARG_NOT_IMPL);
		} else if (this.dir.compareTo(this.root) != 0) {
			this.dir = this.dir.getParentFile();
			write(Response.FILE_ACT_OKEY);
		} else {
			write(Response.FILE_ACT_ERROR);
		}
	}

	private void type(String[] cmd) {
		if (this.state != LOGIN_DONE) {
			write(Response.USER_NOT_LOGGED);
		} else if (cmd.length < 2) {
			write(Response.CMD_ARG_ERROR);
		} else {
			type(cmd[1]);
		}
	}

	private void type(String string) {
		switch (string) {
		case "I":
			this.type = TYPE_IMAGE;
			write(Response.CMD_OKEY);
			break;
		case "A":
			this.type = TYPE_ASCII;
			write(Response.CMD_OKEY);
			break;
		default:
			write(Response.CMD_ARG_NOT_IMPL);
			break;
		}
	}

	private void cwd(String[] cmd) {
		if (this.state != LOGIN_DONE) {
			write(Response.USER_NOT_LOGGED);
		} else if (cmd.length < 2) {
			this.dir = this.root;
			write(Response.FILE_ACT_OKEY);
		} else if (cmd.length < 3) {
			cwd(cmd[1]);
		} else {
			write(Response.CMD_ARG_ERROR);
		}
	}

	private void cwd(String arg) {
		if (arg.startsWith("/")) {
			cwd(new File(this.root, arg));
		} else {
			cwd(new File(this.dir, arg));
		}
	}

	private void cwd(File dir) {
		if (dir.isDirectory()) {
			this.dir = dir;
			write(Response.FILE_ACT_OKEY);
		} else {
			write(Response.FILE_ACT_ERROR);
		}
	}

	private void user(String[] cmd) {
		if (this.state != WAIT_USER) {
			write(Response.CMD_ERROR);
		} else if (cmd.length < 2) {
			write(Response.CMD_ERROR);
		} else if (this.state != WAIT_USER) {
			write(Response.CMD_ERROR);
		} else if (users.has(cmd[1]) < 1) {
			this.state = WAIT_PASS;
			this.name = cmd[1];
			write(Response.USER_OKEY);
		}
	}

	private void pass(String[] cmd) {
		if (cmd.length < 2) {
			write(Response.CMD_ERROR);
		} else if (this.state != WAIT_PASS) {
			write(Response.CMD_ERROR);
		} else if (users.check(this.name, cmd[1]) < 1) {
			String pwd = users.dir(this.name);
			this.state = LOGIN_DONE;
			this.root = new File(pwd);
			this.dir = this.root;
			write(Response.USER_LOGGED);
		} else {
			write(Response.USER_NOT_LOGGED);
		}
	}

	private void write(String code) {
		ByteBuffer buffer = this.writer;
		byte[] bytes = code.getBytes();
		buffer.clear();
		buffer.put(bytes);
		buffer.flip();
		this.client.write(buffer, 8, SECONDS, WRITE_DONE, this);
	}

	private void close() {
		try {
			this.client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
