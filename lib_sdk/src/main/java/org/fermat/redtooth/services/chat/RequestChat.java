package org.fermat.redtooth.services.chat;

import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.model.Profile;

/**
 * Created by furszy on 7/3/17.
 */

public class RequestChat {

    private ProfileInformation remoteProfile;
    private Profile localProfile;

    public RequestChat(ProfileInformation remoteProfile, Profile localProfile) {
        this.remoteProfile = remoteProfile;
        this.localProfile = localProfile;
    }

    public ProfileInformation getRemoteProfile() {
        return remoteProfile;
    }

    public Profile getLocalProfile() {
        return localProfile;
    }
}
