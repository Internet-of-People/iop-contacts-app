package iop.org.iop_sdk_android.core.service.modules.imp.profile;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.fermat.redtooth.core.IoPConnect;
import org.fermat.redtooth.global.Version;
import org.fermat.redtooth.profile_server.engine.app_services.PairingListener;
import org.fermat.redtooth.profile_server.engine.futures.BaseMsgFuture;
import org.fermat.redtooth.profile_server.engine.futures.ConnectionFuture;
import org.fermat.redtooth.profile_server.engine.listeners.EngineListener;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.model.KeyEd25519;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.services.EnabledServices;
import org.fermat.redtooth.wallet.utils.Iso8601Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import iop.org.iop_sdk_android.core.IntentBroadcastConstants;
import iop.org.iop_sdk_android.core.service.IoPConnectService;
import iop.org.iop_sdk_android.core.service.modules.AbstractModule;
import org.fermat.redtooth.services.interfaces.ProfilesModule;

import iop.org.iop_sdk_android.core.service.server_broker.PlatformService;
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
    private LocalBroadcastManager localBroadcastManager;
    private IoPConnect ioPConnect;
    // This instance is just for now to start dividing things, to get and set the profile
    private PlatformService connectService;

    public ProfilesModuleImp(Context context, IoPConnect ioPConnect, PlatformService connectService) {
        super(
                context,
                Version.newProtocolAcceptedVersion(), // version 1 default for now..
                EnabledServices.PROFILE_DATA.getName() // module identifier
        );
        this.ioPConnect = ioPConnect;
        this.connectService = connectService;
        this.localBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    private PairingListener pairingListener = new PairingListener() {
        @Override
        public void onPairReceived(String requesteePubKey, final String name) {
            Intent intent = new Intent(ACTION_ON_PAIR_RECEIVED);
            intent.putExtra(INTENT_EXTRA_PROF_KEY,requesteePubKey);
            intent.putExtra(INTENT_EXTRA_PROF_NAME,name);
            localBroadcastManager.sendBroadcast(intent);
        }

        @Override
        public void onPairResponseReceived(String requesteePubKey, String responseDetail) {
            Intent intent = new Intent(ACTION_ON_RESPONSE_PAIR_RECEIVED);
            intent.putExtra(INTENT_EXTRA_PROF_KEY,requesteePubKey);
            intent.putExtra(INTENT_RESPONSE_DETAIL,responseDetail);
            localBroadcastManager.sendBroadcast(intent);
        }
    };

    @Override
    public String registerProfile(String name, String type, byte[] img, int latitude, int longitude, String extraData) throws Exception {
        if (img!=null){
            img = ImageUtils.compressJpeg(img, 20480);
        }
        Profile profile = ioPConnect.createProfile(null,name,type,img,extraData,null);
        // just for now..
        connectService.setProfile(profile);
        return profile.getHexPublicKey();
    }

    @Override
    public File backupProfile(File backupDir, String password) throws IOException {
        File backupFile = new File(
                backupDir,
                "backup_iop_connect_"+connectService.getProfile().getName()+ Iso8601Format.formatDateTimeT(new Date(System.currentTimeMillis()))+".dat"
        );
        connectService.getConfPref().saveBackupPassword(password);
        logger.info("Backup file path: "+backupFile.getAbsolutePath());
        backupOverwriteProfile(connectService.getProfile(),backupFile,password);
        scheduleBackupProfileFile(connectService.getProfile(),backupDir,password);
        return backupFile;
    }

    @Override
    public void scheduleBackupProfileFile(Profile profile, File backupDir, String password){
        File backupFile = new File(
                backupDir,
                "backup_iop_connect_"+profile.getName()+".dat"
        );
        connectService.getConfPref().setScheduleBackupEnable(true);
        connectService.getConfPref().saveBackupPatch(backupFile.getAbsolutePath());
        connectService.getConfPref().saveBackupPassword(password);
    }

    @Override
    public String registerProfile(String name, byte[] img) throws Exception {
        return registerProfile(name,"IoP-contacts",img,0,0,null);
    }

    @Override
    public void connect(final String pubKey) throws Exception {
        final ConnectionFuture msgListenerFuture = new ConnectionFuture();
        msgListenerFuture.setListener(new BaseMsgFuture.Listener<Boolean>() {
            @Override
            public void onAction(int messageId, Boolean object) {
                Profile profile = connectService.getProfile();
                profile.setHomeHost(msgListenerFuture.getProfServerData().getHost());
                profile.setHomeHostId(msgListenerFuture.getProfServerData().getNetworkId());
                onCheckInCompleted(profile.getHexPublicKey());
            }

            @Override
            public void onFail(int messageId, int status, String statusDetail) {
                Profile profile = connectService.getProfile();
                onCheckInFail(profile,status,statusDetail);
                if (status==400){
                    logger.info("Checking fail, detail "+statusDetail+", trying to reconnect after 5 seconds");
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Future future = executor.submit(reconnectRunnable);
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
                            executor.shutdownNow();
                            executor = null;
                        }
                    }
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
        ioPConnect.connectProfile(pubKey,pairingListener,null,msgListenerFuture);
    }

    @Override
    public void onCheckInCompleted(String localProfilePubKey) {
        Intent intent = new Intent(ACTION_ON_PROFILE_CONNECTED);
        localBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onDisconnect(String localProfilePubKey) {
        Intent intent = new Intent(ACTION_ON_PROFILE_DISCONNECTED);
        localBroadcastManager.sendBroadcast(intent);
    }

    private void onCheckInFail(Profile profile, int status, String statusDetail) {
        logger.warn("on check in fail: "+statusDetail);
        Intent intent = new Intent(ACTION_ON_CHECK_IN_FAIL);
        intent.putExtra(INTENT_RESPONSE_DETAIL,statusDetail);
        localBroadcastManager.sendBroadcast(intent);
    }

    public int updateProfile(String pubKey , String name, byte[] img, int latitude, int longitude, String extraData, final ProfSerMsgListener<Boolean> msgListener) throws Exception {
        try{
            logger.info("Trying to update profile..");
            Version version = connectService.getProfile().getVersion();
            Profile profile = new Profile(version,name,img,latitude,longitude,extraData);
            profile.setKey((KeyEd25519) connectService.getProfile().getKey());
            if (profile.getImg()!=null){
                connectService.getProfile().setImg(ImageUtils.compressJpeg(profile.getImg(), 20480));
            }
            if (name!=null && !connectService.getProfile().getName().equals(name)){
                profile.setName(name);
                connectService.getProfile().setName(name);
            }
            connectService.getConfPref().saveProfile(profile);
            // broadcast profile update
            broadcastUpdateProfile();

            return ioPConnect.updateProfile(profile, new ProfSerMsgListener<Boolean>() {
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

    private void broadcastUpdateProfile() {
        Intent intent = new Intent(IntentBroadcastConstants.ACTION_PROFILE_UPDATED_CONSTANT);
        localBroadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        connectService = null;
        ioPConnect = null;
    }
}
