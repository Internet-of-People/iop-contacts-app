package org.fermat.redtooth.profile_server;


import java.io.IOException;
import java.net.Socket;

import org.fermat.redtooth.profile_server.protocol.IopProfileServer;


/**
 * Created by mati on 03/11/16.
 */

public interface IoSession<M> {

    void write(M message) throws Exception;

    IopProfileServer.ServerRoleType getPortType();

    void closeNow() throws IOException;

    boolean isActive();

    boolean isConnected();

    Socket getChannel();

    boolean isReadSuspended();

    boolean isWriteSuspended();


}
