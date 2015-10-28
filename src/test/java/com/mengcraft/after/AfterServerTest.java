package com.mengcraft.after;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Created on 15-10-27.
 */
public class AfterServerTest {

    @Test
    public void test() throws IOException, ExecutionException, InterruptedException {
        AfterServer server = new AfterServer();
        server.setSubChannelInitializer(ctx -> {
            try {
                ctx.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        server.bind(12329);
        server.start(true);
    }

}
