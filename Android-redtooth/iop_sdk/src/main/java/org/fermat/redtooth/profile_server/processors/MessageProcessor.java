package org.fermat.redtooth.profile_server.processors;

/**
 * Created by mati on 09/11/16.
 */

public interface MessageProcessor<M> {

    void execute(int messageId, M message);

}
