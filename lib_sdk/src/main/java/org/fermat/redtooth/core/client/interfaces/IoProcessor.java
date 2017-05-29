package org.fermat.redtooth.core.client.interfaces;

import org.fermat.redtooth.core.client.basic.ConnectionId;
import org.fermat.redtooth.core.client.basic.IoSessionImp;

/**
 * Created by mati on 14/05/17.
 */

public interface IoProcessor {


    void add(IoSessionImp ioSessionImp) throws Exception;

    IoSessionImp getActiveSession(ConnectionId connectionId);

    <M> void scheduleForFlush(IoSessionImp mIoSessionImp);
}
