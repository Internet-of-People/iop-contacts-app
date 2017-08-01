package org.libertaria.world.profile_server;


import org.libertaria.world.profile_server.protocol.IopProfileServer;

import java.io.IOException;
import java.net.Socket;


/**
 * Created by mati on 03/11/16.
 */

public interface IoSession<M> {

    String getSessionTokenId();

    void write(M message) throws Exception;

    IopProfileServer.ServerRoleType getPortType();

    void closeNow() throws IOException;

    boolean isActive();

    boolean isConnected();

    Socket getChannel();

    boolean isReadSuspended();

    boolean isWriteSuspended();


}
