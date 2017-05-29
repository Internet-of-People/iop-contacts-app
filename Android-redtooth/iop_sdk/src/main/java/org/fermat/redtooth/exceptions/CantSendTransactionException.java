package org.fermat.redtooth.exceptions;

import org.bitcoinj.wallet.Wallet;

/**
 * Created by mati on 21/12/16.
 */
public class CantSendTransactionException extends Throwable {
    public CantSendTransactionException(String s, Wallet.DustySendRequested e) {
        super(s,e);
    }

    public CantSendTransactionException(String message) {
        super(message);
    }
}
