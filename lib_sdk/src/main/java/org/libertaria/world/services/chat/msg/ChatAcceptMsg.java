package org.libertaria.world.services.chat.msg;

/**
 * Created by furszy on 7/5/17.
 */

public class ChatAcceptMsg extends org.libertaria.world.profile_server.engine.app_services.BaseMsg<ChatAcceptMsg> {

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
        return org.libertaria.world.global.utils.SerializationUtils.serialize(this);
    }

    @Override
    public ChatAcceptMsg decode(byte[] msg) throws Exception {
        return org.libertaria.world.global.utils.SerializationUtils.deserialize(msg,ChatAcceptMsg.class);
    }
}
