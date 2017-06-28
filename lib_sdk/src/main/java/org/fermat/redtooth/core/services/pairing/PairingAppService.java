package org.fermat.redtooth.core.services.pairing;

import org.fermat.redtooth.core.services.DefaultServices;
import org.fermat.redtooth.profile_server.engine.app_services.MsgWrapper;
import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.profile_server.engine.app_services.AppService;
import org.fermat.redtooth.profile_server.engine.app_services.CallProfileAppService;
import org.fermat.redtooth.profile_server.engine.app_services.PairingListener;
import org.fermat.redtooth.profile_server.imp.ProfileInformationImp;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profiles_manager.PairingRequest;
import org.fermat.redtooth.profiles_manager.PairingRequestsManager;
import org.fermat.redtooth.profiles_manager.ProfilesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by furszy on 6/8/17.
 */

public class PairingAppService extends AppService {

    private static final Logger logger = LoggerFactory.getLogger(PairingAppService.class);

    private Profile profileServiceOwner;
    private PairingListener pairingListener;
    private PairingRequestsManager pairingRequestsManager;
    private ProfilesManager profilesManager;

    public PairingAppService(Profile profileServiceOwner,PairingRequestsManager pairingRequestsManager, ProfilesManager profilesManager,PairingListener pairingListener) {
        super(DefaultServices.PROFILE_PAIRING.getName());
        this.profilesManager = profilesManager;
        this.profileServiceOwner = profileServiceOwner;
        this.pairingListener = pairingListener;
        this.pairingRequestsManager = pairingRequestsManager;
    }

    /**
     * Wrap call in a PairingAppService call.
     * @param callProfileAppService
     */
    @Override
    public void wrapCall(final CallProfileAppService callProfileAppService) {
        if (pairingListener!=null){
            callProfileAppService.setMsgListener(new CallProfileAppService.CallMessagesListener() {
                @Override
                public void onMessage(byte[] msg) {
                    try {
                        logger.info("pair msg received");
                        MsgWrapper msgWrapper = MsgWrapper.decode(msg);
                        PairingMsgTypes types = PairingMsgTypes.getByName(msgWrapper.getMsgType());
                        switch (types){
                            case PAIR_ACCEPT:
                                // update pair request -> todo: this should be in another place..
                                pairingRequestsManager.updateStatus(profileServiceOwner.getHexPublicKey(),callProfileAppService.getRemotePubKey(),PairingMsgTypes.PAIR_ACCEPT);
                                profilesManager.updatePaired(profileServiceOwner.getPublicKey(), ProfileInformationImp.PairStatus.PAIRED);
                                if (pairingListener!=null){
                                    pairingListener.onPairResponseReceived(callProfileAppService.getRemotePubKey(),"Accepted");
                                }else {
                                    logger.info("pairListener null, please add it if you want to receive pairs");
                                }
                                break;
                            case PAIR_REFUSE:
                                // update pair request -> todo: this should be in another place..
                                pairingRequestsManager.updateStatus(profileServiceOwner.getHexPublicKey(),callProfileAppService.getRemotePubKey(),PairingMsgTypes.PAIR_REFUSE);
                                if (pairingListener!=null){
                                    pairingListener.onPairResponseReceived(callProfileAppService.getRemotePubKey(),"Refused");
                                }else {
                                    logger.info("pairListener null, please add it if you want to receive pairs");
                                }
                                break;
                            case PAIR_REQUEST:
                                PairingMsg pairingMsg = (PairingMsg) msgWrapper.getMsg();
                                // save pair request -> todo: this should be in another place..
                                PairingRequest pairingRequest = PairingRequest.buildPairingRequest(callProfileAppService.getRemotePubKey(),profileServiceOwner.getHexPublicKey(),profileServiceOwner.getNetworkIdHex(),pairingMsg.getName(),pairingMsg.getSenderHost());
                                pairingRequest.setRemotePsHome(profileServiceOwner.getHomeHost());
                                pairingRequestsManager.saveIfNotExistPairingRequest(pairingRequest);
                                profilesManager.updatePaired(CryptoBytes.fromHexToBytes(pairingRequest.getSenderPubKey()), ProfileInformationImp.PairStatus.WAITING_FOR_MY_RESPONSE);
                                if (pairingListener!=null){
                                    pairingListener.onPairReceived(callProfileAppService.getRemotePubKey(),pairingMsg.getName());
                                }else {
                                    logger.info("pairListener null, please add it if you want to receive pairs");
                                }
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
