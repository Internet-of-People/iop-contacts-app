package iop.org.iop_sdk_android.core.modules.pairing;

import org.libertaria.world.core.IoPConnect;
import org.libertaria.world.core.services.pairing.PairAcceptedMessage;
import org.libertaria.world.core.services.pairing.PairDisconnectedMessage;
import org.libertaria.world.core.services.pairing.PairRefusedMessage;
import org.libertaria.world.core.services.pairing.PairRequestMessage;
import org.libertaria.world.core.services.pairing.PairingMessageType;
import org.libertaria.world.crypto.CryptoBytes;
import org.libertaria.world.global.AbstractModule;
import org.libertaria.world.global.SystemContext;
import org.libertaria.world.global.Version;
import org.libertaria.world.profile_server.ProfileInformation;
import org.libertaria.world.profile_server.engine.app_services.CallProfileAppService;
import org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener;
import org.libertaria.world.profile_server.imp.ProfileInformationImp;
import org.libertaria.world.profile_server.model.Profile;
import org.libertaria.world.profiles_manager.PairingRequest;
import org.libertaria.world.services.EnabledServices;
import org.libertaria.world.services.interfaces.PairingModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import iop.org.iop_sdk_android.core.service.server_broker.PlatformService;

/**
 * Created by furszy on 7/20/17.
 */

public class PairingModuleImp extends AbstractModule implements PairingModule {

    private static final Logger logger = LoggerFactory.getLogger(PairingModuleImp.class);

    private PlatformService platformService;

    public PairingModuleImp(SystemContext context, PlatformService ioPConnectService, IoPConnect ioPConnect) {
        super(context, ioPConnect, Version.newProtocolAcceptedVersion(), EnabledServices.PROFILE_PAIRING);
        this.platformService = ioPConnectService;

    }

    @Override
    public void onDestroy() {
        platformService = null;
    }

    @Override
    public void requestPairingProfile(
            final String localProfilePubKey,
            byte[] remotePubKey,
            String remoteName,
            final String psHost,
            final ProfSerMsgListener<ProfileInformation> listener) throws Exception {
        // check if the profile already exist
        //ProfileInformation remoteProfileInformationDb = null;
        final String remotePubKeyStr = CryptoBytes.toHexString(remotePubKey);
        ProfileInformation remoteProfileInformationDb = platformService.getProfilesDb().getProfile(localProfilePubKey, remotePubKeyStr);
        if (remoteProfileInformationDb != null && !remoteProfileInformationDb.getPairStatus().equals(ProfileInformationImp.PairStatus.DISCONNECTED)) {
            if (remoteProfileInformationDb.getPairStatus() != null)
                //throw new IllegalArgumentException("Already known profile");
                listener.onMsgFail(0, 0, "Already known profile");
            return;
        }
        // check if the pairing request exist
        if (platformService.getPairingRequestsDb().containsPairingRequest(localProfilePubKey, remotePubKeyStr)) {
            throw new IllegalStateException("Pairing request already exist");
        }

        final Profile localProfile = ioPConnect.getProfile(localProfilePubKey);

        // now send the request
        final PairingRequest pairingRequest = PairingRequest.buildPairingRequestFromHost(
                localProfile.getHexPublicKey(),
                CryptoBytes.toHexString(remotePubKey),
                psHost, localProfile.getName(),
                localProfile.getHomeHost(),
                remoteName,
                ProfileInformationImp.PairStatus.WAITING_FOR_RESPONSE
        );

        // save request
        final int pairingRequestId = platformService.getPairingRequestsDb().savePairingRequest(pairingRequest);
        pairingRequest.setId(pairingRequestId);

        if (remoteProfileInformationDb == null) {
            remoteProfileInformationDb = new ProfileInformationImp(remotePubKey, remoteName, psHost, ProfileInformationImp.PairStatus.WAITING_FOR_RESPONSE);
            platformService.getProfilesDb().saveProfile(localProfilePubKey, remoteProfileInformationDb);
            // update backup profile is there is any
            String backupProfilePath = null;
            if ((backupProfilePath = platformService.getConfPref().getBackupProfilePath()) != null) {
                try {
                    platformService.getProfileModule().backupOverwriteProfile(
                            localProfile,
                            new File(backupProfilePath),
                            platformService.getConfPref().getBackupPassword()
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.warn("Backup profile fail.");
                }
            }
        } else {
            remoteProfileInformationDb.setPairStatus(ProfileInformationImp.PairStatus.WAITING_FOR_RESPONSE);
            platformService.getProfilesDb().updateProfile(localProfilePubKey, remoteProfileInformationDb);
        }
        PairRequestMessage pairRequestMessage = new PairRequestMessage(pairingRequest.getSenderName(), pairingRequest.getSenderPsHost(), pairingRequestId);
        final ProfileInformation finalRemoteProfileInformationDb = remoteProfileInformationDb;
        prepareCallAndSend(localProfilePubKey, remoteProfileInformationDb, pairRequestMessage, new ProfSerMsgListener<Boolean>() {
            @Override
            public void onMessageReceive(int messageId, Boolean message) {
                // notify
                listener.onMessageReceive(messageId, finalRemoteProfileInformationDb);
            }

            @Override
            public void onMsgFail(int messageId, int statusValue, String details) {
                // rollback pairing request:
//                logger.info("fail pairing request: " + details);
//                platformService.getProfilesDb().deleteProfileByPubKey(localProfilePubKey, remotePubKeyStr);
//                platformService.getPairingRequestsDb().delete(pairingRequest.getId());
                listener.onMsgFail(messageId, statusValue, details);
            }

            @Override
            public String getMessageName() {
                return null;
            }
        }, true);
    }

    @Override
    public void acceptPairingProfile(PairingRequest pairingRequest, final ProfSerMsgListener<Boolean> profSerMsgListener) throws Exception {

        // Remember that here the local device is the pairingRequest.getSender()
        final String remotePubKeyHex = pairingRequest.getSenderPubKey();
        final String localPubKeyHex = pairingRequest.getRemotePubKey();
        logger.info("acceptPairingRequest, remote: " + remotePubKeyHex);
        final PairAcceptedMessage pairAcceptedMessage = new PairAcceptedMessage(pairingRequest.getRemoteId());
        prepareCall(localPubKeyHex, remotePubKeyHex, new ProfSerMsgListener<CallProfileAppService>() {
            @Override
            public void onMessageReceive(int messageId, final CallProfileAppService call) {
                try {
                    call.sendMsg(pairAcceptedMessage, new ProfSerMsgListener<Boolean>() {
                        @Override
                        public void onMessageReceive(int messageId, Boolean object) {
                            logger.info("PairAccept sent");
                            if (call != null)
                                call.dispose();
                            else
                                logger.warn("call null trying to dispose pairing app service. Check this");

                            // update in db the acceptance
                            platformService.getProfilesDb().updatePaired(
                                    localPubKeyHex,
                                    remotePubKeyHex,
                                    ProfileInformationImp.PairStatus.PAIRED);
                            platformService.getPairingRequestsDb().updateStatus(
                                    remotePubKeyHex,
                                    localPubKeyHex,
                                    PairingMessageType.PAIR_ACCEPT,
                                    ProfileInformationImp.PairStatus.PAIRED
                            );

                            // notify
                            if (profSerMsgListener != null)
                                profSerMsgListener.onMessageReceive(messageId, object);
                        }

                        @Override
                        public void onMsgFail(int messageId, int statusValue, String details) {
                            if (profSerMsgListener != null) {
                                profSerMsgListener.onMsgFail(messageId, statusValue, details);
                            }
                        }

                        @Override
                        public String getMessageName() {
                            return "Pair acceptance";
                        }
                    }, true);
                } catch (Exception e) {
                    logger.info("Error sending pair accept " + e.getMessage());
                    profSerMsgListener.onMsgFail(0, 0, e.getMessage());
                }
            }

            @Override
            public void onMsgFail(int messageId, int statusValue, String details) {
                logger.info("accept pairing fail, " + details);
                profSerMsgListener.onMsgFail(messageId, statusValue, details);
            }

            @Override
            public String getMessageName() {
                return "acceptPairingRequest";
            }
        }, true);
    }

    @Override
    public PairingRequest getPairingRequest(int pairingRequestId) {
        return platformService.getPairingRequestsDb().getPairingRequest(pairingRequestId);
    }

    @Override
    public void cancelPairingRequest(PairingRequest pairingRequest, Boolean notify) {
        platformService.getPairingRequestsDb().delete(pairingRequest.getId());
        platformService.getProfilesDb().deleteProfileByPubKey(pairingRequest.getSenderPubKey(), pairingRequest.getRemotePubKey());
        platformService.getProfilesDb().deleteProfileByPubKey(pairingRequest.getRemotePubKey(), pairingRequest.getSenderPubKey());
        if (notify) {
            final String remotePubKeyHex = pairingRequest.getSenderPubKey();
            final String localPubKeyHex = pairingRequest.getRemotePubKey();
            logger.info("cancelPairingRequest, remote: " + remotePubKeyHex);
            final PairRefusedMessage pairRefuseMessage = new PairRefusedMessage(pairingRequest.getRemoteId());
            prepareCallAndSend(localPubKeyHex, remotePubKeyHex, pairRefuseMessage, null, false);
        }
    }

    @Override
    public void disconnectPairingProfile(String localProfilePubKey, ProfileInformation remoteProfile, boolean needsToBeNotified, ProfSerMsgListener<Boolean> listener) {
        platformService.getPairingRequestsDb().disconnectPairingProfile(localProfilePubKey, remoteProfile.getHexPublicKey());
        platformService.getProfilesDb().deleteProfileByPubKey(localProfilePubKey, remoteProfile.getHexPublicKey());
        listener.onMessageReceive(1, true);
        if (!needsToBeNotified) {
            return;
        }
        prepareCallAndSend(localProfilePubKey, remoteProfile, new PairDisconnectedMessage(), null);
    }


    @Override
    public List<PairingRequest> getPairingRequests(String localProfPubKey) {
        return platformService.getPairingRequestsDb().openPairingRequests(localProfPubKey);
    }
}
