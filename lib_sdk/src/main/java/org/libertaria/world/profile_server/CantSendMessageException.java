package org.libertaria.world.profile_server;

/**
 * Created by mati on 20/11/16.
 */
public class CantSendMessageException extends Exception {

    public CantSendMessageException(String s){
        super(s);
    }

    public CantSendMessageException(Exception e) {
        super(e);
    }
}
