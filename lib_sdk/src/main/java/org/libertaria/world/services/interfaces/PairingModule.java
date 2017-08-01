package org.libertaria.world.services.interfaces;

import org.libertaria.world.global.Module;
import org.libertaria.world.profile_server.ProfileInformation;
import org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener;
import org.libertaria.world.profiles_manager.PairingRequest;

import java.util.List;

/**
 * Created by furszy on 7/20/17.
 */

public interface PairingModule extends Module {

    void requestPairingProfile(String localProfilePubKey, byte[] remotePubKey, final String remoteName, final String psHost, final ProfSerMsgListener<ProfileInformation> listener) throws Exception;

    void acceptPairingProfile(PairingRequest pairingRequest, ProfSerMsgListener<Boolean> profSerMsgListener) throws Exception;

    void cancelPairingRequest(PairingRequest pairingRequest);

    List<PairingRequest> getPairingRequests(String localProfPubKey);
}
