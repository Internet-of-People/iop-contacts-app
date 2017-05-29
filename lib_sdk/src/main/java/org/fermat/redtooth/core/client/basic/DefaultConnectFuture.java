package org.fermat.redtooth.core.client.basic;

import org.fermat.redtooth.core.client.interfaces.ConnectFuture;
import org.fermat.redtooth.core.client.interfaces.IoSession;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by mati on 12/05/17.
 */

public class DefaultConnectFuture implements ConnectFuture {

    /** Connection identifier on the client */
    private ConnectionId connectionId;
    /** Exception occured */
    private Throwable exception;
    /** Lock object */
    private ReentrantLock lock = new ReentrantLock();

    @Override
    public ConnectionId getConnectionId() {
        return connectionId;
    }

    @Override
    public Throwable getException() {
        return exception;
    }

    @Override
    public boolean isConnected() {
        return connectionId!=null;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public synchronized void setConnectionId(ConnectionId connectionId) {
        this.connectionId = connectionId;
        notifyAll();
    }

    @Override
    public synchronized void setException(Throwable exception) {
        this.exception = exception;
        notifyAll();
    }

    @Override
    public boolean cancel() {
        return false;
    }

    @Override
    public synchronized ConnectFuture get(long timeout) throws InterruptedException {
        if (connectionId==null && exception==null){
            wait(timeout);
        }
        return this;
    }

}
