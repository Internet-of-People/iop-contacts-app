package org.fermat.redtooth.core.services.pairing;

import org.fermat.redtooth.core.IoPConnect;
import org.fermat.redtooth.services.EnabledServices;
import org.fermat.redtooth.profile_server.ProfileInformation;
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

import java.io.File;

/**
 * Created by furszy on 6/8/17.
 */

public class PairingAppService extends AppService {

    private static final Logger logger = LoggerFactory.getLogger(PairingAppService.class);

    private Profile profileServiceOwner;
    private PairingListener pairingListener;
    private PairingRequestsManager pairingRequestsManager;
    private ProfilesManager profilesManager;
    private IoPConnect ioPConnect;

    private File backupProfileFile;
    private String backupProfilePassword;

    public PairingAppService(Profile profileServiceOwner,PairingRequestsManager pairingRequestsManager, ProfilesManager profilesManager,PairingListener pairingListener,IoPConnect ioPConnect) {
        super(EnabledServices.PROFILE_PAIRING.getName());
        this.profilesManager = profilesManager;
        this.profileServiceOwner = profileServiceOwner;
        this.pairingListener = pairingListener;
        this.pairingRequestsManager = pairingRequestsManager;
        this.ioPConnect = ioPConnect;
    }

    public void setBackupProfile(String backupProfilePath,String backupProfilePassword){
        this.backupProfileFile = new File(backupProfilePath);
        this.backupProfilePassword = backupProfilePassword;
    }


    /**
     * Wrap call in a PairingAppService call.
     * @param callProfileAppService
     */
    @Override
    public void onWrapCall(final CallProfileAppService callProfileAppService) {
        if (pairingListener!=null){
            callProfileAppService.setMsgListener(new CallProfileAppService.CallMessagesListener() {
                @Override
                public void onMessage(MsgWrapper msg) {
                    try {
                        logger.info("pair msg received");

                        PairingMsgTypes types = PairingMsgTypes.getByName(msg.getMsgType());
                        switch (types){
                            case PAIR_ACCEPT:
                                // update pair request -> todo: this should be in another place..
                                boolean updateStatus = pairingRequestsManager.updateStatus(
                                        profileServiceOwner.getHexPublicKey(),
                                        callProfileAppService.getRemotePubKey(),
                                        PairingMsgTypes.PAIR_ACCEPT,
                                        ProfileInformationImp.PairStatus.PAIRED);
                                boolean res = profilesManager.updatePaired(
                                        callProfileAppService.getLocalProfile().getHexPublicKey(),
                                        callProfileAppService.getRemotePubKey(),
                                        ProfileInformationImp.PairStatus.PAIRED
                                );
                                logger.info("Pairing accepted, profiles updated "+res+", update status: "+updateStatus);
                                if (pairingListener!=null){
                                    pairingListener.onPairResponseReceived(callProfileAppService.getRemotePubKey(),"Accepted");
                                }else {
                                    logger.info("pairListener null, please add it if you want to receive pairs");
                                }
                                break;
                            case PAIR_REFUSE:
                                // check if we already have the contact or already have the request
                                ProfileInformation profileInformationDb = profilesManager.getProfile(profileServiceOwner.getHexPublicKey(),callProfileAppService.getRemotePubKey());
                                if (profileInformationDb!=null && profileInformationDb.getPairStatus()!=null){
                                    // Already known profile, shutdown the call now.
                                    logger.info("Pairing profile receive from a known profile, disposing the appService call..");
                                    callProfileAppService.dispose();
                                    return;
                                }

                                // update pair request -> todo: this should be in another place..
                                pairingRequestsManager.updateStatus(
                                        profileServiceOwner.getHexPublicKey(),
                                        callProfileAppService.getRemotePubKey(),
                                        PairingMsgTypes.PAIR_REFUSE,
                                        ProfileInformationImp.PairStatus.PAIRED
                                );
                                if (pairingListener!=null){
                                    pairingListener.onPairResponseReceived(callProfileAppService.getRemotePubKey(),"Refused");
                                }else {
                                    logger.info("pairListener null, please add it if you want to receive pairs");
                                }
                                break;
                            case PAIR_REQUEST:
                                PairingMsg pairingMsg = (PairingMsg) msg.getMsg();
                                // save pair request -> todo: this should be in another place..
                                PairingRequest pairingRequest = PairingRequest.buildPairingRequest(
                                        callProfileAppService.getRemotePubKey(),
                                        profileServiceOwner.getHexPublicKey(),
                                        profileServiceOwner.getNetworkIdHex(),
                                        pairingMsg.getName(),
                                        pairingMsg.getSenderHost(),
                                        profileServiceOwner.getName(),
                                        ProfileInformationImp.PairStatus.WAITING_FOR_MY_RESPONSE
                                );
                                pairingRequest.setRemotePsHome(profileServiceOwner.getHomeHost());
                                int prId = pairingRequestsManager.saveIfNotExistPairingRequest(pairingRequest);

                                ProfileInformation profileInformation = callProfileAppService.getRemoteProfile();
                                profileInformation.setName(pairingMsg.getName());
                                profileInformation.setHomeHost(pairingMsg.getSenderHost());
                                profileInformation.setPairStatus(ProfileInformationImp.PairStatus.WAITING_FOR_MY_RESPONSE);

                                ProfileInformation dbProfile = profilesManager.getProfile(
                                        callProfileAppService.getLocalProfile().getHexPublicKey(),
                                        profileInformation.getHexPublicKey()
                                );
                                if (dbProfile!=null){
                                    logger.info("Profile already exist.., checking if the local profile accept this person..");
                                    // if the profile is already a connection or if i'm waiting for his approvance
                                    if(dbProfile.getPairStatus() == ProfileInformationImp.PairStatus.PAIRED || dbProfile.getPairStatus() == ProfileInformationImp.PairStatus.WAITING_FOR_RESPONSE){
                                        // answer with an ok paired!
                                        callProfileAppService.sendMsg(PairingMsgTypes.PAIR_ACCEPT.getType(), null); // no need of a future here..
                                    }
                                }else {
                                    long profileSaved = profilesManager.saveProfile(
                                            callProfileAppService.getLocalProfile().getHexPublicKey(),
                                            profileInformation
                                    );

                                    // update backup if there is any
                                    if (ioPConnect.isProfileBackupScheduled(profileServiceOwner.getHexPublicKey())) {
                                        ioPConnect.backupProfile(profileServiceOwner, backupProfileFile, backupProfilePassword);
                                    }

                                    logger.info("Pairing request saved with id: " + prId + ", profiles saved " + profileSaved);
                                    if (pairingListener != null) {
                                        pairingListener.onPairReceived(callProfileAppService.getRemotePubKey(), pairingMsg.getName());
                                    } else {
                                        logger.info("pairListener null, please add it if you want to receive pairs");
                                    }
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

    public PairingListener getPairingListener() {
        return pairingListener;
    }
}
