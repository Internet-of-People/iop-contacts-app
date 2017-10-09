package org.libertaria.world.core.exceptions;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 5/10/2017.
 */

public class ConnectionAlreadyInitializedException extends Exception {

    public ConnectionAlreadyInitializedException(String s) {
        super(s);
    }

    public ConnectionAlreadyInitializedException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ConnectionAlreadyInitializedException(Throwable throwable) {
        super(throwable);
    }

    public ConnectionAlreadyInitializedException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }

    public ConnectionAlreadyInitializedException() {
    }
}
