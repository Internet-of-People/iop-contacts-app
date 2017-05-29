package org.fermat.redtooth.profile_server.processors;

import org.fermat.redtooth.profile_server.IoSession;

/**
 * Created by mati on 09/11/16.
 */

public interface MessageProcessor<M> {

    void execute(IoSession session, int messageId, M message);

}
