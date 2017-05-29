package org.fermat.redtooth.wallet.exceptions;

/**
 * Created by mati on 17/11/16.
 */

public class InsuficientBalanceException extends Exception {

    public InsuficientBalanceException(String message) {
        super(message);
    }

    public InsuficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
