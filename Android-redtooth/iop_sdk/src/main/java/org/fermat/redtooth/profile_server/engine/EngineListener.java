package org.fermat.redtooth.profile_server.engine;

import org.fermat.redtooth.profile_server.model.Profile;

/**
 * Created by mati on 15/02/17.
 */

public interface EngineListener {

    void onCheckInCompleted(Profile profile);

//    void onProfileSearchReceived(List<IopProfileServer.IdentityNetworkProfileInformation> profileInformationList);


}
