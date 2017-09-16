package org.libertaria.world.exceptions;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 5/9/2017.
 */

public class CantStartException extends Exception {

    private static final String DEFAULT_MESSAGE = "There was a problem while starting this module.";

    public CantStartException() {
        super(DEFAULT_MESSAGE);
    }

    public CantStartException(Throwable throwable) {
        super(DEFAULT_MESSAGE, throwable);
    }
}
