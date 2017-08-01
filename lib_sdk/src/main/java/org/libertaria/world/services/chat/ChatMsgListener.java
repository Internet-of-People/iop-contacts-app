package org.libertaria.world.services.chat;

import org.libertaria.world.core.services.AppServiceListener;
import org.libertaria.world.profile_server.engine.app_services.BaseMsg;
import org.libertaria.world.profile_server.model.Profile;

/**
 * Created by furszy on 7/3/17.
 */

public interface ChatMsgListener extends AppServiceListener {

    void onChatConnected(Profile localProfile, String remoteProfilePubKey,boolean isLocalCreator);

    void onChatDisconnected(String remotePubKey,String reason);

    void onMsgReceived(String remotePubKey,BaseMsg msg);


}
