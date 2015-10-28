package com.mengcraft.after.util;

import java.nio.ByteBuffer;

import static java.nio.ByteBuffer.allocate;

/**
 * Created on 15-10-28.
 */
public class LocalBuffer extends ThreadLocal<ByteBuffer> {

    @Override
    protected ByteBuffer initialValue() {
        return allocate(0xFF);
    }

}
