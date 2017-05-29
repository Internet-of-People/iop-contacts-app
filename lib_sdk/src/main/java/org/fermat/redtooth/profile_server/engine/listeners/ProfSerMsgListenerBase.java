package org.fermat.redtooth.profile_server.engine.listeners;

/**
 * Created by mati on 30/03/17.
 */

interface ProfSerMsgListenerBase {

    void onMsgFail(int messageId, int statusValue, String details);

    String getMessageName();

}
