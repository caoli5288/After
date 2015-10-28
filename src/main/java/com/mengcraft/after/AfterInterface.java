package com.mengcraft.after;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannel;
import java.util.logging.Logger;

/**
 * Created on 15-10-27.
 */
public interface AfterInterface extends Closeable {

    Logger getLogger();

}
