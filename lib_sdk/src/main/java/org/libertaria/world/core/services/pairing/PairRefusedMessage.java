package org.libertaria.world.core.services.pairing;

import org.libertaria.world.global.utils.SerializationUtils;
import org.libertaria.world.profile_server.engine.app_services.BaseMsg;

/**
 * Created by furszy on 6/4/17.
 */

public class PairRefusedMessage extends BaseMsg<PairRefusedMessage> {

    private int pairingRequestId;

    public PairRefusedMessage() {
    }

    public PairRefusedMessage(int pairingRequestId) {
        this.pairingRequestId = pairingRequestId;
    }

    @Override
    public PairRefusedMessage decode(byte[] msg) throws Exception {
        return SerializationUtils.deserialize(msg,PairRefusedMessage.class);
    }

    @Override
    public byte[] encode() throws Exception {
        // lazy encode to test the entire flow first..
        return SerializationUtils.serialize(this);
    }

    @Override
    public String getType() {
        return PairingMessageType.PAIR_REFUSE.getType();
    }

    public int getPairingRequestId() {
        return pairingRequestId;
    }
}
