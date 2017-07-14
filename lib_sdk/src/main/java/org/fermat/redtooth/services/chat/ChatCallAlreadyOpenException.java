package org.fermat.redtooth.services.chat;

/**
 * Created by furszy on 7/13/17.
 */

public class ChatCallAlreadyOpenException extends Exception {

    public ChatCallAlreadyOpenException(String message) {
        super(message);
    }
}
