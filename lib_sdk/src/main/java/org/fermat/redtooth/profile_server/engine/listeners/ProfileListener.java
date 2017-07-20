package org.fermat.redtooth.profile_server.engine.listeners;

import org.fermat.redtooth.profile_server.model.Profile;

/**
 * Created by furszy on 6/7/17.
 */

public interface ProfileListener {

    void onConnect(Profile profile);

    void onDisconnect(Profile profile);

    void onCheckInFail(Profile profile, int status, String statusDetail);
}
