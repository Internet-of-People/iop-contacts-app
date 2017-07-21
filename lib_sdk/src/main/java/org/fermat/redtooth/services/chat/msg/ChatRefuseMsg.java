package org.fermat.redtooth.services.chat.msg;

import org.fermat.redtooth.global.utils.SerializationUtils;
import org.fermat.redtooth.profile_server.engine.app_services.BaseMsg;

/**
 * Created by furszy on 7/5/17.
 */

public class ChatRefuseMsg extends BaseMsg<ChatRefuseMsg> {

    public ChatRefuseMsg() {
    }

    @Override
    public String getType() {
        return ChatMsgTypes.CHAT_REFUSED.name();
    }

    @Override
    public byte[] encode() throws Exception {
        return SerializationUtils.serialize(this);
    }

    @Override
    public ChatRefuseMsg decode(byte[] msg) throws Exception {
        return SerializationUtils.deserialize(msg,ChatRefuseMsg.class);
    }
}
