package org.libertaria.world.services.chat;

import org.libertaria.world.profile_server.ProfileInformation;

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
