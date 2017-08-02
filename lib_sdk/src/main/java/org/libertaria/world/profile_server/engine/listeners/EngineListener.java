package org.libertaria.world.profile_server.engine.listeners;

import org.libertaria.world.profile_server.engine.app_services.AppService;
import org.libertaria.world.profile_server.engine.app_services.PairingListener;

/**
 * Created by mati on 15/02/17.
 */

public interface EngineListener {

    AppService appServiceInitializer(String appServiceName);

    PairingListener initializePairing();

    // todo: ver si este metodo està bien o es al pedo. El engine es para un solo profile, no deberia hacer esto.
    void onCheckInCompleted(String localProfilePubKey);

    void onDisconnect(String localProfilePubKey);

    //void newCallReceived(CallProfileAppService callProfileAppService);

//    void onProfileSearchReceived(List<IopProfileServer.IdentityNetworkProfileInformation> profileInformationList);


}
