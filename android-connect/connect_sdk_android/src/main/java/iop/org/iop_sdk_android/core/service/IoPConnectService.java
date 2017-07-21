package iop.org.iop_sdk_android.core.service;

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
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.fermat.redtooth.core.IoPConnect;
import org.fermat.redtooth.core.IoPConnectContext;
import org.fermat.redtooth.global.PlatformSerializer;
import org.fermat.redtooth.profile_server.engine.app_services.AppService;
import org.fermat.redtooth.services.EnabledServices;
import org.fermat.redtooth.global.DeviceLocation;
import org.fermat.redtooth.global.GpsLocation;
import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.ProfileServerConfigurations;
import org.fermat.redtooth.profile_server.engine.listeners.EngineListener;
import org.fermat.redtooth.profile_server.engine.SearchProfilesQuery;
import org.fermat.redtooth.profile_server.engine.futures.SearchMessageFuture;
import org.fermat.redtooth.profile_server.engine.futures.SubsequentSearchMsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.imp.ProfileInformationImp;
import org.fermat.redtooth.profile_server.model.KeyEd25519;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;
import org.fermat.redtooth.profiles_manager.PairingRequest;
import org.fermat.redtooth.services.EnabledServicesFactory;
import org.fermat.redtooth.services.chat.ChatCallAlreadyOpenException;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import iop.org.iop_sdk_android.core.crypto.CryptoWrapperAndroid;
import iop.org.iop_sdk_android.core.db.SqlitePairingRequestDb;
import iop.org.iop_sdk_android.core.db.SqliteProfilesDb;
import iop.org.iop_sdk_android.core.service.modules.Core;
import org.fermat.redtooth.global.Module;
import iop.org.iop_sdk_android.core.service.modules.interfaces.ChatModule;
import iop.org.iop_sdk_android.core.service.modules.interfaces.PairingModule;
import iop.org.iop_sdk_android.core.service.modules.interfaces.ProfilesModule;

import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_ON_PROFILE_CONNECTED;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_ON_PROFILE_DISCONNECTED;


/**
 * Created by mati on 09/11/16.
 */

public class IoPConnectService extends Service implements ModuleRedtooth, EngineListener,DeviceLocation {

    private final Logger logger = LoggerFactory.getLogger(IoPConnectService.class);

    private static final String TAG = "IoPConnectService";

    private static final String ACTION_SCHEDULE_SERVICE = "schedule_service";
    public static final String ACTION_BOOT_SERVICE = "boot_service";

    private LocalBroadcastManager localBroadcastManager;

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

    private Core core;

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


    public ProfileServerConfigurations getConfPref() {
        return configurationsPreferences;
    }

    public SqliteProfilesDb getProfilesDb() {
        return profilesDb;
    }

    public SqlitePairingRequestDb getPairingRequestsDb() {
        return pairingRequestDb;
    }

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
                tryScheduleService();

                // init core
                core = new Core(this,ioPConnect);
            }
        }catch (Exception e){
            e.printStackTrace();
            isInitialized.set(false);
        }
    }


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
        configurationsPreferences.saveBackupPassword(password);
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

    @Override
    public boolean isProfileRegistered() {
        return configurationsPreferences.isRegisteredInServer();
    }

    @Override
    public void addService(String serviceName, Object... args) {
        Module module = core.getModule(serviceName);
        AppService appService = EnabledServicesFactory.buildService(serviceName,module,args);
        ioPConnect.addService(profile,appService);
    }

    @Override
    public void connect(final String pubKey) throws Exception {
        ProfilesModule module = core.getModule(EnabledServices.PROFILE_DATA.getName(), ProfilesModule.class);
        module.connect(pubKey);
    }

    public String registerProfile(Profile profile){
        this.profile = ioPConnect.createProfile(profile);
        return profile.getHexPublicKey();
    }

    @Override
    public String registerProfile(String name,String type, byte[] img, int latitude, int longitude, String extraData) throws Exception {
        ProfilesModule module = core.getModule(EnabledServices.PROFILE_DATA.getName(), ProfilesModule.class);
        return module.registerProfile(name,type,img,latitude,longitude,extraData);
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
    public int updateProfile(String pubKey , String name, byte[] img, int latitude, int longitude, String extraData, final ProfSerMsgListener<Boolean> msgListener) throws Exception {
        return core.getModule(EnabledServices.PROFILE_DATA.getName(), ProfilesModule.class)
                .updateProfile(
                        pubKey,
                        name,
                        img,
                        latitude,
                        longitude,
                        extraData,
                        msgListener
                );
    }

    @Override
    public void requestPairingProfile(byte[] remotePubKey, final String remoteName, final String psHost, final ProfSerMsgListener<ProfileInformation> listener) throws Exception {
        core.getModule(EnabledServices.PROFILE_PAIRING.getName(), PairingModule.class)
                .requestPairingProfile(
                        profile,
                        remotePubKey,
                        remoteName,
                        psHost,
                        listener
                );
    }

    @Override
    public void acceptPairingProfile(PairingRequest pairingRequest, ProfSerMsgListener<Boolean> profSerMsgListener) throws Exception {
        core.getModule(EnabledServices.PROFILE_PAIRING.getName(), PairingModule.class)
                .acceptPairingProfile(
                        pairingRequest,
                        profSerMsgListener
                );
    }

    @Override
    public void cancelPairingRequest(PairingRequest pairingRequest) {
        core.getModule(EnabledServices.PROFILE_PAIRING.getName(), PairingModule.class)
                .cancelPairingRequest(pairingRequest
                );
    }

    /**
     * Request chat
     * todo: add timeout..
     * @param remoteProfileInformation
     * @param readyListener
     */
    @Override
    public void requestChat(final ProfileInformation remoteProfileInformation, final ProfSerMsgListener<Boolean> readyListener, TimeUnit timeUnit, long time) throws RequestChatException, ChatCallAlreadyOpenException {
        core.getModule(EnabledServices.CHAT.getName(), ChatModule.class)
                .requestChat(
                        profile,
                        remoteProfileInformation,
                        readyListener,
                        timeUnit,
                        time
                );
    }

    @Override
    public void refuseChatRequest(String hexPublicKey) {
        core.getModule(EnabledServices.CHAT.getName(), ChatModule.class)
                .refuseChatRequest(
                        profile,
                        hexPublicKey
                );
    }

    @Override
    public void acceptChatRequest(String remoteHexPublicKey, ProfSerMsgListener<Boolean> future) throws Exception {
        core.getModule(EnabledServices.CHAT.getName(), ChatModule.class)
                .acceptChatRequest(
                        profile,
                        remoteHexPublicKey,
                        future
                );
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
        core.getModule(EnabledServices.CHAT.getName(), ChatModule.class)
                .sendMsgToChat(
                        profile,
                        remoteProfileInformation,
                        msg,
                        msgListener
                );
    }

    @Override
    public boolean isChatActive(String remotePk){
        return core.getModule(EnabledServices.CHAT.getName(), ChatModule.class)
                .isChatActive(
                        profile,
                        remotePk
                );
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
        core.clean();
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

    @Override
    public boolean isDeviceLocationEnabled() {
        return false;
    }

    @Override
    public GpsLocation getDeviceLocation() {
        return null;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }
}
