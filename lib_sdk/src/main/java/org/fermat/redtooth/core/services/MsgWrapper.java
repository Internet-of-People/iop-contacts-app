package org.fermat.redtooth.core.services;

import org.fermat.redtooth.global.utils.SerializationUtils;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by furszy on 6/5/17.
 */

public class MsgWrapper implements Serializable {

    private BaseMsg msg;
    private String msgType;

    public MsgWrapper(BaseMsg msg, String msgType) {
        this.msg = msg;
        this.msgType = msgType;
    }

    public BaseMsg getMsg() {
        return msg;
    }

    public String getMsgType() {
        return msgType;
    }

    public byte[] encode() throws IOException {
        return SerializationUtils.serialize(this);
    }

    public static MsgWrapper decode(byte[] data) throws IOException, ClassNotFoundException {
        return SerializationUtils.deserialize(data,MsgWrapper.class);
    }
}
