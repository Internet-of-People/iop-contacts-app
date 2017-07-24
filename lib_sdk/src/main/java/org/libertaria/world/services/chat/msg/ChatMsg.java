package org.libertaria.world.services.chat.msg;

import java.io.Serializable;

/**
 * Created by furszy on 7/3/17.
 */

public class ChatMsg extends org.libertaria.world.profile_server.engine.app_services.BaseMsg<ChatMsg> implements Serializable{

    private String text;
    private long timestamp;

    public ChatMsg(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String getType() {
        return ChatMsgTypes.TEXT.name();
    }

    @Override
    public ChatMsg decode(byte[] msg) throws Exception {
        return org.libertaria.world.global.utils.SerializationUtils.deserialize(msg,ChatMsg.class);
    }

    @Override
    public byte[] encode() throws Exception {
        return org.libertaria.world.global.utils.SerializationUtils.serialize(this);
    }

    public long getTimestamp() {
        return timestamp;
    }
}
