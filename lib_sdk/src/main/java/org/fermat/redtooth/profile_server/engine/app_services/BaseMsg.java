package org.fermat.redtooth.profile_server.engine.app_services;

import org.apache.http.MethodNotSupportedException;

import java.io.Serializable;

/**
 * Created by furszy on 6/4/17.
 */

public abstract class BaseMsg<T> implements Serializable{

    public T decode(byte[] msg) throws Exception {
        throw new MethodNotSupportedException("method not implemented");
    }

    public byte[] encode() throws Exception {
        throw new MethodNotSupportedException("method not implemented");
    }

    public abstract String getType();
}
