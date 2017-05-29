package org.fermat.redtooth.profile_server.engine.futures;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by mati on 31/03/17.
 */

public class BaseMsgFuture<O> implements Future<O> {

    protected int messageId;
    protected BlockingQueue<O> queue;

    protected int status;
    protected String statusDetail;

    public BaseMsgFuture() {
        queue = new ArrayBlockingQueue<O>(1);
    }

    @Override
    public boolean cancel(boolean b) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return messageId!=0;
    }

    @Override
    public O get() throws InterruptedException, ExecutionException {
        return queue.take();
    }

    @Override
    public O get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        final O replyOrNull = queue.poll(l, timeUnit);
        if (replyOrNull == null) {
            throw new TimeoutException();
        }
        return replyOrNull;
    }

    public int getStatus() {
        return status;
    }

    public String getStatusDetail() {
        return statusDetail;
    }
}
