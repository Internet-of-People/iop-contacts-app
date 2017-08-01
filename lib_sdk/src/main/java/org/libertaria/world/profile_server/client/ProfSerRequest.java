package org.libertaria.world.profile_server.client;


import org.libertaria.world.profile_server.CantSendMessageException;

/**
 * Created by mati on 02/04/17.
 */
public interface ProfSerRequest {

    int getMessageId();

    void send() throws org.libertaria.world.profile_server.CantConnectException, CantSendMessageException;

}
