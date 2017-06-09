package org.fermat.redtooth.profile_server.engine.app_services;

import org.fermat.redtooth.profile_server.protocol.IopProfileServer;

/**
 * Created by furszy on 5/22/17.
 */

public class AppServiceMsg {

    private String callTokenId;
    private byte[] msg;


    private AppServiceMsg(String callTokenId, byte[] msg) {
        this.callTokenId = callTokenId;
        this.msg = msg;
    }

    public String getCallTokenId() {
        return callTokenId;
    }

    public byte[] getMsg() {
        return msg;
    }

    public static AppServiceMsg wrap(String sessionTokenId, IopProfileServer.ApplicationServiceReceiveMessageNotificationRequest message) {
        return new AppServiceMsg(sessionTokenId,message.getMessage().toByteArray());
    }
}
