package org.fermat.redtooth.blockchain.explorer;

import org.bitcoinj.core.Transaction;

/**
 * Created by mati on 19/12/16.
 */
public interface TransactionFinderListener {

    void onReceiveTransaction(Transaction tx);

}
