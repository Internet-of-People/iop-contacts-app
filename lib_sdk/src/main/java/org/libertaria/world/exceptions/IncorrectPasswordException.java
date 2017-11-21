package org.libertaria.world.exceptions;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 21/11/2017.
 */

public class IncorrectPasswordException extends Exception {

    public IncorrectPasswordException() {
    }

    public IncorrectPasswordException(String s) {
        super(s);
    }

    public IncorrectPasswordException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public IncorrectPasswordException(Throwable throwable) {
        super(throwable);
    }

    public IncorrectPasswordException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
