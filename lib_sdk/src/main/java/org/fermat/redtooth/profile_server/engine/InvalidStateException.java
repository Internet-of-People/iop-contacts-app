package org.fermat.redtooth.profile_server.engine;

import static org.fermat.redtooth.profile_server.engine.ProfSerConnectionState.NO_SERVER;

/**
 * Created by mati on 05/02/17.
 */
public class InvalidStateException extends RuntimeException {

    public InvalidStateException(String s) {
        super(s);
    }

    public InvalidStateException(String state, String validState) {
        super("State: "+state+" must be "+NO_SERVER.toString());
    }
}
