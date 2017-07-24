package org.libertaria.world.profile_server.engine;

import org.libertaria.world.profile_server.CantSendMessageException;

/**
 * Created by mati on 02/04/17.
 */
public abstract class ProfSerRequestImp implements org.libertaria.world.profile_server.client.ProfSerRequest {

    private int msgId;

    public ProfSerRequestImp(int msgId) {
        this.msgId = msgId;
    }

    @Override
    public int getMessageId() {
        return msgId;
    }

    public abstract void send() throws org.libertaria.world.profile_server.CantConnectException, CantSendMessageException;
}
