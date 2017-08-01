package org.libertaria.world.profile_server.processors;

/**
 * Created by mati on 09/11/16.
 */

public interface MessageProcessor<M> {

    void execute(org.libertaria.world.profile_server.IoSession session, int messageId, M message);

}
