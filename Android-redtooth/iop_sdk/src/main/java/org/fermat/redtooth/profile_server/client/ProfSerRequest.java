package org.fermat.redtooth.profile_server.client;


import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;

/**
 * Created by mati on 02/04/17.
 */
public interface ProfSerRequest {

    int getMessageId();

    void send() throws CantConnectException, CantSendMessageException;

}
