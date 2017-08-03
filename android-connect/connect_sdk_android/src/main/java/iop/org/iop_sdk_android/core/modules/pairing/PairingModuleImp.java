package iop.org.iop_sdk_android.core.modules.pairing;

import android.content.Context;
import android.util.Log;

import org.libertaria.world.core.IoPConnect;
import org.libertaria.world.core.services.pairing.DisconnectMsg;
import org.libertaria.world.core.services.pairing.PairingMsg;
import org.libertaria.world.core.services.pairing.PairingMsgTypes;
import org.libertaria.world.crypto.CryptoBytes;
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

import iop.org.iop_sdk_android.core.base.AbstractModule;
import iop.org.iop_sdk_android.core.service.server_broker.PlatformService;

/**
 * Created by furszy on 7/20/17.
 */

public class PairingModuleImp extends AbstractModule implements PairingModule{

    private static final Logger logger = LoggerFactory.getLogger(PairingModuleImp.class);

    private PlatformService platformService;

    public PairingModuleImp(Context context, PlatformService ioPConnectService, IoPConnect ioPConnect) {
        super(context,ioPConnect,Version.newProtocolAcceptedVersion(), EnabledServices.PROFILE_PAIRING);
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
        String remotePubKeyStr = CryptoBytes.toHexString(remotePubKey);
        ProfileInformation remoteProfileInformationDb = platformService.getProfilesDb().getProfile(localProfilePubKey,remotePubKeyStr);
        if(remoteProfileInformationDb!= null && !remoteProfileInformationDb.getPairStatus().equals(ProfileInformationImp.PairStatus.DISCONNECTED)){
            if(remoteProfileInformationDb.getPairStatus() != null)
                //throw new IllegalArgumentException("Already known profile");
                listener.onMsgFail(0,0,"Already known profile");
        }
        // check if the pairing request exist
        if (platformService.getPairingRequestsDb().containsPairingRequest(localProfilePubKey,remotePubKeyStr)){
            throw new IllegalStateException("Pairing request already exist");
        }

        final Profile localProfile = ioPConnect.getProfile(localProfilePubKey);

        // now send the request
        final PairingRequest pairingRequest = PairingRequest.buildPairingRequestFromHost(
                localProfile.getHexPublicKey(),
                CryptoBytes.toHexString(remotePubKey),
                psHost,localProfile.getName(),
                localProfile.getHomeHost(),
                remoteName,
                ProfileInformationImp.PairStatus.WAITING_FOR_RESPONSE
        );

        // save request
        final int pairingRequestId = platformService.getPairingRequestsDb().savePairingRequest(pairingRequest);
        pairingRequest.setId(pairingRequestId);

        if (remoteProfileInformationDb==null){
            remoteProfileInformationDb = new ProfileInformationImp(remotePubKey,remoteName,psHost, ProfileInformationImp.PairStatus.WAITING_FOR_RESPONSE);
            platformService.getProfilesDb().saveProfile(localProfilePubKey,remoteProfileInformationDb);
            // update backup profile is there is any
            String backupProfilePath = null;
            if((backupProfilePath = platformService.getConfPref().getBackupProfilePath())!=null){
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
            platformService.getProfilesDb().updateProfile(localProfilePubKey,remoteProfileInformationDb);
        }
        PairingMsg pairingMsg = new PairingMsg(pairingRequest.getSenderName(), pairingRequest.getSenderPsHost());
        final ProfileInformation finalRemoteProfileInformationDb = remoteProfileInformationDb;
        prepareCallAndSend(localProfilePubKey,remoteProfileInformationDb,pairingMsg,new ProfSerMsgListener<Boolean>() {
            @Override
            public void onMessageReceive(int messageId, Boolean message) {
                // notify
                listener.onMessageReceive(messageId, finalRemoteProfileInformationDb);
            }

            @Override
            public void onMsgFail(int messageId, int statusValue, String details) {
                // rollback pairing request:
                logger.info("fail pairing request: "+details);
                platformService.getPairingRequestsDb().delete(pairingRequest.getId());
                listener.onMsgFail(messageId,statusValue,details);
            }

            @Override
            public String getMessageName() {
                return null;
            }
        });
    }

    @Override
    public void acceptPairingProfile(PairingRequest pairingRequest, final ProfSerMsgListener<Boolean> profSerMsgListener) throws Exception{

        // Remember that here the local device is the pairingRequest.getSender()
        final String remotePubKeyHex =  pairingRequest.getSenderPubKey();
        final String localPubKeyHex = pairingRequest.getRemotePubKey();
        logger.info("acceptPairingRequest, remote: " + remotePubKeyHex);

        prepareCall(localPubKeyHex, remotePubKeyHex, new ProfSerMsgListener<CallProfileAppService>() {
            @Override
            public void onMessageReceive(int messageId, final CallProfileAppService call) {
                try {
                    call.sendMsg(PairingMsgTypes.PAIR_ACCEPT.getType(), new ProfSerMsgListener<Boolean>() {
                        @Override
                        public void onMessageReceive(int messageId, Boolean object) {
                            logger.info("PairAccept sent");
                            if (call!=null)
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
                                    PairingMsgTypes.PAIR_ACCEPT,
                                    ProfileInformationImp.PairStatus.PAIRED
                            );

                            // notify
                            if (profSerMsgListener!=null)
                                profSerMsgListener.onMessageReceive(messageId,object);
                        }

                        @Override
                        public void onMsgFail(int messageId, int statusValue, String details) {
                            if (profSerMsgListener!=null){
                                profSerMsgListener.onMsgFail(messageId,statusValue,details);
                            }
                        }

                        @Override
                        public String getMessageName() {
                            return "Pair acceptance";
                        }
                    });
                }catch (Exception e){
                    logger.info("Error sending pair accept "+e.getMessage());
                    profSerMsgListener.onMsgFail(0,0,e.getMessage());
                }
            }

            @Override
            public void onMsgFail(int messageId, int statusValue, String details) {
                logger.info("accept pairing fail, "+details);
                profSerMsgListener.onMsgFail(messageId,statusValue,details);
            }

            @Override
            public String getMessageName() {
                return "acceptPairingRequest";
            }
        });
    }

    @Override
    public void cancelPairingRequest(PairingRequest pairingRequest) {
        platformService.getPairingRequestsDb().delete(pairingRequest.getId());
    }

    @Override
    public void disconectPairingProfile(String localProfilePubKey, ProfileInformation remoteProfile, boolean needsToBeNotified, ProfSerMsgListener<Boolean> listener) {
        platformService.getPairingRequestsDb().disconnectPairingProfile(localProfilePubKey,remoteProfile.getHexPublicKey());
        platformService.getProfilesDb().deleteProfileByPubKey(localProfilePubKey,remoteProfile.getHexPublicKey());
        listener.onMessageReceive(1,true);
        if (!needsToBeNotified) { return; }
        prepareCallAndSend(localProfilePubKey,remoteProfile,new DisconnectMsg(),null);
    }



    @Override
    public List<PairingRequest> getPairingRequests(String localProfPubKey) {
        return platformService.getPairingRequestsDb().openPairingRequests(localProfPubKey);
    }
}
