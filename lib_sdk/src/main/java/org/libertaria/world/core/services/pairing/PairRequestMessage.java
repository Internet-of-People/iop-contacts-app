package org.libertaria.world.core.services.pairing;

import org.libertaria.world.global.utils.SerializationUtils;
import org.libertaria.world.profile_server.engine.app_services.BaseMsg;

/**
 * Created by furszy on 6/4/17.
 */

public class PairRequestMessage extends BaseMsg<PairRequestMessage> {

    private String name;
    private String senderHost;
    private int pairingRequestId;

    public PairRequestMessage() {
    }

    public PairRequestMessage(String name, String senderHost, int pairingRequestId) {
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
    public PairRequestMessage decode(byte[] msg) throws Exception {
        return SerializationUtils.deserialize(msg,PairRequestMessage.class);
    }

    @Override
    public byte[] encode() throws Exception {
        // lazy encode to test the entire flow first..
        return SerializationUtils.serialize(this);
    }

    @Override
    public String getType() {
        return PairingMessageType.PAIR_REQUEST.getType();
    }

    public int getPairingRequestId() {
        return pairingRequestId;
    }
}
