package org.fermat.redtooth.services.chat;

import org.fermat.redtooth.core.services.AppServiceListener;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.app_services.BaseMsg;
import org.fermat.redtooth.profile_server.model.Profile;

/**
 * Created by furszy on 7/3/17.
 */

public interface ChatMsgListener extends AppServiceListener {

    void onChatConnected(Profile localProfile, String remoteProfilePubKey,boolean isLocalCreator);

    void onChatDisconnected(String remotePubKey);

    void onMsgReceived(String remotePubKey,BaseMsg msg);


}
