package org.libertaria.world.core.services.pairing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by furszy on 6/8/17.
 */

public class PairingAppService extends org.libertaria.world.profile_server.engine.app_services.AppService {

    private static final Logger logger = LoggerFactory.getLogger(PairingAppService.class);

    private org.libertaria.world.profile_server.model.Profile profileServiceOwner;
    private org.libertaria.world.profile_server.engine.app_services.PairingListener pairingListener;
    private org.libertaria.world.profiles_manager.PairingRequestsManager pairingRequestsManager;
    private org.libertaria.world.profiles_manager.ProfilesManager profilesManager;
    private org.libertaria.world.core.IoPConnect ioPConnect;

    private File backupProfileFile;
    private String backupProfilePassword;

    public PairingAppService(org.libertaria.world.profile_server.model.Profile profileServiceOwner, org.libertaria.world.profiles_manager.PairingRequestsManager pairingRequestsManager, org.libertaria.world.profiles_manager.ProfilesManager profilesManager, org.libertaria.world.profile_server.engine.app_services.PairingListener pairingListener, org.libertaria.world.core.IoPConnect ioPConnect) {
        super(org.libertaria.world.services.EnabledServices.PROFILE_PAIRING.getName());
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
    public void onWrapCall(final org.libertaria.world.profile_server.engine.app_services.CallProfileAppService callProfileAppService) {
        if (pairingListener!=null){
            callProfileAppService.setMsgListener(new org.libertaria.world.profile_server.engine.app_services.CallProfileAppService.CallMessagesListener() {
                @Override
                public void onMessage(org.libertaria.world.profile_server.engine.app_services.MsgWrapper msg) {
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
                                        org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus.PAIRED);
                                boolean res = profilesManager.updatePaired(
                                        callProfileAppService.getLocalProfile().getHexPublicKey(),
                                        callProfileAppService.getRemotePubKey(),
                                        org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus.PAIRED
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
                                org.libertaria.world.profile_server.ProfileInformation profileInformationDb = profilesManager.getProfile(profileServiceOwner.getHexPublicKey(),callProfileAppService.getRemotePubKey());
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
                                        org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus.PAIRED
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
                                org.libertaria.world.profiles_manager.PairingRequest pairingRequest = org.libertaria.world.profiles_manager.PairingRequest.buildPairingRequest(
                                        callProfileAppService.getRemotePubKey(),
                                        profileServiceOwner.getHexPublicKey(),
                                        profileServiceOwner.getNetworkIdHex(),
                                        pairingMsg.getName(),
                                        pairingMsg.getSenderHost(),
                                        profileServiceOwner.getName(),
                                        org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus.WAITING_FOR_MY_RESPONSE
                                );
                                pairingRequest.setRemotePsHome(profileServiceOwner.getHomeHost());
                                int prId = pairingRequestsManager.saveIfNotExistPairingRequest(pairingRequest);

                                org.libertaria.world.profile_server.ProfileInformation profileInformation = callProfileAppService.getRemoteProfile();
                                profileInformation.setName(pairingMsg.getName());
                                profileInformation.setHomeHost(pairingMsg.getSenderHost());
                                profileInformation.setPairStatus(org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus.WAITING_FOR_MY_RESPONSE);

                                org.libertaria.world.profile_server.ProfileInformation dbProfile = profilesManager.getProfile(
                                        callProfileAppService.getLocalProfile().getHexPublicKey(),
                                        profileInformation.getHexPublicKey()
                                );
                                if (dbProfile!=null){
                                    logger.info("Profile already exist.., checking if the local profile accept this person..");
                                    // if the profile is already a connection or if i'm waiting for his approvance
                                    if(dbProfile.getPairStatus() == org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus.PAIRED || dbProfile.getPairStatus() == org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus.WAITING_FOR_RESPONSE){
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

    public org.libertaria.world.profile_server.engine.app_services.PairingListener getPairingListener() {
        return pairingListener;
    }
}
