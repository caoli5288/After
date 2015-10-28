package com.mengcraft.after.channel;

/**
 * Created on 15-10-27.
 */
public interface ChannelInitializer {

    /**
     * @param ctx The client context.
     */
    void initialize(ChannelContext ctx);

}
