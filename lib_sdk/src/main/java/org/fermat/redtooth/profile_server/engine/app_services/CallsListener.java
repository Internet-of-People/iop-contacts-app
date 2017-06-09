package org.fermat.redtooth.profile_server.engine.app_services;

import org.fermat.redtooth.profile_server.engine.app_services.AppServiceMsg;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;

/**
 * Created by mati on 19/05/17.
 */

public interface CallsListener {

    void incomingCallNotification(int messageId, IopProfileServer.IncomingCallNotificationRequest message);

    void incomingAppServiceMessage(int messageId, AppServiceMsg message);



}
