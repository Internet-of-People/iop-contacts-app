package org.libertaria.world.wallet;

/**
 * Created by mati on 26/12/16.
 */

public interface WalletManagerListener {

    void onWalletRestored();

    boolean isOutputLocked(String hash, long index);

}
