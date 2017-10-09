package org.libertaria.world.core.services.pairing;

import org.libertaria.world.global.utils.SerializationUtils;
import org.libertaria.world.profile_server.engine.app_services.BaseMsg;

/**
 * Created by German on 02/08/2017.
 */

public class PairDisconnectedMessage extends BaseMsg<PairDisconnectedMessage> {

    public PairDisconnectedMessage(){}

    @Override
    public byte[] encode() throws Exception {
        return SerializationUtils.serialize(this);
    }

    @Override
    public PairDisconnectedMessage decode(byte[] msg) throws Exception {
        return SerializationUtils.deserialize(msg,PairDisconnectedMessage.class);
    }

    @Override
    public String getType() {
        return PairingMessageType.PAIR_DISCONNECT.getType();
    }
}
