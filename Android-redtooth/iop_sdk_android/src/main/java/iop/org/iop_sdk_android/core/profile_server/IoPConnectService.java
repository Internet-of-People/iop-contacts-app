package iop.org.iop_sdk_android.core.profile_server;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.fermat.redtooth.core.IoPConnect;
import org.fermat.redtooth.core.IoPConnectContext;
import org.fermat.redtooth.core.services.DefaultServices;
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
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.listeners.EngineListener;
import org.fermat.redtooth.profile_server.engine.SearchProfilesQuery;
import org.fermat.redtooth.profile_server.engine.futures.SearchMessageFuture;
import org.fermat.redtooth.profile_server.engine.futures.SubsequentSearchMsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.app_services.PairingListener;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.engine.listeners.ProfileListener;
import org.fermat.redtooth.profile_server.model.KeyEd25519;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;
import org.fermat.redtooth.profiles_manager.PairingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import iop.org.iop_sdk_android.core.crypto.CryptoWrapperAndroid;
import iop.org.iop_sdk_android.core.db.SqlitePairingRequestDb;
import iop.org.iop_sdk_android.core.db.SqliteProfilesDb;


/**
 * Created by mati on 09/11/16.
 */

public class IoPConnectService extends Service implements ModuleRedtooth, EngineListener,DeviceLocation {

    private final Logger logger = LoggerFactory.getLogger(IoPConnectService.class);

    private static final String TAG = "IoPConnectService";

    private LocalBroadcastManager localBroadcastManager;

    private ExecutorService executor;
    /** Context */
    private IoPConnectContext application;
    /** Main library */
    private IoPConnect ioPConnect;
    /** Configurations impl */
    private ProfileServerConfigurations configurationsPreferences;
    private Profile profile;
    /** Listeners */
    private ProfileListener profileListener;
    private PairingListener pairingListener;
    /** Databases */
    private SqlitePairingRequestDb pairingRequestDb;
    private SqliteProfilesDb profilesDb;

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
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        try {
            application = (IoPConnectContext) getApplication();
            configurationsPreferences = new ProfileServerConfigurationsImp(this,getSharedPreferences(ProfileServerConfigurationsImp.PREFS_NAME,0));
            KeyEd25519 keyEd25519 = (KeyEd25519) configurationsPreferences.getUserKeys();
            if (keyEd25519!=null)
                profile = new Profile(configurationsPreferences.getProfileVersion(),configurationsPreferences.getUsername(),configurationsPreferences.getProfileType(),(KeyEd25519) configurationsPreferences.getUserKeys());
            executor = Executors.newFixedThreadPool(3);
            pairingRequestDb = new SqlitePairingRequestDb(this);
            profilesDb = new SqliteProfilesDb(this);
            ioPConnect = new IoPConnect(application,new CryptoWrapperAndroid(),new SslContextFactory(this),profilesDb,pairingRequestDb,this);//configurationsPreferences,new CryptoWrapperAndroid(),new SslContextFactory(this));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean isProfileRegistered() {
        return configurationsPreferences.isRegisteredInServer();
    }

    @Override
    public void connect(String pubKey) throws Exception {
        MsgListenerFuture<Boolean> msgListenerFuture = new MsgListenerFuture();
        msgListenerFuture.setListener(new BaseMsgFuture.Listener<Boolean>() {
            @Override
            public void onAction(int messageId, Boolean object) {
                onCheckInCompleted(profile);
            }

            @Override
            public void onFail(int messageId, int status, String statusDetail) {
                onCheckInFail(profile,status,statusDetail);
            }
        });
        ioPConnect.connectProfile(pubKey,pairingListener,null,msgListenerFuture);
    }

    @Override
    public String registerProfile(String name,String type, byte[] img, int latitude, int longitude, String extraData) throws Exception {
        profile = ioPConnect.createProfile(null,name,type,img,extraData,null);
        configurationsPreferences.setIsCreated(true);
        return profile.getHexPublicKey();
    }

    @Override
    public String registerProfile(String name,byte[] img) throws Exception {
        return registerProfile(name,"IoP-contacts",img,0,0,null);
    }

    @Override
    public int updateProfile(String name, ProfSerMsgListener msgListener) throws Exception {
        return updateProfile(null,profile.getHexPublicKey(),name,null,0,0,null,msgListener);
    }

    @Override
    public int updateProfile(String name,byte[] img,ProfSerMsgListener msgListener) throws Exception {
        return updateProfile(null,profile.getHexPublicKey(),name,img,0,0,null,msgListener);
    }

    @Override
    public int updateProfile(String pubKey,String name, byte[] img, String extraData, ProfSerMsgListener msgListener) throws Exception {
        return updateProfile(null,pubKey,name,img,0,0,extraData,msgListener);
    }

    @Override
    public int updateProfile(byte[] version, String pubKey ,String name, byte[] img, int latitude, int longitude, String extraData, ProfSerMsgListener msgListener) throws Exception {
//        Log.d(TAG,"updateProfile, state: "+state);
        try{
            Profile profile = new Profile(version,name,img,latitude,longitude,extraData);
            profile.setKey((KeyEd25519) this.profile.getKey());
            if (version==null){
                profile.setVersion(this.profile.getVersion());
            }
            return ioPConnect.updateProfile(profile,msgListener);
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
    public void requestPairingProfile(byte[] remotePubKey, byte[] profileServerId, ProfSerMsgListener<Integer> listener) {
        PairingRequest pairingRequest = PairingRequest.buildPairingRequest(profile.getHexPublicKey(),CryptoBytes.toHexString(remotePubKey),null,profile.getName());
        ioPConnect.requestPairingProfile(pairingRequest,listener);
    }

    @Override
    public void requestPairingProfile(byte[] remotePubKey, String psHost, ProfSerMsgListener<Integer> listener) {
        PairingRequest pairingRequest = PairingRequest.buildPairingRequestFromHost(profile.getHexPublicKey(),CryptoBytes.toHexString(remotePubKey),psHost,profile.getName());
        ioPConnect.requestPairingProfile(pairingRequest,listener);
    }

    @Override
    public void acceptPairingProfile(PairingRequest pairingRequest) {
        ioPConnect.acceptPairingRequest(pairingRequest);
    }

    @Override
    public void cancelPairingRequest(PairingRequest pairingRequest) {
        // todo: improve this
    }

    @Override
    public boolean isIdentityCreated() {
        return configurationsPreferences.isIdentityCreated();
    }

    @Override
    public void setPairListener(PairingListener pairListener) {
        this.pairingListener = pairListener;
    }

    @Override
    public void setProfileListener(ProfileListener profileListener) {
        this.profileListener = profileListener;
    }

    @Override
    public void getProfileInformation(String profPubKey,final ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException {
        ioPConnect.searchAndGetProfile(profile.getHexPublicKey(),profPubKey,profileFuture);
    }

    @Override
    public void getProfileInformation(String profPubKey, boolean withImage, ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException {
        ioPConnect.searchAndGetProfile(profile.getHexPublicKey(),profPubKey,profileFuture);
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
    public List<ProfileInformation> getKnownProfiles() {
        List<ProfileInformation> ret = new ArrayList<>();
        List<ProfileInformation> knownProfiles = ioPConnect.getKnownProfiles(profile.getPublicKey());
        // todo: this is a lazy remove..
        for (ProfileInformation knownProfile : knownProfiles) {
            if (!Arrays.equals(knownProfile.getPublicKey(),profile.getPublicKey())){
                ret.add(knownProfile);
            }
        }
        return ret;
    }

    @Override
    public ProfileInformation getKnownProfile(byte[] pubKey){
        return ioPConnect.getKnownProfile(pubKey);
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        ioPConnect.stop();
        executor.shutdown();
        super.onDestroy();
    }

    @Override
    public void onCheckInCompleted(Profile profile) {
        if (profileListener!=null){
            profileListener.onConnect(profile);
        }
    }

    private void onCheckInFail(Profile profile, int status, String statusDetail) {
        if (profileListener!=null){
            profileListener.onCheckInFail(profile,status,statusDetail);
        }
    }

    @Override
    public boolean isDeviceLocationEnabled() {
        return false;
    }

    @Override
    public GpsLocation getDeviceLocation() {
        return null;
    }

}
