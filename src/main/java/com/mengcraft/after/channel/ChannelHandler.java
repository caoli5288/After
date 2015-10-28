package com.mengcraft.after.channel;

import com.mengcraft.after.AfterInterface;

import java.nio.ByteBuffer;

/**
 * Created on 15-10-27.
 */
public interface ChannelHandler extends AfterInterface {

    void failed(Throwable exc, ChannelContext ctx);

    void handle(ByteBuffer in, ChannelContext ctx);

    void decode(ByteBuffer in, ByteBuffer out);

    void encode(ByteBuffer in, ByteBuffer out);

}
