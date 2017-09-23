package org.libertaria.world.communication;

import org.libertaria.world.exceptions.CantStartException;
import org.libertaria.world.profile_server.CantSendMessageException;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 5/9/2017.
 * <p>
 * Interface used to create a communication channel between modules and
 * client apps.
 */
public interface ClientCommunication {

    /**
     * Start the communication channel.
     */
    void start() throws CantStartException;

    /**
     * Shutdowns the communication channel.
     */
    void shutdown();

    /**
     * Check if a client is connected
     *
     * @param clientId {@link String} with the ID of the client to check.
     * @return {@code true} if the client is registered and connected, otherwise, false.
     */
    boolean isConnected(String clientId);

    /**
     * Dispatch a message to the specified client.
     *
     * @param clientId the ID of the receptor of the message.
     * @param message  the message to be sent.
     * @throws CantSendMessageException if there was any error sending the message like unregistered client.
     */
    <T> void dispatchMessage(String clientId, T message) throws CantSendMessageException;
}
