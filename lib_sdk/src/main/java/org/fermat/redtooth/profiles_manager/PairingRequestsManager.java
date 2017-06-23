package org.fermat.redtooth.profiles_manager;

import org.fermat.redtooth.core.services.pairing.PairingMsgTypes;

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

    boolean updateStatus(String senderPubKey, String remotePubKey, PairingMsgTypes status);

    int removeRequest(String senderPubKey, String remotePubkey);
}
