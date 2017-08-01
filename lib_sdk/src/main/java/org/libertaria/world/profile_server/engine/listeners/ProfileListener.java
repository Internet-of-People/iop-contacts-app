package org.libertaria.world.profile_server.engine.listeners;

/**
 * Created by furszy on 6/7/17.
 */

public interface ProfileListener {

    void onConnect(org.libertaria.world.profile_server.model.Profile profile);

    void onDisconnect(org.libertaria.world.profile_server.model.Profile profile);

    void onCheckInFail(org.libertaria.world.profile_server.model.Profile profile, int status, String statusDetail);
}
