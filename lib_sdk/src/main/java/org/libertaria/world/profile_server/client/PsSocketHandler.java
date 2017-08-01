package org.libertaria.world.profile_server.client;

import org.libertaria.world.profile_server.IoSession;
import org.libertaria.world.profile_server.protocol.IopProfileServer;

/**
 * Created by mati on 19/05/17.
 */
public interface PsSocketHandler<T> {

    void messageSent(IoSession session, T message) throws Exception;

    void sessionCreated(IoSession session) throws Exception;

    void sessionOpened(IoSession session) throws Exception;

    void sessionClosed(IoSession session) throws Exception;

    void exceptionCaught(IoSession session, Throwable cause) throws Exception;

    void messageReceived(IoSession session, T message) throws Exception;

    void portStarted(IopProfileServer.ServerRoleType portType);

    void inputClosed(IoSession session) throws Exception;

}
