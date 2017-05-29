package org.fermat.redtooth.forum;

/**
 * Created by mati on 03/03/17.
 */
public class CantReplayPostException extends Throwable {
    public CantReplayPostException(String message) {
        super(message);
    }
}
