package org.fermat.redtooth.core.client.interfaces;

import org.fermat.redtooth.core.client.exceptions.InvalidProtocolViolationException;

import java.nio.ByteBuffer;

/**
 * Created by mati on 11/05/17.
 */

public abstract class ProtocolEncoder<M> {

    public abstract ByteBuffer encode(M message) throws InvalidProtocolViolationException;

}
