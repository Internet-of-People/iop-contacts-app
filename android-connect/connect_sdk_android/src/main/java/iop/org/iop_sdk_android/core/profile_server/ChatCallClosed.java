package iop.org.iop_sdk_android.core.profile_server;

import org.fermat.redtooth.profile_server.ProfileInformation;

/**
 * Created by furszy on 7/6/17.
 */
public class ChatCallClosed extends Exception {

    ProfileInformation remoteProfileInformation;

    public ChatCallClosed(String s, ProfileInformation remoteProfileInformation) {
        super(s);
        this.remoteProfileInformation = remoteProfileInformation;
    }
}
