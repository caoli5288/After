package com.mengcraft.after;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LineFrameDecoder {

	private final ArrayList<byte[]> array = new ArrayList<>();
	private final ByteBuffer buffer;

	public LineFrameDecoder() {
		this(1024);
	}

	public LineFrameDecoder(int i) {
		this.buffer = ByteBuffer.allocate(i);
	}

	public List<String> decode(byte[] in) {
		for (byte b : in) {
			decode(b);
		}
		ArrayList<String> out = new ArrayList<>();
		Iterator<byte[]> it = this.array.iterator();
		for (; it.hasNext();) {
			byte[] bs = it.next();
			out.add(new String(bs));
			it.remove();
		}
		return out;
	}

	private void decode(byte b) {
		switch (b) {
		case 0x0d:
		case 0x0a:
			decode();
			break;
		default:
			this.buffer.put(b);
			break;
		}
	}

	private void decode() {
		ByteBuffer buffer = this.buffer;
		buffer.flip();
		int i = buffer.remaining();
		if (i > 0) {
			byte[] bs = new byte[i];
			buffer.get(bs);
			this.array.add(bs);
		} 
		buffer.clear();
	}

}
