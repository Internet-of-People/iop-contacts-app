package org.fermat.redtooth.services.interfaces;

import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profiles_manager.PairingRequest;

import org.fermat.redtooth.global.Module;

import java.util.List;

/**
 * Created by furszy on 7/20/17.
 */

public interface PairingModule extends Module {

    void requestPairingProfile(String localProfilePubKey, byte[] remotePubKey, final String remoteName, final String psHost, final ProfSerMsgListener<ProfileInformation> listener) throws Exception;

    void acceptPairingProfile(PairingRequest pairingRequest, ProfSerMsgListener<Boolean> profSerMsgListener) throws Exception;

    void cancelPairingRequest(PairingRequest pairingRequest);

    List<PairingRequest> getPairingRequests();
}
