package com.mengcraft.after.channel;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * Created on 15-10-27.
 */
public interface ChannelContext extends Closeable {

    /**
     * @return The logger of this Channel.
     */
    default Logger getLogger() {
        throw new UnsupportedOperationException("Not implemented!");
    }

    default ChannelPipeline getPipeline() {
        throw new UnsupportedOperationException("Not implemented!");
    }

    default void write(ByteBuffer buffer, Runnable runnable) {
        throw new UnsupportedOperationException("Not implemented!");
    }

}
