package org.fermat.redtooth.core.client.interfaces;

import org.fermat.redtooth.core.client.exceptions.InvalidProtocolViolationException;

import java.nio.ByteBuffer;

/**
 * Created by mati on 11/05/17.
 */

public abstract class ProtocolDecoder<M> {

    public abstract M decode(ByteBuffer byteBuffer) throws InvalidProtocolViolationException;

}
