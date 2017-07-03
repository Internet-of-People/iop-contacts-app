package org.fermat.redtooth.services.chat;

import org.fermat.redtooth.core.services.AppServiceListener;

/**
 * Created by furszy on 7/3/17.
 */

public interface ChatMsgListener extends AppServiceListener {

    void onChatConnected(String remotePubKey);

    void onChatDisconnected(String remotePubKey);

    void onMsgReceived(String remotePubKey,byte[] msg);

}
