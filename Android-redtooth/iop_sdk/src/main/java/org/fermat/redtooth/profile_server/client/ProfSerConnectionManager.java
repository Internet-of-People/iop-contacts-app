package org.fermat.redtooth.profile_server.client;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import org.fermat.redtooth.IoHandler;
import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.SslContextFactory;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;


/**
 * Created by mati on 07/11/16.
 *
 * Esta clase basicamente se encarga de controlar la conexión con un profile server en especifico.
 * Abriendo,cerrando y/o manteniendo los sockets
 *
 */

public class ProfSerConnectionManager {

    private SSLContext sslContext;

    private String host;

    private Map<IopProfileServer.ServerRoleType,ProfileServerSocket> serverSockets;

    private IoHandler<IopProfileServer.Message> handler;

    /** seconds */
    private static final long connectionTimeout = 45;

    public ProfSerConnectionManager(String host, SslContextFactory sslContextFactory,IoHandler<IopProfileServer.Message> handler) throws Exception {
        this.host = host;
        serverSockets = new HashMap<>();
        this.handler = handler;
        initContext(sslContextFactory);
    }

    public void setHandler(IoHandler<IopProfileServer.Message> handler) {
        this.handler = handler;
    }

    private void initContext(SslContextFactory sslContextFactory) throws Exception {
        if (sslContextFactory==null) throw new IllegalArgumentException("ssl context factory null");
        this.sslContext = sslContextFactory.buildContext();
    }

    public boolean connectToSecurePort(final IopProfileServer.ServerRoleType portType, final int port) throws CantConnectException {
        boolean isActive = false;
        if (!serverSockets.containsKey(portType)){
            isActive = syncAddServer(portType,port);
        }else {
            isActive = serverSockets.get(portType).isActive();
        }
        return isActive;
    }

    public boolean connectToUnSecurePort(IopProfileServer.ServerRoleType portType, int port) throws CantConnectException {
        boolean isActive = false;
        if (!serverSockets.containsKey(portType)) {
            isActive = syncAddServer(portType,port);
        }else {
            isActive = serverSockets.get(portType).isActive();
        }
        return isActive;
    }

    /**
     * Connect to the server with a timeout
     *
     * @param portType
     * @param port
     * @return
     */
    private boolean syncAddServer(final IopProfileServer.ServerRoleType portType, final int port) throws CantConnectException{
        boolean isActive = false;
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<Boolean> future = executorService.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    try {
                        if (portType == IopProfileServer.ServerRoleType.PRIMARY) {
                            addServerSocket(SocketFactory.getDefault(), portType, host, port).connect();
                        }else
                            addServerSocket(sslContext.getSocketFactory(), portType, host, port).connect();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                    return true;
                }
            });
            try {
                isActive = future.get(connectionTimeout, TimeUnit.SECONDS);
            }catch (TimeoutException exception){
                throw new CantConnectException("Timeout exception, host "+host+":"+port+", type: "+portType,exception);
            }
            executorService.shutdownNow();
        }catch (Exception e){
            e.printStackTrace();
        }
        return isActive;
    }

    private ProfileServerSocket addServerSocket(SocketFactory socketFactory, IopProfileServer.ServerRoleType portType, String host, int port) throws Exception {
        ProfileServerSocket profileServerSocket = new ProfileServerSocket(
                socketFactory,
                host,
                port,
                portType
        );
        profileServerSocket.setHandler(handler);
        serverSockets.put(
                portType,
                profileServerSocket

        );
        return profileServerSocket;
    }

    /**
     *  Send a message
     *
     * @param portType
     * @param port
     * @param message
     * @throws Exception
     */
    public void write(IopProfileServer.ServerRoleType portType, int port, IopProfileServer.Message message) throws CantConnectException,CantSendMessageException {

        try {
            boolean result = connectToPort(portType,port);
            if (!result) throw new Exception("Something happen with the connection");
            ProfileServerSocket profileServerSocket = serverSockets.get(portType);
            profileServerSocket.write(message);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private boolean connectToPort(IopProfileServer.ServerRoleType portType, int port) throws CantConnectException {
        boolean isConnected = false;
        switch (portType){
            case CL_CUSTOMER:
                isConnected = connectToSecurePort(portType,port);
                break;
            case CL_NON_CUSTOMER:
                isConnected = connectToSecurePort(portType,port);
                break;
            case PRIMARY:
                isConnected = connectToUnSecurePort(portType,port);
                break;
        }
        return isConnected;
    }


    public void close(IopProfileServer.ServerRoleType portType) throws IOException {
        this.serverSockets.remove(portType).closeNow();
    }
}
