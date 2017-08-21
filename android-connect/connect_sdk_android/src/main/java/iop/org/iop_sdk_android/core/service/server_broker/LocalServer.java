package iop.org.iop_sdk_android.core.service.server_broker;

import android.net.LocalServerSocket;
import android.net.LocalSocket;

import com.google.protobuf.ByteString;

import org.libertaria.world.global.utils.SerializationUtils;
import org.libertaria.world.profile_server.CantSendMessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import world.libertaria.shared.library.global.ModuleObject;
import world.libertaria.shared.library.global.service.IntentServiceAction;
import world.libertaria.shared.library.global.socket.LocalSocketSession;
import world.libertaria.shared.library.global.socket.SessionHandler;

/**
 * Created by furszy on 8/19/17.
 */

public class LocalServer {

    private static final Logger logger = LoggerFactory.getLogger(LocalServer.class);

    private LocalServerSocket localServerSocket;
    private PlatformServiceImp platformServiceImp;
    private Thread serverThread;
    /** Authentificated clients */
    private ConcurrentHashMap<String, LocalSocketSession> connectedClients = new ConcurrentHashMap<>();
    /** Pending authentification clients */
    private CopyOnWriteArraySet<LocalSocketSession> pendingClients = new CopyOnWriteArraySet<>();

    private SessionHandler sessionHandler = new SessionHandler() {
        @Override
        public void onReceive(LocalSocketSession localSocketSession,ModuleObject.ModuleResponse response) {
            logger.info("onReceive "+localSocketSession.toString());
            if (!localSocketSession.isAuth()) {
                try {
                    ModuleObject.ModuleObjectWrapper wrapper = response.getObj();
                    String clientPk = wrapper.getObj().toStringUtf8();
                    logger.info("client auth received " + clientPk);
                    // adding client to the connectedClients
                    pendingClients.remove(localSocketSession);
                    localSocketSession.setClientId(clientPk);
                    connectedClients.put(clientPk, localSocketSession);
                }catch (Exception e){
                    e.printStackTrace();
                    logger.info("Client connection is not authentificated, closing it");
                    pendingClients.remove(localSocketSession);
                    localSocketSession.closeNow();
                }
            }else{
                logger.info("Local channel message arrived.. "+localSocketSession.getClientId()+" "+response.toString());
            }
        }

        @Override
        public void sessionClosed(LocalSocketSession localSocketSession,String clientPk) {
            logger.info("sessionClosed "+clientPk);
        }
    };

    public LocalServer(PlatformServiceImp platformServiceImp) {
        this.platformServiceImp = platformServiceImp;
    }

    public void start() throws IOException {

        localServerSocket = new LocalServerSocket(IntentServiceAction.SERVICE_NAME);
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (; ; ) {
                        LocalSocket localSocket = localServerSocket.accept();
                        localSocket.setSendBufferSize(LocalSocketSession.RECEIVE_BUFFER_SIZE);
//                            localSocket.setSoTimeout(0);
                        LocalSocketSession session = new LocalSocketSession(
                                IntentServiceAction.SERVICE_NAME,
                                null, // null because the authentification is not ready yet
                                localSocket,
                                sessionHandler,
                                true
                        );
                        session.startReceiver();
                        pendingClients.add(session);
                        logger.info("app client connected, pending to authentificate");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    logger.error("server socket thread fail");
                }
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void shutdown(){
        try {
            if (localServerSocket != null)
                localServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (serverThread != null && !serverThread.isInterrupted()) {
                serverThread.interrupt();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public boolean isClientConnected(String clientId){
        return connectedClients.containsKey(clientId);
    }

    public void dispathMsg(String clientId, String requestId, int messageId, ModuleObject.ModuleResponse response) throws IOException, CantSendMessageException {
        if (connectedClients.containsKey(clientId)) {
            connectedClients.get(clientId).write(response);
        }else {
            logger.warn("ClientId not found on open local channels.. id: "+clientId+", open channels: "+ Arrays.toString(connectedClients.keySet().toArray()));
        }
    }
}
