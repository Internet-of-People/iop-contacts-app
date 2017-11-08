package org.libertaria.world.core.services.pairing;

import org.libertaria.world.profile_server.engine.app_services.MessageWrapper;
import org.libertaria.world.profile_server.imp.ProfileInformationImp;
import org.libertaria.world.profiles_manager.PairingRequest;
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

    public void setBackupProfile(String backupProfilePath, String backupProfilePassword) {
        this.backupProfileFile = new File(backupProfilePath);
        this.backupProfilePassword = backupProfilePassword;
    }


    /**
     * Wrap call in a PairingAppService call.
     *
     * @param callProfileAppService
     */
    @Override
    public void onWrapCall(final org.libertaria.world.profile_server.engine.app_services.CallProfileAppService callProfileAppService) {
        callProfileAppService.setMsgListener(new org.libertaria.world.profile_server.engine.app_services.CallProfileAppService.CallMessagesListener() {
            @Override
            public void onMessage(MessageWrapper msg) {
                try {
                    logger.info("pair msg received");

                    PairingMessageType types = PairingMessageType.getByName(msg.getMsgType());
                    switch (types) {
                        case PAIR_ACCEPT:
                            if (msg.getMsg() instanceof PairAcceptedMessage) {
                                PairAcceptedMessage pairAcceptedMessage = (PairAcceptedMessage) msg.getMsg();
                                PairingRequest pairingRequest = pairingRequestsManager.getPairingRequest(pairAcceptedMessage.getExternalRequestId());
                                pairingRequestsManager.updateStatus((int) pairingRequest.getId(), PairingMessageType.PAIR_ACCEPT, ProfileInformationImp.PairStatus.PAIRED);
                                profilesManager.updatePaired(pairingRequest.getSenderPubKey(), pairingRequest.getRemotePubKey(), ProfileInformationImp.PairStatus.PAIRED);
                                logger.info("Pairing accepted, profiles updated " + pairingRequest.getRemoteId() + ", update status: " + ProfileInformationImp.PairStatus.PAIRED);
                                if (pairingListener != null) {
                                    pairingListener.onPairResponseReceived(pairingRequest.getRemotePubKey(), "Accepted");
                                } else {
                                    logger.info("pairListener null, please add it if you want to receive pairs");
                                }
                            } else {
                                // update pair request -> todo: this should be in another place..
                                boolean updateStatus = pairingRequestsManager.updateStatus(
                                        profileServiceOwner.getHexPublicKey(),
                                        callProfileAppService.getRemotePubKey(),
                                        PairingMessageType.PAIR_ACCEPT,
                                        org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus.PAIRED);
                                boolean res = profilesManager.updatePaired(
                                        callProfileAppService.getLocalProfile().getHexPublicKey(),
                                        callProfileAppService.getRemotePubKey(),
                                        org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus.PAIRED
                                );
                                logger.info("Pairing accepted, profiles updated " + res + ", update status: " + updateStatus);
                                if (pairingListener != null) {
                                    pairingListener.onPairResponseReceived(callProfileAppService.getRemotePubKey(), "Accepted");
                                } else {
                                    logger.info("pairListener null, please add it if you want to receive pairs");
                                }
                            }
                            break;
                        case PAIR_REFUSE:
                            if (msg.getMsg() instanceof PairRefusedMessage) {
                                PairRefusedMessage pairRefusedMessage = (PairRefusedMessage) msg.getMsg();
                                PairingRequest pairingRequest = pairingRequestsManager.getPairingRequest(pairRefusedMessage.getExternalPairingId());
                                pairingRequestsManager.updateStatus((int) pairingRequest.getId(), PairingMessageType.PAIR_REFUSE, ProfileInformationImp.PairStatus.NOT_PAIRED);
                                if (pairingListener != null) {
                                    pairingListener.onPairResponseReceived(pairingRequest.getRemotePubKey(), "Refused");
                                } else {
                                    logger.info("pairListener null, please add it if you want to receive pairs");
                                }
                            } else {
                                // check if we already have the contact or already have the request
                                org.libertaria.world.profile_server.ProfileInformation profileInformationDb = profilesManager.getProfile(profileServiceOwner.getHexPublicKey(), callProfileAppService.getRemotePubKey());
                                if (profileInformationDb != null && profileInformationDb.getPairStatus() != null) {
                                    // Already known profile, shutdown the call now.
                                    logger.info("Pairing profile receive from a known profile, disposing the appService call..");
                                    callProfileAppService.dispose();
                                    return;
                                }

                                // update pair request -> todo: this should be in another place..
                                pairingRequestsManager.updateStatus(
                                        profileServiceOwner.getHexPublicKey(),
                                        callProfileAppService.getRemotePubKey(),
                                        PairingMessageType.PAIR_REFUSE,
                                        ProfileInformationImp.PairStatus.NOT_PAIRED
                                );
                                if (pairingListener != null) {
                                    pairingListener.onPairResponseReceived(callProfileAppService.getRemotePubKey(), "Refused");
                                } else {
                                    logger.info("pairListener null, please add it if you want to receive pairs");
                                }
                            }
                            break;
                        case PAIR_REQUEST:
                            if (msg.getMsg() instanceof PairRequestMessage) {
                                PairRequestMessage pairRequestMessage = (PairRequestMessage) msg.getMsg();
                                // save pair request -> todo: this should be in another place..
                                org.libertaria.world.profiles_manager.PairingRequest pairingRequest = org.libertaria.world.profiles_manager.PairingRequest.buildPairingRequest(
                                        callProfileAppService.getRemotePubKey(),
                                        profileServiceOwner.getHexPublicKey(),
                                        profileServiceOwner.getNetworkIdHex(),
                                        pairRequestMessage.getName(),
                                        pairRequestMessage.getSenderHost(),
                                        profileServiceOwner.getName(),
                                        org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus.WAITING_FOR_MY_RESPONSE
                                );
                                pairingRequest.setRemotePsHome(profileServiceOwner.getHomeHost());
                                pairingRequest.setRemoteId(pairRequestMessage.getPairingRequestId());
                                int prId = pairingRequestsManager.saveIfNotExistPairingRequest(pairingRequest);

                                org.libertaria.world.profile_server.ProfileInformation profileInformation = callProfileAppService.getRemoteProfile();
                                profileInformation.setName(pairRequestMessage.getName());
                                profileInformation.setHomeHost(pairRequestMessage.getSenderHost());
                                profileInformation.setPairStatus(org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus.WAITING_FOR_MY_RESPONSE);

                                org.libertaria.world.profile_server.ProfileInformation dbProfile = profilesManager.getProfile(
                                        callProfileAppService.getLocalProfile().getHexPublicKey(),
                                        profileInformation.getHexPublicKey()
                                );
                                if (dbProfile != null) {
                                    logger.info("Profile already exist.., checking if the local profile accept this person..");
                                    // if the profile is already a connection or if i'm waiting for his approvance
                                    if (dbProfile.getPairStatus() == org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus.PAIRED || dbProfile.getPairStatus() == org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus.WAITING_FOR_RESPONSE) {
                                        // answer with an ok paired!
                                        callProfileAppService.sendMsg(PairingMessageType.PAIR_ACCEPT.getType(), null, true); // no need of a future here..
                                    }
                                } else {
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
                                        pairingListener.onPairReceived(callProfileAppService.getRemotePubKey(), pairRequestMessage.getName());
                                    } else {
                                        logger.info("pairListener null, please add it if you want to receive pairs");
                                    }
                                }
                            }
                            break;
                        case PAIR_DISCONNECT:
                            if (msg.getMsg() instanceof PairDisconnectedMessage) {
                                logger.info("PAIR_DISCONNECT");
                                pairingRequestsManager.disconnectPairingProfile(
                                        callProfileAppService.getRemotePubKey(),
                                        profileServiceOwner.getHexPublicKey());
                                boolean wasUpdateProfile = profilesManager.updatePaired(
                                        profileServiceOwner.getHexPublicKey(),
                                        callProfileAppService.getRemotePubKey(),
                                        ProfileInformationImp.PairStatus.DISCONNECTED);
                                logger.info("UPDATESTATUS IN PROFILE {}", wasUpdateProfile);
                            }
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public org.libertaria.world.profile_server.engine.app_services.PairingListener getPairingListener() {
        return pairingListener;
    }
}
