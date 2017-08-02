package iop.org.iop_sdk_android.core.modules.pairing;

import android.content.Context;

import org.libertaria.world.core.IoPConnect;
import org.libertaria.world.crypto.CryptoBytes;
import org.libertaria.world.global.Version;
import org.libertaria.world.profile_server.ProfileInformation;
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
    public void requestPairingProfile(final String localProfilePubKey, byte[] remotePubKey, String remoteName, final String psHost, final ProfSerMsgListener<ProfileInformation> listener) throws Exception {
        // check if the profile already exist
        ProfileInformation profileInformationDb = null;
        String remotePubKeyStr = CryptoBytes.toHexString(remotePubKey);
        if((profileInformationDb = platformService.getProfilesDb().getProfile(localProfilePubKey,remotePubKeyStr))!=null){
            if(profileInformationDb.getPairStatus() != null)
                throw new IllegalArgumentException("Already known profile");
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
        ioPConnect.requestPairingProfile(pairingRequest, new ProfSerMsgListener<ProfileInformation>() {
            @Override
            public void onMessageReceive(int messageId, ProfileInformation remote) {
                remote.setHomeHost(psHost);
                remote.setPairStatus(ProfileInformationImp.PairStatus.WAITING_FOR_RESPONSE);
                // Save invisible contact
                platformService.getProfilesDb().saveProfile(localProfilePubKey,remote);
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
                // notify
                listener.onMessageReceive(messageId,remote);
            }

            @Override
            public void onMsgFail(int messageId, int statusValue, String details) {
                // rollback pairing request:
                platformService.getPairingRequestsDb().delete(pairingRequest.getId());
                listener.onMsgFail(messageId,statusValue,details);
            }

            @Override
            public String getMessageName() {
                return "Request pairing";
            }
        });
    }

    @Override
    public void acceptPairingProfile(PairingRequest pairingRequest, ProfSerMsgListener<Boolean> profSerMsgListener) throws Exception{
        ioPConnect.acceptPairingRequest(pairingRequest,profSerMsgListener);
    }

    @Override
    public void cancelPairingRequest(PairingRequest pairingRequest) {
        ioPConnect.cancelPairingRequest(pairingRequest);
    }

    @Override
    public void disconectPairingProfile(String localProfilePubKey, String remoteProfilePubKey, boolean needsToBeNotified, ProfSerMsgListener<Boolean> listener) {
        platformService.getPairingRequestsDb().disconnectPairingProfile(localProfilePubKey,remoteProfilePubKey);
        platformService.getProfilesDb().deleteProfileByPubKey(localProfilePubKey,remoteProfilePubKey);
        listener.onMessageReceive(1,true);
        if (!needsToBeNotified) { return; }

    }

    @Override
    public List<PairingRequest> getPairingRequests(String localProfPubKey) {
        return platformService.getPairingRequestsDb().openPairingRequests(localProfPubKey);
    }
}
