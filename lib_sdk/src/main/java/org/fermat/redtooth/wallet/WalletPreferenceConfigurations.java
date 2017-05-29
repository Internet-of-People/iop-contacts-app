package org.fermat.redtooth.wallet;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;

import java.math.BigDecimal;

/**
 * Created by mati on 25/12/16.
 */
public interface WalletPreferenceConfigurations {

    boolean getConnectivityNotificationEnabled();

    int getBestChainHeightEver();

    int maybeIncrementBestChainHeightEver(final int bestChainHeightEver);

    void saveNode(String host);

    String getNode();

    void saveLastIoPUsdPrice(BigDecimal usd);

    void setLastIopPriceRequest(long time);

    long getLastIoPPriceRequest();

    BigDecimal getLastIoPUsdPrice();

    BigDecimal getLastMonthIoPRate();

    void saveLastMonthIoPRate(BigDecimal usd);

    void saveReceiveAddress(String address);

    String getReceiveAddress();

    /** Admin notification popup to alert users about something from the server */
    int getAdminNotification();

    void setAdminNotification(int type);

    /****** PREFERENCE CONSTANTS   ******/

    String getMnemonicFilename();

    String getWalletProtobufFilename();

    NetworkParameters getNetworkParams();

    Context getWalletContext();

    String getKeyBackupProtobuf();

    long getBackupMaxChars();

    boolean isTest();

    long getWalletAutosaveDelayMs();

    String getBlockchainFilename();

    int getPeerTimeoutMs();

    long getPeerDiscoveryTimeoutMs();

    void remove();

    String getCheckpointFilename();

    /****** END PREFERENCE CONSTANTS   ******/


}
