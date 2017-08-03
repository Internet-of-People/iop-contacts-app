package org.libertaria.world.profiles_manager;

import org.libertaria.world.profile_server.imp.ProfileInformationImp;

import java.util.List;

/**
 * Created by furszy on 6/6/17.
 */

public interface PairingRequestsManager {

    int savePairingRequest(PairingRequest pairingRequest);

    int saveIfNotExistPairingRequest(PairingRequest pairingRequest);

    PairingRequest getPairingRequest(String senderPubKey, String remotePubkey);

    List<PairingRequest> pairingRequests(String senderPubKey);

    List<PairingRequest> openPairingRequests(String senderPubKey);

    boolean updateStatus(String senderPubKey, String remotePubKey, org.libertaria.world.core.services.pairing.PairingMsgTypes status, ProfileInformationImp.PairStatus paired);

    int disconnectPairingProfile(String senderPubKey, String remotePubKey);

    int removeRequest(String senderPubKey, String remotePubkey);

    void delete(long id);

    void truncate();
}
