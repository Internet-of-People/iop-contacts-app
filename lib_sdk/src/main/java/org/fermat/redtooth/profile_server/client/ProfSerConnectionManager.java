package org.fermat.redtooth.profile_server.client;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.IoHandler;
import org.fermat.redtooth.profile_server.SslContextFactory;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by mati on 07/11/16.
 *
 * Esta clase basicamente se encarga de controlar la conexión con un profile server en especifico.
 * Abriendo,cerrando y/o manteniendo los sockets
 *
 * todo: Add idle channel agent.
 */

public class ProfSerConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(ProfSerConnectionManager.class);

    private SSLContext sslContext;

    private String host;
    /** Regular channel */
    private ConcurrentMap<IopProfileServer.ServerRoleType,ProfileServerSocket> serverSockets;
    /**
     * Channels opened by AppServicesCalls on the app service port (shity implementation of the profile server)
     * The mapping is AppServiceCall token -> channel for this call
     */
    private ConcurrentMap<String,ProfileServerSocket> appServicesSockets = new ConcurrentHashMap<>();

    private PsSocketHandler<IopProfileServer.Message> handler;

    /** seconds */
    private static final long connectionTimeout = 45;

    public ProfSerConnectionManager(String host, SslContextFactory sslContextFactory,PsSocketHandler<IopProfileServer.Message> handler) {
        this.host = host;
        serverSockets = new ConcurrentHashMap<>();
        this.handler = handler;
        initContext(sslContextFactory);
    }

    public void setHandler(PsSocketHandler<IopProfileServer.Message> handler) {
        this.handler = handler;
    }

    private void initContext(SslContextFactory sslContextFactory){
        try {
            if (sslContextFactory == null)
                throw new IllegalArgumentException("ssl context factory null");
            this.sslContext = sslContextFactory.buildContext();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    public boolean connectToSecurePort(final IopProfileServer.ServerRoleType portType, final int port) throws CantConnectException {
        boolean isActive = false;
        if (!serverSockets.containsKey(portType)){
            isActive = syncAddServer(portType,port,null);
        }else {
            isActive = serverSockets.get(portType).isActive();
            if (!isActive){
                // remove references and notify upper layer about it
                serverSockets.remove(portType);
                // todo: Improve this.
                throw new IllegalStateException("Connection not available with port: "+portType);
            }
        }
        return isActive;
    }

    public boolean connectToSecureAppServicePort(final int port,String tokenHex) throws CantConnectException {
        boolean isActive = false;
        if (!appServicesSockets.containsKey(tokenHex)){
            isActive = syncAddServer(IopProfileServer.ServerRoleType.CL_APP_SERVICE,port,tokenHex);
        }else {
            isActive = appServicesSockets.get(tokenHex).isActive();
            if (!isActive){
                // remove references and notify upper layer about it
                appServicesSockets.remove(tokenHex);
                // todo: Improve this.
                throw new AppServiceCallNotAvailableException("Connection not longer available with appService with token: "+tokenHex);
            }
        }
        return isActive;
    }

    public boolean connectToUnSecurePort(IopProfileServer.ServerRoleType portType, int port) throws CantConnectException {
        boolean isActive = false;
        if (!serverSockets.containsKey(portType)) {
            isActive = syncAddServer(portType,port,null);
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
    private synchronized boolean syncAddServer(final IopProfileServer.ServerRoleType portType, final int port, final String tokenIdentifierHex) throws CantConnectException{
        boolean isActive = false;
        try {
            logger.info("syncAddServer "+portType);
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Future<Boolean> future = executorService.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    try {
                        if (portType == IopProfileServer.ServerRoleType.PRIMARY) {
                            addServerSocket(SocketFactory.getDefault(), portType, host, port,null).connect();
                        }else if(portType == IopProfileServer.ServerRoleType.CL_APP_SERVICE){
                            addServerSocket(sslContext.getSocketFactory(),portType,host,port,tokenIdentifierHex).connect();
                        }else
                            addServerSocket(sslContext.getSocketFactory(), portType, host, port,null).connect();
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
                logger.info("connection timeout on port: "+port);
                throw new CantConnectException("Timeout exception, host "+host+":"+port+", type: "+portType,exception);
            }
            executorService.shutdownNow();
        }catch (Exception e){
            e.printStackTrace();
            logger.error("syncAddServer exception",e,portType,port);
        }
        return isActive;
    }

    private ProfileServerSocket addServerSocket(SocketFactory socketFactory, IopProfileServer.ServerRoleType portType, String host, int port,String tokenIdentifierHex) throws Exception {
        ProfileServerSocket profileServerSocket = new ProfileServerSocket(
                socketFactory,
                host,
                port,
                portType,
                tokenIdentifierHex
        );
        profileServerSocket.setHandler(handler);
        if (portType== IopProfileServer.ServerRoleType.CL_APP_SERVICE){
            appServicesSockets.put(
                    tokenIdentifierHex,
                    profileServerSocket
            );
        }else {
            serverSockets.put(
                    portType,
                    profileServerSocket

            );
        }
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
    public void write(IopProfileServer.ServerRoleType portType, int port, IopProfileServer.Message message) throws CantSendMessageException,CantConnectException {
        boolean result = connectToPort(portType,port,null);
        if (!result) throw new CantSendMessageException("Cant connect to: "+portType.name()+", port number: "+port);
        ProfileServerSocket profileServerSocket = serverSockets.get(portType);
        profileServerSocket.write(message);
    }

    public void writeToAppServiceCall(IopProfileServer.ServerRoleType portType, int port, IopProfileServer.Message message,String token) throws CantSendMessageException,CantConnectException {
        if (token==null || token.length()<1) throw new IllegalArgumentException("bad token value");
        boolean result = connectToPort(portType,port,token);
        if (!result) throw new CantSendMessageException("Connection fail");
        ProfileServerSocket profileServerSocket = appServicesSockets.get(token);
        profileServerSocket.write(message);
    }

    private boolean connectToPort(IopProfileServer.ServerRoleType portType, int port,String tokenIdentifier) throws CantConnectException {
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
            case CL_APP_SERVICE:
                isConnected = connectToSecureAppServicePort(port,tokenIdentifier);
                break;
        }
        logger.info("connectToPort "+portType +", port "+port +" is connected: "+isConnected);
        return isConnected;
    }


    public void close(IopProfileServer.ServerRoleType portType) throws IOException {
        this.serverSockets.remove(portType).closeNow();
    }

    public void close(String callToken) throws IOException {
        this.appServicesSockets.remove(callToken).closeNow();
    }

    public void shutdown() throws IOException {
        for (ProfileServerSocket profileServerSocket : this.serverSockets.values()) {
            try {
                profileServerSocket.closeNow();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        for (Map.Entry<String, ProfileServerSocket> stringProfileServerSocketEntry : appServicesSockets.entrySet()) {
            try {
                stringProfileServerSocketEntry.getValue().closeNow();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}
