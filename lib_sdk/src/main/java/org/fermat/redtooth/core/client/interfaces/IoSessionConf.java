package org.fermat.redtooth.core.client.interfaces;

import org.fermat.redtooth.core.client.basic.IdleStatus;
import org.fermat.redtooth.core.client.basic.IoSessionImp;

/**
 * Created by mati on 11/05/17.
 */

public interface IoSessionConf {


    /** Read buffer size */
    int getReadBufferSize();
    /** Max read buffer size */
    int getMaxReadBufferSize();
    /** Write buffer size */
    int getWriteBufferSize();
    /** Get socket timeout in millis */
    int getSocketTimeout();
    /** Max time that this session could be open on idle status */
    int getIdleTime(IdleStatus status);
    /** Max time that this session could be open on idle status in millis */
    long getIdleTimeInMillis(IdleStatus idleStatus);

    long getWriteTimeoutInMillis();

    int getSocketReadBufferSize();

    Class<? extends IoSessionImp> getIoSessionClass();

    ProtocolDecoder getProtocolDecoder();

    ProtocolEncoder getProtocolEncoder();

}
