package org.fermat.redtooth.core.services;

import org.apache.http.MethodNotSupportedException;

/**
 * Created by furszy on 6/4/17.
 */

public class BaseMsg<T> {

    public T decode(byte[] msg) throws Exception {
        throw new MethodNotSupportedException("method not implemented");
    }

    public byte[] encode() throws Exception {
        throw new MethodNotSupportedException("method not implemented");
    }
}
