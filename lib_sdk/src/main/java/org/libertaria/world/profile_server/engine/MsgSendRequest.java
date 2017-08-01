package org.libertaria.world.profile_server.engine;

/**
 * Created by mati on 02/04/17.
 */

public class MsgSendRequest {

    private int msgId;
    private org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener listener;

    public MsgSendRequest(int msgId) {
        this.msgId = msgId;
    }

    public void send(){

    }


}
