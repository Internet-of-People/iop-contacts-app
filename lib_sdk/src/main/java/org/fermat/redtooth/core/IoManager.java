package org.fermat.redtooth.core;

import org.fermat.redtooth.core.client.IoLooper;
import org.fermat.redtooth.core.client.IoProcessorImp;
import org.fermat.redtooth.core.client.basic.ConnectionId;
import org.fermat.redtooth.core.client.basic.IoSessionImp;
import org.fermat.redtooth.core.client.basic.WriteFutureImp;
import org.fermat.redtooth.core.client.basic.WriteRequestImp;
import org.fermat.redtooth.core.client.exceptions.ConnectionFailureException;
import org.fermat.redtooth.core.client.interfaces.ConnectFuture;
import org.fermat.redtooth.core.client.interfaces.IoHandler;
import org.fermat.redtooth.core.client.interfaces.IoProcessor;
import org.fermat.redtooth.core.client.interfaces.IoSessionConf;
import org.fermat.redtooth.core.client.interfaces.write.WriteFuture;
import org.fermat.redtooth.core.client.interfaces.write.WriteRequest;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mati on 14/05/17.
 */

public class IoManager implements IoProcessor {

    public static AtomicInteger connectorsId = new AtomicInteger(0);
    public static AtomicInteger processorsId = new AtomicInteger(0);

    private Map<Integer,IoLooper> connectors;
    private Map<Integer,IoProcessorImp> ioProcessor;

    private ExecutorService executorService;

    public IoManager(int numConnectors,int numProcessors) throws IOException {
        connectors = new HashMap<>();
        ioProcessor = new HashMap<>();

        executorService = Executors.newFixedThreadPool(2);

        for (int i = 0; i < numProcessors; i++) {
            ioProcessor.put(connectorsId.getAndIncrement(), new IoProcessorImp(SelectorProvider.provider(), executorService));
        }

        for (int i = 0; i < numConnectors; i++) {
            connectors.put(processorsId.getAndIncrement(), new IoLooper(executorService, ioProcessor.get(0)));
        }

    }


    @Override
    public void add(IoSessionImp ioSessionImp) throws Exception {
        ioProcessor.get(0).add(ioSessionImp);
    }

    @Override
    public IoSessionImp getActiveSession(ConnectionId connectionId) {
        for (Map.Entry<Integer, IoProcessorImp> integerIoProcessorImpEntry : ioProcessor.entrySet()) {
            IoSessionImp ioSessionImp = integerIoProcessorImpEntry.getValue().getActiveSession(connectionId);
            if (ioSessionImp!=null){
                return ioSessionImp;
            }
        }
        return null;
    }

    @Override
    public <M> void scheduleForFlush(IoSessionImp session) {
        for (Map.Entry<Integer, IoProcessorImp> integerIoProcessorImpEntry : ioProcessor.entrySet()) {
            IoSessionImp ioSessionImp = integerIoProcessorImpEntry.getValue().getActiveSession(new ConnectionId(session.getId()));
            if (ioSessionImp!=null){
                ioSessionImp.setScheduledForFlush();
            }
        }
    }


    public WriteRequest send(Object msg, ConnectionId connectionId){
        WriteFuture writeFuture = new WriteFutureImp();
        WriteRequest writeRequest = new WriteRequestImp(msg,writeFuture);
        ioProcessor.get(0).getActiveSession(connectionId).addWriteRequest(writeRequest);
        return writeRequest;
    }


    public ConnectFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, IoHandler ioHandler, IoSessionConf ioSessionConf) throws ConnectionFailureException {
        return connectors.get(0).connect(remoteAddress,localAddress,ioHandler,ioSessionConf);
    }
}
