package org.fermat.redtooth.profile_server.engine.futures;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;

/**
 * Created by mati on 31/03/17.
 */

public class MsgListenerFuture<O> extends BaseMsgFuture<O> implements ProfSerMsgListener<O> {

    public MsgListenerFuture() {
        queue = new ArrayBlockingQueue<O>(1);
    }

    @Override
    public void onMessageReceive(int messageId, O message) {
        this.messageId = messageId;
        queue.offer(message);
    }

    @Override
    public void onMsgFail(int messageId, int statusValue, String details) {
        this.status = statusValue;
        this.statusDetail = details;
        queue.offer(null);
    }
}
