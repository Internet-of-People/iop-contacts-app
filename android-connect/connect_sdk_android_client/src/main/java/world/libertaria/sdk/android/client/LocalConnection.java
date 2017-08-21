package world.libertaria.sdk.android.client;

import android.net.LocalSocket;

import com.google.protobuf.ByteString;

import org.libertaria.world.profile_server.CantSendMessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import world.libertaria.shared.library.global.ModuleObject;
import world.libertaria.shared.library.global.service.IntentServiceAction;
import world.libertaria.shared.library.global.socket.LocalSocketSession;
import world.libertaria.shared.library.global.socket.SessionHandler;

/**
 * Created by furszy on 8/19/17.
 */

public class LocalConnection {

    private Logger logger = LoggerFactory.getLogger(LocalConnection.class);

    private LocalSocketSession serviceSocket;
    private String clientId;
    private SessionHandler sessionHandler;

    public LocalConnection(String clientId,SessionHandler sessionHandler) {
        this.clientId = clientId;
        this.sessionHandler = sessionHandler;
    }

    public void start() throws IOException, CantSendMessageException {
        logger.info("Starting socket receiver, client id: "+clientId);
        LocalSocket localSocket = new LocalSocket();
        serviceSocket = new LocalSocketSession(IntentServiceAction.SERVICE_NAME,clientId, localSocket, sessionHandler);
        logger.info("connecting local socket");
        serviceSocket.connect();
        // sending auth message
        logger.info("sending auth message");
        ModuleObject.ModuleObjectWrapper moduleObjectWrapper = ModuleObject.ModuleObjectWrapper.newBuilder()
                .setObj(ByteString.copyFromUtf8(clientId))
                .build();
        ModuleObject.ModuleResponse moduleResponse = ModuleObject.ModuleResponse.newBuilder()
                .setObj(moduleObjectWrapper)
                .build();
        serviceSocket.write(moduleResponse);
        logger.info("auth message sent");
    }

    public void shutdown(){
        try{
            serviceSocket.closeNow();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public String getClientId() {
        return clientId;
    }
}
