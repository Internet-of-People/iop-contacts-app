package iop.org.iop_sdk_android.core.profile_server;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.fermat.redtooth.core.Redtooth;
import org.fermat.redtooth.core.RedtoothContext;
import org.fermat.redtooth.core.RedtoothProfileConnection;
import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.ProfileServerConfigurations;
import org.fermat.redtooth.profile_server.Signer;
import org.fermat.redtooth.profile_server.engine.CallProfileAppService;
import org.fermat.redtooth.profile_server.engine.EngineListener;
import org.fermat.redtooth.profile_server.engine.SearchProfilesQuery;
import org.fermat.redtooth.profile_server.engine.futures.BaseMsgFuture;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.futures.SearchMessageFuture;
import org.fermat.redtooth.profile_server.engine.futures.SubsequentSearchMsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.model.KeyEd25519;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import iop.org.iop_sdk_android.core.crypto.CryptoWrapperAndroid;
import iop.org.iop_sdk_android.core.db.SqliteProfilesDb;


/**
 * Created by mati on 09/11/16.
 */

public class RedtoothService extends Service implements ModuleRedtooth, EngineListener {

    private static final String TAG = "RedtoothService";

    private ExecutorService executor;
    /** Context */
    private RedtoothContext application;
    /** Main library */
    private Redtooth redtooth;
    /** Configurations impl */
    private ProfileServerConfigurations configurationsPreferences;

    private Profile profile;

    public class ProfServerBinder extends Binder {
        public RedtoothService getService() {
            return RedtoothService.this;
        }
    }

    private final IBinder mBinder = new RedtoothService.ProfServerBinder();

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
        try {
            application = (RedtoothContext) getApplication();
            configurationsPreferences = new ProfileServerConfigurationsImp(this,getSharedPreferences(ProfileServerConfigurationsImp.PREFS_NAME,0));
            KeyEd25519 keyEd25519 = (KeyEd25519) configurationsPreferences.getUserKeys();
            if (keyEd25519!=null)
                profile = new Profile(configurationsPreferences.getProfileVersion(),configurationsPreferences.getUsername(),configurationsPreferences.getProfileType(),(KeyEd25519) configurationsPreferences.getUserKeys());
            executor = Executors.newFixedThreadPool(3);
            redtooth = new Redtooth(application,new CryptoWrapperAndroid(),new SslContextFactory(this),new SqliteProfilesDb(this));//configurationsPreferences,new CryptoWrapperAndroid(),new SslContextFactory(this));
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
        redtooth.connectProfileSync(pubKey,null);
    }

    @Override
    public String registerProfile(String name,String type, byte[] img, int latitude, int longitude, String extraData) throws Exception {
        profile = redtooth.createProfile(null,name,type,extraData,null);
        configurationsPreferences.setIsCreated(true);
        return profile.getHexPublicKey();
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
            return redtooth.updateProfile(profile,msgListener);
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
    public boolean isIdentityCreated() {
        return configurationsPreferences.isRegisteredInServer();
    }

    @Override
    public void getProfileInformation(String profPubKey,final ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException {
        redtooth.searchAndGetProfile(profile.getHexPublicKey(),profPubKey,profileFuture);
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
    public void requestProfileConnection(byte[] remotePubKey) {
        redtooth.requestProfileConnection(profile.getPublicKey(),remotePubKey);
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
        List<ProfileInformation> knownProfiles = redtooth.getKnownProfiles(profile.getPublicKey());
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
        return redtooth.getKnownProfile(pubKey);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        redtooth.stop();
        executor.shutdown();
        super.onDestroy();
    }

    @Override
    public void newCallReceived(CallProfileAppService callProfileAppService) {

    }


}
