package org.fermat.redtooth.forum;

/**
 * Created by mati on 30/11/16.
 */
public class InvalidUserParametersException extends Throwable {
    public InvalidUserParametersException(String user_error) {
        super(user_error);
    }
}
