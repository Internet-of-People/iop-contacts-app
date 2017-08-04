package org.libertaria.world.core.services;

import org.libertaria.world.profile_server.ProfileInformation;

/**
 * Created by furszy on 7/3/17.
 */

public interface AppServiceListener {
    /**
     * Method called before initilize an incoming call.
     * @return true if the call is permitted
     * @param localPublicKey
     * @param remoteProfile
     */
    boolean onPreCall(String localPublicKey, ProfileInformation remoteProfile);
}
