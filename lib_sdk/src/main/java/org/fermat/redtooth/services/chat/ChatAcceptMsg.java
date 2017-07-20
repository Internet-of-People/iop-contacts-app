package org.fermat.redtooth.services.chat;

import org.fermat.redtooth.global.utils.SerializationUtils;
import org.fermat.redtooth.profile_server.engine.app_services.BaseMsg;

/**
 * Created by furszy on 7/5/17.
 */

public class ChatAcceptMsg extends BaseMsg<ChatAcceptMsg> {

    long timestamp;

    public ChatAcceptMsg(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String getType() {
        return ChatMsgTypes.CHAT_ACCEPTED.name();
    }

    @Override
    public byte[] encode() throws Exception {
        return SerializationUtils.serialize(this);
    }

    @Override
    public ChatAcceptMsg decode(byte[] msg) throws Exception {
        return SerializationUtils.deserialize(msg,ChatAcceptMsg.class);
    }
}
