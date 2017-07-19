package iop.org.iop_sdk_android.core.service.exceptions;

import org.fermat.redtooth.profile_server.ProfileInformation;

/**
 * Created by furszy on 7/6/17.
 */
public class ChatCallClosedException extends Exception {

    ProfileInformation remoteProfileInformation;

    public ChatCallClosedException(String s, ProfileInformation remoteProfileInformation) {
        super(s);
        this.remoteProfileInformation = remoteProfileInformation;
    }
}
