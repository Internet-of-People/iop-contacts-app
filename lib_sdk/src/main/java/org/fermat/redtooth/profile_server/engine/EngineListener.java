package org.fermat.redtooth.profile_server.engine;

import org.fermat.redtooth.profile_server.model.Profile;

/**
 * Created by mati on 15/02/17.
 */

public interface EngineListener {

    // todo: ver si este metodo està bien o es al pedo. El engine es para un solo profile, no deberia hacer esto.
    void onCheckInCompleted(Profile profile);

    void newCallReceived(CallProfileAppService callProfileAppService);

//    void onProfileSearchReceived(List<IopProfileServer.IdentityNetworkProfileInformation> profileInformationList);


}
