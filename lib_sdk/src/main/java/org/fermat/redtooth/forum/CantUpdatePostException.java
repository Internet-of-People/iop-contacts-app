package org.fermat.redtooth.forum;

/**
 * Created by mati on 25/12/16.
 */
public class CantUpdatePostException extends Exception {

    public CantUpdatePostException(String s) {
        super(s);
    }

    public CantUpdatePostException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
