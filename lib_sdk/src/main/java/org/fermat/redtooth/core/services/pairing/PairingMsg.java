package org.fermat.redtooth.core.services.pairing;

import org.fermat.redtooth.profile_server.engine.app_services.BaseMsg;
import org.fermat.redtooth.global.utils.SerializationUtils;

import java.io.Serializable;

/**
 * Created by furszy on 6/4/17.
 */

public class PairingMsg extends BaseMsg<PairingMsg> {

    private String name;
    private String senderHost;

    public PairingMsg() {
    }

    public PairingMsg(String name,String senderHost) {
        this.name = name;
        this.senderHost = senderHost;
    }

    public String getName() {
        return name;
    }

    public String getSenderHost() {
        return senderHost;
    }

    @Override
    public PairingMsg decode(byte[] msg) throws Exception {
        return SerializationUtils.deserialize(msg,PairingMsg.class);
    }

    @Override
    public byte[] encode() throws Exception {
        // lazy encode to test the entire flow first..
        return SerializationUtils.serialize(this);
    }

    @Override
    public String getType() {
        return PairingMsgTypes.PAIR_REQUEST.getType();
    }


}
