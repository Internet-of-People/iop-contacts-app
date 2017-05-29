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
        super();
    }

    @Override
    public void onMessageReceive(int messageId, O message) {
        synchronized(reentrantLock) {
            this.messageId = messageId;
            this.status = 200;
            object = message;
            reentrantLock.notifyAll();
        }
        if (listener != null) {
            listener.onAction(messageId,message);
        }
    }

    @Override
    public void onMsgFail(int messageId, int statusValue, String details) {
        synchronized(reentrantLock) {
            this.status = statusValue;
            this.statusDetail = details;
            reentrantLock.notifyAll();
        }
        if (listener != null) {
            listener.onFail(messageId,statusValue,details);
        }
    }

}
