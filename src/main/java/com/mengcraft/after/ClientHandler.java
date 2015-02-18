package com.mengcraft.after;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ClientHandler implements CompletionHandler<Integer, Object> {

	private final static TimeUnit SECONDS = TimeUnit.SECONDS;
	private final static File DIR_ANONYMOUS = new File("dir/anonymous");

	private final static Object DEFAULT = new Object();

	private final static Object WRITE_DONE = new Object();
	private final static Object READ_DONE = new Object();

	private final static Object LOGIN_DONE = new Object();
	private final static Object WAIT_PASS = new Object();

	// private final static Object TYPE_IMAGE = new Object();
	// private final static Object TYPE_ASCII = new Object();

	private final static Object MODE_PASV = new Object();

	private final LineFrameDecoder decoder = new LineFrameDecoder();
	private final AsynchronousSocketChannel client;
	private final AsynchronousServerSocketChannel channel;
	private final ByteBuffer reader = ByteBuffer.allocate(1024);
	private final ByteBuffer writer = ByteBuffer.allocate(1024);
	private final UserManager users = AfterServer.USERS;

	private Object state = DEFAULT;
	// private Object type = TYPE_ASCII;
	private Object mode = DEFAULT;
	private File root = DIR_ANONYMOUS;
	private File dir = DIR_ANONYMOUS;
	private String name = "anonymous";

	public ClientHandler(AsynchronousSocketChannel client, AsynchronousServerSocketChannel data) {
		this.client = client;
		this.channel = data;
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
		exc.printStackTrace();
		close();
	}

	public void start() {
		read();
		write(Response.SERVICE_READY);
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
			read();
		} else {
			close();
		}
	}

	private void read() {
		if (this.client.isOpen()) {
			this.reader.clear();
			this.client.read(this.reader, 300, SECONDS, READ_DONE, this);
		}
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

	/*
	 * USER, QUIT, PORT, TYPE, MODE, STRU, RETR, STOR, NOOP
	 */
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
			close();
			break;
		case "SYST":
			write(Response.SYS_INFO);
			break;
		case "CWD":
			cwd(cmd);
			break;
		case "PWD":
			pwd(cmd);
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
		case "RMD":
			rmd(cmd);
			break;
		case "DELE":
			dele(cmd);
			break;
		case "PASV":
			pasv(cmd);
			break;
		case "NOOP":
			write(Response.CMD_OKEY);
			break;
		case "LIST":
			list(cmd);
			break;
		case "STOR":
			stor(cmd);
			break;
		case "RETR":
			retr(cmd);
			break;
		default:
			write(Response.CMD_NOT_IMPL);
			break;
		}
	}

	private void retr(String[] cmd) {
		if (this.state != LOGIN_DONE) {
			write(Response.USER_NOT_LOGGED);
		} else if (this.mode != MODE_PASV) {
			write(Response.CMD_BAD_SEQUENCE);
		} else if (cmd.length != 2) {
			write(Response.CMD_ARG_ERROR);
		} else {
			retr(getRealFile(cmd[1]));
		}
	}

	private void retr(File file) {
		if (file.isFile()) {
			write(Response.FILE_STATUS_OKEY);
			this.channel.accept(this, new DataAcceptHandler(file, DataHandler.ACT_PUSH));
		} else {
			write(Response.FILE_ACT_NOT_TAKEN);
		}
	}

	private void stor(String[] cmd) {
		if (this.state != LOGIN_DONE) {
			write(Response.USER_NOT_LOGGED);
		} else if (this.mode != MODE_PASV) {
			write(Response.CMD_BAD_SEQUENCE);
		} else if (cmd.length != 2) {
			write(Response.CMD_ARG_ERROR);
		} else {
			write(Response.FILE_STATUS_OKEY);
			File file = getRealFile(cmd[1]);
			this.channel.accept(this, new DataAcceptHandler(file, DataHandler.ACT_TAKE));
		}
	}

	private void list(String[] cmd) {
		if (this.mode != MODE_PASV) {
			write(Response.CMD_BAD_SEQUENCE);
		} else if (cmd.length < 2) {
			list(this.dir);
		} else if (cmd[1].equals("/")) {
			list(this.root);
		} else {
			File file = getRealFile(cmd[1]);
			list(file);
		}
	}

	private void list(File file) {
		write(Response.FILE_STATUS_OKEY);
		try {
			AsynchronousSocketChannel socket = this.channel.accept().get(60, SECONDS);
			new DataHandler(this, socket, file, DataHandler.ACT_LIST).start();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("INSERT #1");
			write(Response.FILE_ACT_NOT_TAKEN);
		}
	}

	private void pasv(String[] cmd) {
		if (this.state != LOGIN_DONE) {
			write(Response.USER_NOT_LOGGED);
		} else {
			pasv();
		}
	}

	private void pasv() {
		try {
			String string = this.client.getLocalAddress().toString().replace('.', ',');
			int i = string.indexOf(':');
			String host = string.substring(1, i);
			String data = this.channel.getLocalAddress().toString();
			int j = data.lastIndexOf(':');
			String port = data.substring(j + 1, data.length());
			pasv(host, port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void pasv(String host, String string) {
		int port = Integer.parseInt(string);
		this.mode = MODE_PASV;
		write("227 Entering Passive Mode (" + host + ',' + port / 256 + ',' + port % 256 + ")\r\n");
	}

	private void dele(String[] cmd) {
		if (this.state != LOGIN_DONE) {
			write(Response.USER_NOT_LOGGED);
		} else if (cmd.length != 2) {
			write(Response.CMD_ARG_ERROR);
		} else {
			dele(cmd[1]);
		}
	}

	private void dele(String string) {
		if (string.equals("/")) {
			write(Response.FILE_ACT_ERROR);
		} else {
			dele(getRealFile(string));
		}
	}

	private void dele(File file) {
		if (file.isFile()) {
			file.delete();
			write(Response.FILE_ACT_OKEY);
		} else {
			write(Response.FILE_ACT_ERROR);
		}
	}

	private void rmd(String[] cmd) {
		if (this.state != LOGIN_DONE) {
			write(Response.USER_NOT_LOGGED);
		} else if (cmd.length != 2) {
			write(Response.CMD_ARG_ERROR);
		} else {
			rmd(cmd[1]);
		}
	}

	private void rmd(String arg) {
		if (arg.equals("/")) {
			write(Response.FILE_ACT_NOT_TAKEN);
		} else {
			rmd(getRealFile(arg));
		}
	}

	private void rmd(File dir) {
		if (dir.isDirectory() && dir.list().length < 1) {
			dir.delete();
			write(Response.FILE_ACT_OKEY);
		} else if (dir.isDirectory()) {
			write(Response.FILE_ACT_NOT_TAKEN);
		} else {
			write(Response.FILE_ACT_ERROR);
		}
	}

	private void pwd(String[] cmd) {
		if (this.state != LOGIN_DONE) {
			write(Response.USER_NOT_LOGGED);
		} else if (this.dir.compareTo(this.root) != 0) {
			String current = this.dir.getPath();
			write("257 \"" + current.substring(this.root.getPath().length(), current.length()) + "\" is current directory\r\n");
		} else {
			write("257 \"/\" is current directory\r\n");
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

	private void mkd(String arg) {
		if (arg.equals("/")) {
			write(Response.FILE_ACT_ERROR);
		} else {
			mkd(getRealFile(arg), arg);
		}
	}

	private void mkd(File directory, String arg) {
		if (directory.isDirectory()) {
			write(Response.FILE_ACT_ERROR);
		} else if (directory.getParentFile().isDirectory()) {
			directory.mkdir();
			write("257 \"" + arg + "\" created\r\n");
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
			// this.type = TYPE_IMAGE;
			write(Response.CMD_OKEY);
			break;
		case "A":
			// this.type = TYPE_ASCII;
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
		if (arg.equals("/")) {
			this.dir = this.root;
			write(Response.FILE_ACT_OKEY);
		} else {
			cwd(getRealFile(arg));
		}
	}

	private File getRealFile(String arg) {
		if (arg.startsWith("/")) {
			return new File(this.root, arg);
		}
		return new File(this.dir, arg);
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
		if (this.state != DEFAULT) {
			write(Response.CMD_ERROR);
		} else if (cmd.length < 2) {
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
		} else if (this.users.check(this.name, cmd[1]) < 1) {
			String pwd = this.users.dir(this.name);
			this.state = LOGIN_DONE;
			this.root = new File(pwd);
			this.dir = this.root;
			this.dir.mkdir();
			write(Response.USER_LOGGED);
		} else {
			write(Response.USER_NOT_LOGGED);
		}
	}

	public void write(String code) {
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
			this.channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
