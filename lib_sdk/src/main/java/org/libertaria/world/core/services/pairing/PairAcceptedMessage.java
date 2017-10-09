package org.libertaria.world.core.services.pairing;

import org.libertaria.world.global.utils.SerializationUtils;
import org.libertaria.world.profile_server.engine.app_services.BaseMsg;

/**
 * Created by furszy on 6/4/17.
 */

public class PairAcceptedMessage extends BaseMsg<PairAcceptedMessage> {

    private int pairingRequestId;

    public PairAcceptedMessage() {
    }

    public PairAcceptedMessage(int pairingRequestId) {
        this.pairingRequestId = pairingRequestId;
    }

    @Override
    public PairAcceptedMessage decode(byte[] msg) throws Exception {
        return SerializationUtils.deserialize(msg,PairAcceptedMessage.class);
    }

    @Override
    public byte[] encode() throws Exception {
        // lazy encode to test the entire flow first..
        return SerializationUtils.serialize(this);
    }

    @Override
    public String getType() {
        return PairingMessageType.PAIR_ACCEPT.getType();
    }

    public int getPairingRequestId() {
        return pairingRequestId;
    }
}
