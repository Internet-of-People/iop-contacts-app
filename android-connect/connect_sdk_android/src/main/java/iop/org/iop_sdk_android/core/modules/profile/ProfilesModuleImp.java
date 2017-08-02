package iop.org.iop_sdk_android.core.modules.profile;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.libertaria.world.core.IoPConnect;
import org.libertaria.world.global.PlatformSerializer;
import org.libertaria.world.global.Version;
import org.libertaria.world.profile_server.CantConnectException;
import org.libertaria.world.profile_server.CantSendMessageException;
import org.libertaria.world.profile_server.ProfileInformation;
import org.libertaria.world.profile_server.ProfileServerConfigurations;
import org.libertaria.world.profile_server.engine.app_services.AppService;
import org.libertaria.world.profile_server.engine.app_services.PairingListener;
import org.libertaria.world.profile_server.engine.futures.BaseMsgFuture;
import org.libertaria.world.profile_server.engine.futures.ConnectionFuture;
import org.libertaria.world.profile_server.engine.futures.MsgListenerFuture;
import org.libertaria.world.profile_server.engine.listeners.EngineListener;
import org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener;
import org.libertaria.world.profile_server.imp.ProfileInformationImp;
import org.libertaria.world.profile_server.model.KeyEd25519;
import org.libertaria.world.profile_server.model.Profile;
import org.libertaria.world.services.EnabledServices;
import org.libertaria.world.services.ServiceFactory;
import org.libertaria.world.services.interfaces.ProfilesModule;
import org.libertaria.world.wallet.utils.Iso8601Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import iop.org.iop_sdk_android.core.IntentBroadcastConstants;
import iop.org.iop_sdk_android.core.base.AbstractModule;
import iop.org.iop_sdk_android.core.utils.ImageUtils;

import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_ON_CHECK_IN_FAIL;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_ON_PAIR_RECEIVED;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_ON_PROFILE_CONNECTED;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_ON_PROFILE_DISCONNECTED;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_ON_RESPONSE_PAIR_RECEIVED;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.INTENT_EXTRA_PROF_KEY;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.INTENT_EXTRA_PROF_NAME;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.INTENT_RESPONSE_DETAIL;

/**
 * Created by furszy on 7/19/17.
 */

public class ProfilesModuleImp extends AbstractModule implements ProfilesModule,EngineListener{

    private static final Logger logger = LoggerFactory.getLogger(ProfilesModuleImp.class);

    // todo: change this for the non local broadcast..
    private ProfileServerConfigurations confPref;
    private ServiceFactory serviceFactory;
    // This instance is just for now to start dividing things, to get and set the profile
    //private PlatformService connectService;

    private PlatformSerializer platformSerializer = new PlatformSerializer(){
        @Override
        public KeyEd25519 toPlatformKey(byte[] privKey, byte[] pubKey) {
            return iop.org.iop_sdk_android.core.crypto.KeyEd25519.wrap(privKey,pubKey);
        }
    };

    public ProfilesModuleImp(Context context, IoPConnect ioPConnect,ServiceFactory serviceFactory ,ProfileServerConfigurations confPref) {
        super(
                context,
                ioPConnect,
                Version.newProtocolAcceptedVersion(), // version 1 default for now..
                EnabledServices.PROFILE_DATA // module identifier
        );
        this.confPref = confPref;
        this.serviceFactory = serviceFactory;
        //this.connectService = connectService;
    }

    private PairingListener pairingListener = new PairingListener() {
        @Override
        public void onPairReceived(String requesteePubKey, final String name) {
            Intent intent = new Intent(ACTION_ON_PAIR_RECEIVED);
            intent.putExtra(INTENT_EXTRA_PROF_KEY,requesteePubKey);
            intent.putExtra(INTENT_EXTRA_PROF_NAME,name);
            sendBroadcast(intent);
        }

        @Override
        public void onPairResponseReceived(String requesteePubKey, String responseDetail) {
            Intent intent = new Intent(ACTION_ON_RESPONSE_PAIR_RECEIVED);
            intent.putExtra(INTENT_EXTRA_PROF_KEY,requesteePubKey);
            intent.putExtra(INTENT_RESPONSE_DETAIL,responseDetail);
            sendBroadcast(intent);
        }
    };

    @Override
    public String registerProfile(String name, String type, byte[] img, int latitude, int longitude, String extraData) throws Exception {
        if (img!=null){
            img = ImageUtils.compressJpeg(img, 20480);
        }
        Profile profile = ioPConnect.createProfile(null,name,type,img,extraData,null);
        // just for now..
        //connectService.setProfile(profile);
        return profile.getHexPublicKey();
    }

    @Override
    public File backupProfile(String localProfPubKey,File backupDir, String password) throws IOException {
        Profile localProfile = ioPConnect.getProfile(localProfPubKey);
        File backupFile = new File(
                backupDir,
                "backup_iop_connect_"+localProfile.getName()+ Iso8601Format.formatDateTimeT(new Date(System.currentTimeMillis()))+".dat"
        );
        confPref.saveBackupPassword(password);
        logger.info("Backup file path: "+backupFile.getAbsolutePath());
        backupOverwriteProfile(localProfile,backupFile,password);
        scheduleBackupProfileFile(localProfile,backupDir,password);
        return backupFile;
    }

    @Override
    public void scheduleBackupProfileFile(Profile profile, File backupDir, String password){
        File backupFile = new File(
                backupDir,
                "backup_iop_connect_"+profile.getName()+".dat"
        );
        confPref.setScheduleBackupEnable(true);
        confPref.saveBackupPatch(backupFile.getAbsolutePath());
        confPref.saveBackupPassword(password);
    }

    @Override
    public String registerProfile(String name, byte[] img) throws Exception {
        return registerProfile(name,"IoP-contacts",img,0,0,null);
    }

    public String registerProfile(Profile profile){
        profile = ioPConnect.createProfile(profile);
        //connectService.setProfile(profile);
        return profile.getHexPublicKey();
    }

    @Override
    public void connect(final String pubKey) throws Exception {
        final ConnectionFuture msgListenerFuture = new ConnectionFuture();
        msgListenerFuture.setListener(new BaseMsgFuture.Listener<Boolean>() {
            @Override
            public void onAction(int messageId, Boolean object) {
                Profile profile = ioPConnect.getProfile(pubKey);
                profile.setHomeHost(msgListenerFuture.getProfServerData().getHost());
                profile.setHomeHostId(msgListenerFuture.getProfServerData().getNetworkId());
                ioPConnect.updateProfile(profile,false,null);
                onCheckInCompleted(profile.getHexPublicKey());
            }

            @Override
            public void onFail(int messageId, int status, String statusDetail) {
                Profile profile = ioPConnect.getProfile(pubKey);
                onCheckInFail(profile,status,statusDetail);
                if (status==400){
                    logger.info("Checking fail, detail "+statusDetail+", trying to reconnect after 5 seconds");
                    /*ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                    Future future = executor.schedule(reconnectRunnable,15,TimeUnit.SECONDS);
                    try {
                        future.get(15,TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                    }finally {
                        if (executor!=null){
                            executor.shutdown();
                            executor = null;
                        }
                    }*/
                }

            }
            Callable reconnectRunnable = new Callable() {
                @Override
                public Object call() {
                    try {
                        connect(pubKey);
                    } catch (Exception e) {
                        e.printStackTrace();
                        // connection fail
                    }
                    return null;
                }
            };

        });
        ioPConnect.connectProfile(pubKey,null,msgListenerFuture);
    }

    @Override
    public AppService appServiceInitializer(String appServiceName) {
        return serviceFactory.buildOrGetService(appServiceName);
    }

    @Override
    public PairingListener initializePairing() {
        return pairingListener;
    }

    @Override
    public void onCheckInCompleted(String localProfilePubKey) {
        Intent intent = new Intent(ACTION_ON_PROFILE_CONNECTED);
        intent.putExtra(INTENT_EXTRA_PROF_KEY,localProfilePubKey);
        sendBroadcast(intent);
    }

    @Override
    public void onDisconnect(String localProfilePubKey) {
        Intent intent = new Intent(ACTION_ON_PROFILE_DISCONNECTED);
        intent.putExtra(INTENT_EXTRA_PROF_KEY,localProfilePubKey);
        sendBroadcast(intent);
    }

    public void onCheckInFail(Profile profile, int status, String statusDetail) {
        logger.warn("on check in fail: "+statusDetail);
        Intent intent = new Intent(ACTION_ON_CHECK_IN_FAIL);
        intent.putExtra(INTENT_RESPONSE_DETAIL,statusDetail);
        sendBroadcast(intent);
    }

    @Override
    public void updateProfile(String localProfPubKey,String name, byte[] profImgData, MsgListenerFuture<Boolean> listenerFuture) throws Exception {
        logger.info("Trying to update profile..");
        updateProfile(localProfPubKey,name,profImgData,0,0,null,listenerFuture);
    }

    public void updateProfile(String pubKey , String name, byte[] img, int latitude, int longitude, String extraData, final ProfSerMsgListener<Boolean> msgListener) throws Exception {
        try{
            logger.info("Trying to update profile..");

            if (img.length>20480){
                img = ImageUtils.compressJpeg(img, 20480);
            }

            ioPConnect.updateProfile(
                    pubKey,
                    name,
                    img,
                    latitude,
                    longitude,
                    extraData,
                    new ProfSerMsgListener<Boolean>() {
                @Override
                public void onMessageReceive(int messageId, Boolean message) {
                    msgListener.onMessageReceive(messageId,message);
                }

                @Override
                public void onMsgFail(int messageId, int statusValue, String details) {
                    if (details.equals("profile.version")){
                        // add version correction
                        msgListener.onMsgFail(messageId,statusValue,details);
                    }else {
                        msgListener.onMsgFail(messageId, statusValue, details);
                    }
                }

                @Override
                public String getMessageName() {
                    return "updateProfile";
                }
            });

            // broadcast profile update
            broadcastUpdateProfile();

        }catch (Exception e){
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * todo: this is bad.. i need to do it synchronized.
     * @param backupFile
     * @param password
     * @return
     * @throws IOException
     */
    public File backupOverwriteProfile(Profile localProfile,File backupFile, String password) throws IOException {
        logger.info("Backup file path: "+backupFile.getAbsolutePath());
        ioPConnect.backupProfile(localProfile,backupFile,password);
        return backupFile;
    }

    /**
     * // todo: improve this doing the LocalProfiles db.
     * @param profilePubKey
     * @param serviceName
     */
    @Override
    public void addService(String profilePubKey,String serviceName) {
        logger.info("addService: "+serviceName);
        AppService appService = serviceFactory.buildOrGetService(serviceName);
        ioPConnect.addService(profilePubKey,appService);
    }

    @Override
    public boolean isProfileConnectedOrConnecting(String localProfilePubKey) {
        return ioPConnect.isProfileConnectedOrConnecting(localProfilePubKey);
    }

    /**
     * Return profiles that this profile is paired or it's waiting for some pair answer from the other side.
     * @return
     */
    @Override
    public List<ProfileInformation> getKnownProfiles(String localProfilePubKey) {
        List<ProfileInformation> ret = new ArrayList<>();
        List<ProfileInformation> knownProfiles = ioPConnect.getKnownProfiles(localProfilePubKey);
        // todo: this is a lazy remove..
        for (ProfileInformation knownProfile : knownProfiles) {
            if (!knownProfile.getHexPublicKey().equals(localProfilePubKey)){
                ret.add(knownProfile);
            }
        }
        return ret;
    }

    @Override
    public ProfileInformation getKnownProfile(String localProfilePubKey,String remotePk) {
        return ioPConnect.getKnownProfile(localProfilePubKey,remotePk);
    }

    @Override
    public boolean isProfileRegistered(String localProfilePubKey) {
        return confPref.isRegisteredInServer();
    }

    @Override
    public ProfileInformation getProfile(String localProfilePubKey) {
        return getMyProfile(localProfilePubKey);
    }

    @Override
    public boolean isIdentityCreated(String localProfilePubKey) {
        return confPref.isIdentityCreated();
    }

    public ProfileInformation getMyProfile(String localProfilePubKey) {
        Set<String> services = new HashSet<>();
        Profile profile = ioPConnect.getProfile(localProfilePubKey);
        if (profile.getApplicationServices()!=null) {
            for (AppService appService : profile.getApplicationServices().values()) {
                services.add(appService.getName());
            }
        }
        return new ProfileInformationImp(
                profile.getVersion(),
                profile.getPublicKey(),
                profile.getName(),
                profile.getType(),
                profile.getImg(),
                profile.getThumbnailImg(),
                profile.getLatitude(),
                profile.getLongitude(),
                profile.getExtraData(),
                services,
                profile.getNetworkId(),
                profile.getHomeHost()

        );
    }

    @Override
    public void getProfileInformation(String localProfilePubKey,String profPubKey, ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException {
        getProfileInformation(localProfilePubKey,profPubKey,false,profileFuture);
    }

    @Override
    public void getProfileInformation(String localProfilePubKey,String remoteProfPubKey, boolean getInfo, final ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException {
        ioPConnect.searchAndGetProfile(localProfilePubKey,remoteProfPubKey,getInfo,profileFuture);
    }

    @Override
    public void restoreProfileFrom(File file, String password) {
        logger.info("Restoring profile");
        if (file.exists()){
            try {
                IoPConnect.ProfileRestored profileRestored = ioPConnect.restoreFromBackup(file, password, platformSerializer);
                Profile profile = profileRestored.getProfile();
                //connectService.setProfile(profile);
                // clean everything
                ioPConnect.stop();
                profile = profileRestored.getProfile();
                // connect
                registerProfile(profile);
                connect(profile.getHexPublicKey());
                logger.info("restore profile completed");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else
            throw new IllegalArgumentException("File not exist, "+file.getAbsolutePath());
    }

    private void broadcastUpdateProfile() {
        Intent intent = new Intent(IntentBroadcastConstants.ACTION_PROFILE_UPDATED_CONSTANT);
        sendBroadcast(intent);
    }

    public PairingListener getPairingListener() {
        return pairingListener;
    }

    @Override
    public void onDestroy() {
        //connectService = null;
        ioPConnect = null;
    }
}
