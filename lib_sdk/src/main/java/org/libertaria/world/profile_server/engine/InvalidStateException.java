package org.libertaria.world.profile_server.engine;

/**
 * Created by mati on 05/02/17.
 */
public class InvalidStateException extends RuntimeException {

    public InvalidStateException(String s) {
        super(s);
    }

    public InvalidStateException(String state, String validState) {
        super("State: "+state+" must be "+ ProfSerConnectionState.NO_SERVER.toString());
    }
}
