package org.fermat.redtooth.profile_server.engine;

import org.fermat.redtooth.profile_server.engine.futures.BaseMsgFuture;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;

/**
 * Created by mati on 02/04/17.
 */

public class MsgSendRequest {

    private int msgId;
    private ProfSerMsgListener listener;

    public MsgSendRequest(int msgId) {
        this.msgId = msgId;
    }

    public void send(){

    }


}
