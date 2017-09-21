package org.libertaria.world.core.services.pairing;

/**
 * Created by furszy on 6/4/17.
 */

public class PairingMsg extends org.libertaria.world.profile_server.engine.app_services.BaseMsg<PairingMsg> {

    private String name;
    private String senderHost;
    private int pairingRequestId;

    public PairingMsg() {
    }

    public PairingMsg(String name, String senderHost, int pairingRequestId) {
        this.name = name;
        this.senderHost = senderHost;
        this.pairingRequestId = pairingRequestId;
    }

    public String getName() {
        return name;
    }

    public String getSenderHost() {
        return senderHost;
    }

    @Override
    public PairingMsg decode(byte[] msg) throws Exception {
        return org.libertaria.world.global.utils.SerializationUtils.deserialize(msg,PairingMsg.class);
    }

    @Override
    public byte[] encode() throws Exception {
        // lazy encode to test the entire flow first..
        return org.libertaria.world.global.utils.SerializationUtils.serialize(this);
    }

    @Override
    public String getType() {
        return PairingMsgTypes.PAIR_REQUEST.getType();
    }

    public int getPairingRequestId() {
        return pairingRequestId;
    }
}
