package org.fermat.redtooth.profile_server.engine.listeners;

import org.fermat.redtooth.profile_server.engine.CallProfileAppService;

/**
 * Created by furszy on 6/4/17.
 */

public interface PairingListener {

    void onPairReceived(String requesteePubKey,String name);

}
