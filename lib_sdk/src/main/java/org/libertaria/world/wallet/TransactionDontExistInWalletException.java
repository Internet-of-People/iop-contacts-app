package org.libertaria.world.wallet;

/**
 * Created by mati on 06/03/17.
 */
public class TransactionDontExistInWalletException extends Exception {

    public TransactionDontExistInWalletException(String s) {
        super(s);
    }
}
