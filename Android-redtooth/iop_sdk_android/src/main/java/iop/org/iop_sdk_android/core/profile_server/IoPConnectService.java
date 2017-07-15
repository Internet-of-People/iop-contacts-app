package iop.org.iop_sdk_android.core.profile_server;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.fermat.redtooth.core.IoPConnect;
import org.fermat.redtooth.core.IoPConnectContext;
import org.fermat.redtooth.global.PlatformSerializer;
import org.fermat.redtooth.global.Version;
import org.fermat.redtooth.profile_server.client.AppServiceCallNotAvailableException;
import org.fermat.redtooth.profile_server.engine.app_services.AppService;
import org.fermat.redtooth.profile_server.engine.app_services.CallProfileAppService;
import org.fermat.redtooth.services.EnabledServices;
import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.global.DeviceLocation;
import org.fermat.redtooth.global.GpsLocation;
import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.ProfileServerConfigurations;
import org.fermat.redtooth.profile_server.Signer;
import org.fermat.redtooth.profile_server.engine.futures.BaseMsgFuture;
import org.fermat.redtooth.profile_server.engine.futures.ConnectionFuture;
import org.fermat.redtooth.profile_server.engine.listeners.EngineListener;
import org.fermat.redtooth.profile_server.engine.SearchProfilesQuery;
import org.fermat.redtooth.profile_server.engine.futures.SearchMessageFuture;
import org.fermat.redtooth.profile_server.engine.futures.SubsequentSearchMsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.app_services.PairingListener;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.imp.ProfileInformationImp;
import org.fermat.redtooth.profile_server.model.KeyEd25519;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;
import org.fermat.redtooth.profiles_manager.PairingRequest;
import org.fermat.redtooth.services.EnabledServicesFactory;
import org.fermat.redtooth.services.chat.ChatAcceptMsg;
import org.fermat.redtooth.services.chat.ChatAppService;
import org.fermat.redtooth.services.chat.ChatCallAlreadyOpenException;
import org.fermat.redtooth.services.chat.ChatMsg;
import org.fermat.redtooth.services.chat.RequestChatException;
import org.fermat.redtooth.wallet.utils.BlockchainState;
import org.fermat.redtooth.wallet.utils.Iso8601Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import iop.org.iop_sdk_android.core.IntentBroadcastConstants;
import iop.org.iop_sdk_android.core.crypto.CryptoWrapperAndroid;
import iop.org.iop_sdk_android.core.db.SqlitePairingRequestDb;
import iop.org.iop_sdk_android.core.db.SqliteProfilesDb;
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
 * Created by mati on 09/11/16.
 */

public class IoPConnectService extends Service implements ModuleRedtooth, EngineListener,DeviceLocation {

    private final Logger logger = LoggerFactory.getLogger(IoPConnectService.class);

    private static final String TAG = "IoPConnectService";

    private static final String ACTION_SCHEDULE_SERVICE = "schedule_service";
    public static final String ACTION_BOOT_SERVICE = "boot_service";

    private LocalBroadcastManager localBroadcastManager;

    private Handler handler = new Handler();
    private ExecutorService executor;
    /** Context */
    private IoPConnectContext application;
    /** Main library */
    private IoPConnect ioPConnect;
    /** Configurations impl */
    private ProfileServerConfigurations configurationsPreferences;
    private Profile profile;
    /** Databases */
    private SqlitePairingRequestDb pairingRequestDb;
    private SqliteProfilesDb profilesDb;

    private AtomicBoolean isInitialized = new AtomicBoolean(false);

    private final Set<BlockchainState.Impediment> impediments = EnumSet.noneOf(BlockchainState.Impediment.class);

    private PlatformSerializer platformSerializer = new PlatformSerializer(){
        @Override
        public KeyEd25519 toPlatformKey(byte[] privKey, byte[] pubKey) {
            return iop.org.iop_sdk_android.core.crypto.KeyEd25519.wrap(privKey,pubKey);
        }
    };

    private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                final NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                final boolean hasConnectivity = networkInfo.isConnected();
                logger.info("network is {}, state {}/{}", hasConnectivity ? "up" : "down", networkInfo.getState(), networkInfo.getDetailedState());
                if (hasConnectivity)
                    impediments.remove(BlockchainState.Impediment.NETWORK);
                else {
                    impediments.add(BlockchainState.Impediment.NETWORK);
                }
                check();
            } else if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(action)) {
                logger.info("device storage low");
                impediments.add(BlockchainState.Impediment.STORAGE);
                // todo: control this on the future
                //check();
            } else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(action)) {
                logger.info("device storage ok");
                impediments.remove(BlockchainState.Impediment.STORAGE);
                // todo: control this on the future
                //check();
            }
        }
    };

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

    public class ProfServerBinder extends Binder {
        public IoPConnectService getService() {
            return IoPConnectService.this;
        }
    }

    private final IBinder mBinder = new IoPConnectService.ProfServerBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,".onBind()");
        return mBinder;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate");
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_LOW);
        intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
        registerReceiver(connectivityReceiver, intentFilter); // implicitly init PeerGroup
        initService();
    }

    private void initService(){
        try {
            if (isInitialized.compareAndSet(false, true)) {
                localBroadcastManager = LocalBroadcastManager.getInstance(this);
                application = (IoPConnectContext) getApplication();
                executor = Executors.newFixedThreadPool(3);
                configurationsPreferences = new ProfileServerConfigurationsImp(this, getSharedPreferences(ProfileServerConfigurationsImp.PREFS_NAME, 0));
                KeyEd25519 keyEd25519 = (KeyEd25519) configurationsPreferences.getUserKeys();
                if (keyEd25519 != null)
                    profile = configurationsPreferences.getProfile();
                pairingRequestDb = new SqlitePairingRequestDb(this);
                profilesDb = new SqliteProfilesDb(this);
                ioPConnect = new IoPConnect(application,new CryptoWrapperAndroid(),new SslContextFactory(this),profilesDb,pairingRequestDb,this);
                ioPConnect.setEngineListener(this);
                /*executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        init();
                    }
                });*/
                tryScheduleService();
            }
        }catch (Exception e){
            e.printStackTrace();
            isInitialized.set(false);
        }
    }

    /*private void init(){
        try {

        }catch (Exception e){
            e.printStackTrace();
        }
    }*/

    /**
     * Try to solve some impediment if there is any.
     */
    private void check(){
        try {
            if (configurationsPreferences.getBackgroundServiceEnable()) {
                if (impediments.contains(BlockchainState.Impediment.NETWORK)) {
                    // network unnavailable, check if i have to clean something here
                    Intent intent = new Intent(ACTION_ON_PROFILE_DISCONNECTED);
                    localBroadcastManager.sendBroadcast(intent);
                    return;
                }
                if (profile != null) {
                    if (!ioPConnect.isProfileConnectedOrConnecting(profile.getHexPublicKey())) {
                        connect(profile.getHexPublicKey());
                    } else {
                        Intent intent = new Intent(ACTION_ON_PROFILE_CONNECTED);
                        localBroadcastManager.sendBroadcast(intent);
                        logger.info("check, profile connected or connecting. no actions");
                    }
                } else {
                    logger.warn("### Trying to check with a null profile.");
                }
            }else {
                logger.warn("### background service disabled");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception on check",e);
        }
    }

    @Override
    public File backupProfile(File backupDir, String password) throws IOException {
        File backupFile = new File(
                backupDir,
                "backup_iop_connect_"+profile.getName()+Iso8601Format.formatDateTimeT(new Date(System.currentTimeMillis()))+".dat"
        );
        logger.info("Backup file path: "+backupFile.getAbsolutePath());
        backupOverwriteProfile(backupFile,password);
        scheduleBackupProfileFile(backupDir,password);
        return backupFile;
    }

    /**
     * todo: this is bad.. i need to do it synchronized.
     * @param backupFile
     * @param password
     * @return
     * @throws IOException
     */
    public File backupOverwriteProfile(File backupFile, String password) throws IOException {
        logger.info("Backup file path: "+backupFile.getAbsolutePath());
        ioPConnect.backupProfile(profile,backupFile,password);
        return backupFile;
    }

    @Override
    public void scheduleBackupProfileFile(File backupDir,String password){
        File backupFile = new File(
                backupDir,
                "backup_iop_connect_"+profile.getName()+".dat"
        );
        configurationsPreferences.setScheduleBackupEnable(true);
        configurationsPreferences.saveBackupPatch(backupFile.getAbsolutePath());
        configurationsPreferences.saveBackupPassword(password);
    }

    @Override
    public void restoreFrom(File file, String password) {
        logger.info("Restoring profile");
        if (file.exists()){
            try {
                IoPConnect.ProfileRestored profileRestored = ioPConnect.restoreFromBackup(file, password, platformSerializer);
                this.profile = profileRestored.getProfile();
                // clean everything
                ioPConnect.stop();
                this.profile = profileRestored.getProfile();
                pairingRequestDb.truncate();
                profilesDb.truncate();

                // re start
                profilesDb.saveAllProfiles(profile.getHexPublicKey(),profileRestored.getProfileInformationList());
                registerProfile(profile);
                connect(profile.getHexPublicKey());
                logger.info("restore profile completed");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else
            throw new IllegalArgumentException("File not exist, "+file.getAbsolutePath());
    }

    @Override
    public boolean isProfileRegistered() {
        return configurationsPreferences.isRegisteredInServer();
    }

    @Override
    public void addService(String serviceName, Object... args) {
        AppService appService = EnabledServicesFactory.buildService(serviceName,args);
        ioPConnect.addService(profile,appService);
    }

    @Override
    public void connect(final String pubKey) throws Exception {
        final ConnectionFuture msgListenerFuture = new ConnectionFuture();
        msgListenerFuture.setListener(new BaseMsgFuture.Listener<Boolean>() {
            @Override
            public void onAction(int messageId, Boolean object) {
                profile.setHomeHost(msgListenerFuture.getProfServerData().getHost());
                profile.setHomeHostId(msgListenerFuture.getProfServerData().getNetworkId());
                onCheckInCompleted(profile.getHexPublicKey());
            }

            @Override
            public void onFail(int messageId, int status, String statusDetail) {
                onCheckInFail(profile,status,statusDetail);
                if (status==400){
                    logger.info("Checking fail, detail "+statusDetail+", trying to reconnect after 5 seconds");
                    handler.postDelayed(reconnectRunnable,TimeUnit.SECONDS.toMillis(15));
                }

            }
            Runnable reconnectRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        connect(pubKey);
                    } catch (Exception e) {
                        e.printStackTrace();
                        // connection fail
                    }
                }
            };

        });
        ioPConnect.connectProfile(pubKey,pairingListener,null,msgListenerFuture);
    }

    public String registerProfile(Profile profile){
        this.profile = ioPConnect.createProfile(profile);
        configurationsPreferences.setIsCreated(true);
        return profile.getHexPublicKey();
    }

    @Override
    public String registerProfile(String name,String type, byte[] img, int latitude, int longitude, String extraData) throws Exception {
        if (img!=null){
            while (img.length>20480) {
                throw new BigImageException();
                // compact the image more
                //img = ImageUtils.compress(img,10);
            }
        }
        profile = ioPConnect.createProfile(null,name,type,img,extraData,null);
        configurationsPreferences.setIsCreated(true);
        return profile.getHexPublicKey();
    }

    @Override
    public String registerProfile(String name,byte[] img) throws Exception {
        return registerProfile(name,"IoP-contacts",img,0,0,null);
    }


    @Override
    public int updateProfile(String name,byte[] img,ProfSerMsgListener<Boolean> msgListener) throws Exception {
        return updateProfile(profile.getHexPublicKey(),name,img,0,0,null,msgListener);
    }

    @Override
    public int updateProfile(String pubKey,String name, byte[] img, String extraData, ProfSerMsgListener<Boolean> msgListener) throws Exception {
        return updateProfile(pubKey,name,img,0,0,extraData,msgListener);
    }

    public int updateProfile(String pubKey , String name, byte[] img, int latitude, int longitude, String extraData, final ProfSerMsgListener<Boolean> msgListener) throws Exception {
        try{
            Version version = profile.getVersion();
            //version.addMinor();
            Profile profile = new Profile(version,name,img,latitude,longitude,extraData);
            profile.setKey((KeyEd25519) this.profile.getKey());
            if (profile.getImg()!=null){
                this.profile.setImg(profile.getImg());
                while (profile.getImg().length>20480) {
                    // compact the image more
                    profile.setImg(ImageUtils.compress(profile.getImg(),10));
                }
            }
            if (name!=null && !profile.getName().equals(name)){
                this.profile.setName(name);
            }
            configurationsPreferences.saveProfile(profile);
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

    @Override
    public int updateProfileExtraData(String pubKey,Signer signer, String extraData) throws Exception {
        return 0;//profile_server.updateProfileExtraData(signer,extraData);
    }

    @Override
    public void requestPairingProfile(byte[] remotePubKey, final String remoteName, final String psHost, final ProfSerMsgListener<ProfileInformation> listener) throws Exception {
        // check if the profile already exist
        ProfileInformation profileInformationDb = null;
        String remotePubKeyStr = CryptoBytes.toHexString(remotePubKey);
        if((profileInformationDb = profilesDb.getProfile(profile.getHexPublicKey(),remotePubKeyStr))!=null){
            if(profileInformationDb.getPairStatus() != null)
                throw new IllegalArgumentException("Already known profile");
        }
        // check if the pairing request exist
        if (pairingRequestDb.containsPairingRequest(profile.getHexPublicKey(),remotePubKeyStr)){
            throw new IllegalStateException("Pairing request already exist");
        }

        // now send the request
        final PairingRequest pairingRequest = PairingRequest.buildPairingRequestFromHost(
                profile.getHexPublicKey(),
                CryptoBytes.toHexString(remotePubKey),
                psHost,profile.getName(),
                profile.getHomeHost(),
                remoteName,
                ProfileInformationImp.PairStatus.WAITING_FOR_RESPONSE
        );
        ioPConnect.requestPairingProfile(pairingRequest, new ProfSerMsgListener<ProfileInformation>() {
            @Override
            public void onMessageReceive(int messageId, ProfileInformation remote) {
                remote.setHomeHost(psHost);
                remote.setPairStatus(ProfileInformationImp.PairStatus.WAITING_FOR_RESPONSE);
                // Save invisible contact
                profilesDb.saveProfile(profile.getHexPublicKey(),remote);
                // update backup profile is there is any
                String backupProfilePath = null;
                if((backupProfilePath = configurationsPreferences.getBackupProfilePath())!=null){
                    try {
                        backupOverwriteProfile(
                                new File(backupProfilePath),
                                configurationsPreferences.getBackupPassword()
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
                pairingRequestDb.delete(pairingRequest.getId());
                listener.onMsgFail(messageId,statusValue,details);
            }

            @Override
            public String getMessageName() {
                return "Request pairing";
            }
        });
    }

    @Override
    public void acceptPairingProfile(PairingRequest pairingRequest, ProfSerMsgListener<Boolean> profSerMsgListener) throws Exception {
        ioPConnect.acceptPairingRequest(pairingRequest,profSerMsgListener);
    }

    @Override
    public void cancelPairingRequest(PairingRequest pairingRequest) {
        // todo: improve this
        ioPConnect.cancelPairingRequest(pairingRequest);
    }

    /**
     * Request chat
     * todo: add timeout..
     * @param remoteProfileInformation
     * @param readyListener
     */
    @Override
    public void requestChat(final ProfileInformation remoteProfileInformation, final ProfSerMsgListener<Boolean> readyListener, TimeUnit timeUnit, long time) throws RequestChatException, ChatCallAlreadyOpenException {
        if(!profile.hasService(EnabledServices.CHAT.getName())) throw new IllegalStateException("App service "+ EnabledServices.CHAT.name()+" is not enabled on local profile");
        try {
            // first check if the chat is active or was requested
            ChatAppService chatAppService = profile.getAppService(EnabledServices.CHAT.getName(), ChatAppService.class);
            if(chatAppService.hasOpenCall(remoteProfileInformation.getHexPublicKey())){
                // chat app service call already open, check if it stablish or it's done
                CallProfileAppService call = chatAppService.getOpenCall(remoteProfileInformation.getHexPublicKey());
                if (call!=null && !call.isDone() && !call.isFail()){
                    // call is open, throw exception
                    throw new ChatCallAlreadyOpenException("Chat call with: "+remoteProfileInformation.getName()+", already open");
                }else {
                    // this should not happen but i will check that
                    // the call is not open but the object still active.. i have to close it
                    try {
                        if (call!=null) {
                            call.dispose();
                            chatAppService.removeCall(call,"call open and done/fail without reason..");
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

            }
            final boolean tryUpdateRemoteServices = !remoteProfileInformation.hasService(EnabledServices.CHAT.getName());
            Future future = executor.submit(new Callable() {
                public Object call() {
                    try {
                        ioPConnect.callService(EnabledServices.CHAT.getName(), profile, remoteProfileInformation, tryUpdateRemoteServices, readyListener);
                    }catch (Exception e){
                        throw e;
                    }
                    return null;
                }
            });
            future.get(time, timeUnit);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException | InterruptedException e) {
            // destroy call
            CallProfileAppService callProfileAppService = profile.getAppService(EnabledServices.CHAT.getName()).getOpenCall(remoteProfileInformation.getHexPublicKey());
            callProfileAppService.dispose();
            throw new RequestChatException(e);
        }
    }

    @Override
    public void refuseChatRequest(String hexPublicKey) {
        ChatAppService chatAppService = profile.getAppService(EnabledServices.CHAT.getName(),ChatAppService.class);
        CallProfileAppService callProfileAppService = chatAppService.getOpenCall(hexPublicKey);
        if (callProfileAppService == null) return;
        callProfileAppService.dispose();
        chatAppService.removeCall(callProfileAppService,"local profile refuse chat");
    }

    @Override
    public void acceptChatRequest(String hexPublicKey, ProfSerMsgListener<Boolean> future) throws Exception {
        CallProfileAppService callProfileAppService = profile.getAppService(EnabledServices.CHAT.getName()).getOpenCall(hexPublicKey);
        if (callProfileAppService!=null) {
            callProfileAppService.sendMsg(new ChatAcceptMsg(System.currentTimeMillis()), future);
        }else {
            throw new AppServiceCallNotAvailableException("Connection not longer available");
        }
    }

    /**
     *
     * @param remoteProfileInformation
     * @param msg
     * @param msgListener
     * @throws Exception
     */
    @Override
    public void sendMsgToChat(ProfileInformation remoteProfileInformation, String msg, ProfSerMsgListener<Boolean> msgListener) throws Exception {
        if(!profile.hasService(EnabledServices.CHAT.getName())) throw new IllegalStateException("App service "+ EnabledServices.CHAT.name()+" is not enabled on local profile");
        //if(!remoteProfileInformation.hasService(EnabledServices.CHAT.getName())) throw new IllegalStateException("App service "+ EnabledServices.CHAT.name()+" is not enabled on remote profile");
        CallProfileAppService callProfileAppService = null;
        try {
            ChatMsg chatMsg = new ChatMsg(msg);
            callProfileAppService = profile.getAppService(EnabledServices.CHAT.getName())
                    .getOpenCall(remoteProfileInformation.getHexPublicKey());
            if (callProfileAppService==null) throw new ChatCallClosed("Chat connection is not longer available",remoteProfileInformation);
            callProfileAppService.sendMsg(chatMsg, msgListener);
        }catch (AppServiceCallNotAvailableException e){
            e.printStackTrace();
            throw new ChatCallClosed("Chat call not longer available",remoteProfileInformation);
        }
    }

    @Override
    public boolean isIdentityCreated() {
        return configurationsPreferences.isIdentityCreated();
    }

    @Override
    public void getProfileInformation(String profPubKey, ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException {
        getProfileInformation(profPubKey,false,profileFuture);
    }

    @Override
    public void getProfileInformation(String profPubKey, boolean getInfo, final ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException {
        ioPConnect.searchAndGetProfile(profile.getHexPublicKey(),profPubKey,getInfo,profileFuture);
    }

    @Override
    public void searchProfileByName(String name, ProfSerMsgListener<List<IopProfileServer.ProfileQueryInformation>> listener) {
        //redtoothProfileConnection.searchProfileByName(name,listener);
    }
    @Override
    public SearchMessageFuture<List<IopProfileServer.ProfileQueryInformation>> searchProfiles(SearchProfilesQuery searchProfilesQuery) {
        return null;//redtoothProfileConnection.searchProfiles(searchProfilesQuery);
    }

    @Override
    public SubsequentSearchMsgListenerFuture<List<IopProfileServer.ProfileQueryInformation>> searchSubsequentsProfiles(SearchProfilesQuery searchProfilesQuery) {
        return null;//redtoothProfileConnection.searchSubsequentProfiles(searchProfilesQuery);
    }

    @Override
    public File getUserImageFile() {
        return configurationsPreferences.getUserImageFile();
    }

    @Override
    public Profile getProfile() {
        return profile;
    }

    @Override
    public ProfileInformation getMyProfile() {
        Set<String> services = new HashSet<>();
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

    /**
     * Return profiles that this profile is paired or it's waiting for some pair answer from the other side.
     * @return
     */
    @Override
    public List<ProfileInformation> getKnownProfiles() {
        List<ProfileInformation> ret = new ArrayList<>();
        List<ProfileInformation> knownProfiles = ioPConnect.getKnownProfiles(profile.getHexPublicKey());
        // todo: this is a lazy remove..
        for (ProfileInformation knownProfile : knownProfiles) {
            if (!Arrays.equals(knownProfile.getPublicKey(),profile.getPublicKey())){
                ret.add(knownProfile);
            }
        }
        return ret;
    }

    @Override
    public ProfileInformation getKnownProfile(String pubKey){
        return ioPConnect.getKnownProfile(profile.getHexPublicKey(),pubKey);
    }

    @Override
    public PairingRequest getProfilePairingRequest(String hexPublicKey) {
        return pairingRequestDb.getPairingRequest(profile.getHexPublicKey(),hexPublicKey);
    }

    @Override
    public List<PairingRequest> getPairingRequests() {
        return pairingRequestDb.pairingRequests(profile.getHexPublicKey());
    }

    @Override
    public List<PairingRequest> getPairingOpenRequests(){
        return pairingRequestDb.openPairingRequests(profile.getHexPublicKey());
    }

    @Override
    public String getPsHost() {
        return configurationsPreferences.getMainProfileServer().getHost();
    }

    @Override
    public void deteleContacts() {
        profilesDb.truncate();
    }

    @Override
    public void deletePairingRequests() {
        pairingRequestDb.truncate();
    }

    @Override
    public Collection<PairingRequest> listAllPairingRequests() {
        return pairingRequestDb.list();
    }

    @Override
    public Collection<ProfileInformation> listAllProfileInformation() {
        return profilesDb.listAll(profile.getHexPublicKey());
    }

    @Override
    public boolean isProfileConnectedOrConnecting() {
        return ioPConnect.isProfileConnectedOrConnecting(profile.getHexPublicKey());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand");
        if (intent!=null) {
            String action = intent.getAction();
            if (action.equals(ACTION_SCHEDULE_SERVICE)) {
                check();
            } else if (action.equals(ACTION_BOOT_SERVICE)) {
                // only check for now..
                check();
            }
        }else {
            //
            check();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        executor.shutdown();
        ioPConnect.stop();
        super.onDestroy();
    }

    /**
     * Schedule service for later
     */
    private void tryScheduleService() {
        boolean isSchedule = System.currentTimeMillis()<configurationsPreferences.getScheduleServiceTime();

        if (!isSchedule){
            logger.info("scheduling service");
            AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
            long scheduleTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5); // 10 minutes from now

            Intent intent = new Intent(this, IoPConnectService.class);
            intent.setAction(ACTION_SCHEDULE_SERVICE);
            alarm.set(
                    // This alarm will wake up the device when System.currentTimeMillis()
                    // equals the second argument value
                    alarm.RTC_WAKEUP,
                    scheduleTime,
                    // PendingIntent.getService creates an Intent that will start a service
                    // when it is called. The first argument is the Context that will be used
                    // when delivering this intent. Using this has worked for me. The second
                    // argument is a request code. You can use this code to cancel the
                    // pending intent if you need to. Third is the intent you want to
                    // trigger. In this case I want to create an intent that will start my
                    // service. Lastly you can optionally pass flags.
                    PendingIntent.getService(this, 0,intent , 0)
            );
            // save
            configurationsPreferences.saveScheduleServiceTime(scheduleTime);
        }
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

    @Override
    public boolean isDeviceLocationEnabled() {
        return false;
    }

    @Override
    public GpsLocation getDeviceLocation() {
        return null;
    }

    private void broadcastUpdateProfile() {
        Intent intent = new Intent(IntentBroadcastConstants.ACTION_PROFILE_UPDATED_CONSTANT);
        localBroadcastManager.sendBroadcast(intent);
    }
}
