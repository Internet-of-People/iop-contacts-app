package org.libertaria.world.services.chat.msg;

/**
 * Created by furszy on 7/3/17.
 */

public class RequestChat {

    private org.libertaria.world.profile_server.ProfileInformation remoteProfile;
    private org.libertaria.world.profile_server.model.Profile localProfile;

    public RequestChat(org.libertaria.world.profile_server.ProfileInformation remoteProfile, org.libertaria.world.profile_server.model.Profile localProfile) {
        this.remoteProfile = remoteProfile;
        this.localProfile = localProfile;
    }

    public org.libertaria.world.profile_server.ProfileInformation getRemoteProfile() {
        return remoteProfile;
    }

    public org.libertaria.world.profile_server.model.Profile getLocalProfile() {
        return localProfile;
    }
}
