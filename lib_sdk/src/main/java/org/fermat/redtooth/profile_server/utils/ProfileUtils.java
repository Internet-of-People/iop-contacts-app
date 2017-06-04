package org.fermat.redtooth.profile_server.utils;

import org.fermat.redtooth.profile_server.ProfileBase;
import org.fermat.redtooth.profile_server.ProfileInformation;

/**
 * Created by mati on 01/06/17.
 */

public class ProfileUtils {

    /**
     * Profile URI example: IoP:profile/<hash>/update?name=Matias
     * @return
     */
    public static String getProfileURI(ProfileBase profileBase){
        return "IoP:profile/"+profileBase.getHexPublicKey()+"/update?name="+profileBase.getName();
    }

}
