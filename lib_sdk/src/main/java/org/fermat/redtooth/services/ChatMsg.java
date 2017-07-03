package org.fermat.redtooth.services;

import org.fermat.redtooth.profile_server.ProfileInformation;

/**
 * Created by furszy on 7/3/17.
 */

public class ChatMsg {

    private ProfileInformation from;
    private ProfileInformation to;
    private String text;

    public ChatMsg(ProfileInformation from, ProfileInformation to, String text) {
        this.from = from;
        this.to = to;
        this.text = text;
    }

    public ProfileInformation getFrom() {
        return from;
    }

    public ProfileInformation getTo() {
        return to;
    }

    public String getText() {
        return text;
    }
}
