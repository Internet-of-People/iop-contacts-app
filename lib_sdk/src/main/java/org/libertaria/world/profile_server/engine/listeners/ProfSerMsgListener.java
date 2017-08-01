package org.libertaria.world.profile_server.engine.listeners;

/**
 * Created by mati on 17/02/17.
 */

public interface ProfSerMsgListener<O> extends ProfSerMsgListenerBase {

    void onMessageReceive(int messageId, O message);

}
