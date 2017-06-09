package org.fermat.redtooth.profile_server.engine.app_services;

/**
 * Created by furszy on 6/4/17.
 */

public interface PairingListener {

    void onPairReceived(String requesteePubKey,String name);

    void onPairResponseReceived(String requesteePubKey,String responseDetail);

}
