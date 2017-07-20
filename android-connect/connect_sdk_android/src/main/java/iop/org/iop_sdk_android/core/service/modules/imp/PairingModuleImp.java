package iop.org.iop_sdk_android.core.service.modules.imp;

import org.fermat.redtooth.core.IoPConnect;
import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.global.Version;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.imp.ProfileInformationImp;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profiles_manager.PairingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import iop.org.iop_sdk_android.core.service.IoPConnectService;
import iop.org.iop_sdk_android.core.service.modules.AbstractModule;
import iop.org.iop_sdk_android.core.service.modules.ModuleId;
import iop.org.iop_sdk_android.core.service.modules.interfaces.PairingModule;

/**
 * Created by furszy on 7/20/17.
 */

public class PairingModuleImp extends AbstractModule implements PairingModule{

    private static final Logger logger = LoggerFactory.getLogger(PairingModuleImp.class);

    private IoPConnect ioPConnect;
    private IoPConnectService ioPConnectService;

    public PairingModuleImp(IoPConnectService ioPConnectService,IoPConnect ioPConnect) {
        super(Version.newProtocolAcceptedVersion(), ModuleId.PAIRING.getId());
        this.ioPConnect = ioPConnect;
        this.ioPConnectService = ioPConnectService;

    }

    @Override
    public void onDestroy() {
        ioPConnectService = null;
    }

    @Override
    public void requestPairingProfile(final Profile localProfile, byte[] remotePubKey, String remoteName, final String psHost, final ProfSerMsgListener<ProfileInformation> listener) throws Exception {
        // check if the profile already exist
        ProfileInformation profileInformationDb = null;
        String remotePubKeyStr = CryptoBytes.toHexString(remotePubKey);
        if((profileInformationDb = ioPConnectService.getProfilesDb().getProfile(localProfile.getHexPublicKey(),remotePubKeyStr))!=null){
            if(profileInformationDb.getPairStatus() != null)
                throw new IllegalArgumentException("Already known profile");
        }
        // check if the pairing request exist
        if (ioPConnectService.getPairingRequestsDb().containsPairingRequest(localProfile.getHexPublicKey(),remotePubKeyStr)){
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
                ioPConnectService.getProfilesDb().saveProfile(localProfile.getHexPublicKey(),remote);
                // update backup profile is there is any
                String backupProfilePath = null;
                if((backupProfilePath = ioPConnectService.getConfPref().getBackupProfilePath())!=null){
                    try {
                        ioPConnectService.backupOverwriteProfile(
                                new File(backupProfilePath),
                                ioPConnectService.getConfPref().getBackupPassword()
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
                ioPConnectService.getPairingRequestsDb().delete(pairingRequest.getId());
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
