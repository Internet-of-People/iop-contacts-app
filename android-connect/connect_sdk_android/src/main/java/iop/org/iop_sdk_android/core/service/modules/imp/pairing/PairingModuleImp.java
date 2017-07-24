package iop.org.iop_sdk_android.core.service.modules.imp.pairing;

import android.content.Context;

import org.fermat.redtooth.core.IoPConnect;
import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.global.Version;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.imp.ProfileInformationImp;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profiles_manager.PairingRequest;
import org.fermat.redtooth.services.EnabledServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import iop.org.iop_sdk_android.core.service.modules.AbstractModule;
import iop.org.iop_sdk_android.core.service.server_broker.PlatformService;

import org.fermat.redtooth.services.interfaces.PairingModule;

/**
 * Created by furszy on 7/20/17.
 */

public class PairingModuleImp extends AbstractModule implements PairingModule{

    private static final Logger logger = LoggerFactory.getLogger(PairingModuleImp.class);

    private IoPConnect ioPConnect;
    private PlatformService platformService;

    public PairingModuleImp(Context context, PlatformService ioPConnectService, IoPConnect ioPConnect) {
        super(context,Version.newProtocolAcceptedVersion(), EnabledServices.PROFILE_PAIRING.getName());
        this.ioPConnect = ioPConnect;
        this.platformService = ioPConnectService;

    }

    @Override
    public void onDestroy() {
        platformService = null;
    }

    @Override
    public void requestPairingProfile(final Profile localProfile, byte[] remotePubKey, String remoteName, final String psHost, final ProfSerMsgListener<ProfileInformation> listener) throws Exception {
        // check if the profile already exist
        ProfileInformation profileInformationDb = null;
        String remotePubKeyStr = CryptoBytes.toHexString(remotePubKey);
        if((profileInformationDb = platformService.getProfilesDb().getProfile(localProfile.getHexPublicKey(),remotePubKeyStr))!=null){
            if(profileInformationDb.getPairStatus() != null)
                throw new IllegalArgumentException("Already known profile");
        }
        // check if the pairing request exist
        if (platformService.getPairingRequestsDb().containsPairingRequest(localProfile.getHexPublicKey(),remotePubKeyStr)){
            throw new IllegalStateException("Pairing request already exist");
        }

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
                platformService.getProfilesDb().saveProfile(localProfile.getHexPublicKey(),remote);
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
}
