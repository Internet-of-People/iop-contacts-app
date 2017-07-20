package org.fermat.redtooth.services.chat;

import org.fermat.redtooth.global.utils.SerializationUtils;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.app_services.BaseMsg;

import java.io.Serializable;

/**
 * Created by furszy on 7/3/17.
 */

public class ChatMsg extends BaseMsg<ChatMsg> implements Serializable{

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
        return SerializationUtils.deserialize(msg,ChatMsg.class);
    }

    @Override
    public byte[] encode() throws Exception {
        return SerializationUtils.serialize(this);
    }

    public long getTimestamp() {
        return timestamp;
    }
}
