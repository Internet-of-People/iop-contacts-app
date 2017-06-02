package org.fermat.redtooth.core;

import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.crypto.CryptoWrapper;
import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.ProfileServerConfigurations;
import org.fermat.redtooth.profile_server.SslContextFactory;
import org.fermat.redtooth.profile_server.engine.futures.BaseMsgFuture;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.imp.ProfileInformationImp;
import org.fermat.redtooth.profile_server.model.KeyEd25519;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;
import org.fermat.redtooth.profiles_manager.ProfilesManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by mati on 17/05/17.
 * todo: clase encargada de crear perfiles, agregar aplication services y hablar con las capas superiores.
 */

public class Redtooth {

    /** Profiles connection manager */
    private ConcurrentMap<String,RedtoothProfileConnection> managers;
    /** Enviroment context */
    private RedtoothContext context;
    /** Profiles manager  */
    private ProfilesManager profilesManager;
    /** Crypto platform implementation */
    private CryptoWrapper cryptoWrapper;
    /** Socket factory */
    private SslContextFactory sslContextFactory;

    public Redtooth(RedtoothContext contextWrapper, CryptoWrapper cryptoWrapper, SslContextFactory sslContextFactory,ProfilesManager profilesManager) {
        this.context = contextWrapper;
        this.cryptoWrapper = cryptoWrapper;
        this.sslContextFactory = sslContextFactory;
        this.managers = new ConcurrentHashMap<>();
        this.profilesManager = profilesManager;
    }


    /**
     * Create a profile inside the redtooth
     *
     * @param profileOwnerChallenge -> the owner of the profile must sign his messages
     * @param name
     * @param type
     * @param extraData
     * @param secretPassword -> encription password for the profile keys
     * @return profile pubKey
     */
    public Profile createProfile(byte[] profileOwnerChallenge,String name,String type,String extraData,String secretPassword){
        byte[] version = new byte[]{0,0,1};
        ProfileServerConfigurations profileServerConfigurations = createEmptyProfileServerConf();
        KeyEd25519 keyEd25519 = profileServerConfigurations.createNewUserKeys();
        Profile profile = new Profile(version, name,type,keyEd25519);
        profile.setExtraData(extraData);
        // save
        profileServerConfigurations.saveUserKeys(profile.getKey());
        profileServerConfigurations.saveProfile(profile);
        profileServerConfigurations.setIsCreated(true);
        // save profile
        profileServerConfigurations.saveProfile(profile);
        addConnection(profileServerConfigurations,keyEd25519);
        // todo: return profile connection pk
        return profile;
    }

    public void backupProfile(long profId,String backupDir){
        //Profile profile = profilesManager.getProfile(profId);
        // todo: backup the profile on an external dir file.
    }

    public void connectProfile(String profilePublicKey,byte[] ownerChallenge) throws Exception {
         managers.get(profilePublicKey).init();
    }

    public void connectProfileSync(String profilePublicKey,byte[] ownerChallenge) throws Exception {
        if (!managers.containsKey(profilePublicKey)){
            ProfileServerConfigurations profileServerConfigurations = createEmptyProfileServerConf();
            KeyEd25519 keyEd25519 = (KeyEd25519) profileServerConfigurations.getUserKeys();
            if (keyEd25519==null) throw new IllegalStateException("no pubkey saved");
            addConnection(profileServerConfigurations,keyEd25519);
        }
        MsgListenerFuture<Boolean> initFuture = new MsgListenerFuture<Boolean>();
        managers.get(profilePublicKey).init(initFuture);
        initFuture.get();
    }

    public int updateProfile(Profile profile, ProfSerMsgListener msgListener) throws Exception {
        if (!managers.containsKey(profile.getHexPublicKey())){
            ProfileServerConfigurations profileServerConfigurations = createEmptyProfileServerConf();
            KeyEd25519 keyEd25519 = (KeyEd25519) profileServerConfigurations.getUserKeys();
            if (keyEd25519==null) throw new IllegalStateException("no pubkey saved");
            MsgListenerFuture<Boolean> initFuture = new MsgListenerFuture<Boolean>();
            addConnection(profileServerConfigurations,keyEd25519).init(initFuture);
            initFuture.get();
        }
        return managers.get(profile.getHexPublicKey()).updateProfile(profile.getVersion(),profile.getName(),profile.getImg(),profile.getLatitude(),profile.getLongitude(),profile.getExtraData(),msgListener);
    }

    private RedtoothProfileConnection addConnection(ProfileServerConfigurations profileServerConfigurations,KeyEd25519 keyEd25519){
        // profile connection
        RedtoothProfileConnection redtoothProfileConnection = new RedtoothProfileConnection(context,profileServerConfigurations,cryptoWrapper,sslContextFactory);
        // map the profile connection with his public key
        managers.put(keyEd25519.getPublicKeyHex(),redtoothProfileConnection);
        return redtoothProfileConnection;
    }

    /**
     * Search based on CAN, could be LOC and Profile server.
     *
     * @param requeteerPubKey
     * @param profPubKey
     * @param future
     * @throws CantConnectException
     * @throws CantSendMessageException
     */
    public void searchAndGetProfile(String requeteerPubKey,String profPubKey,final ProfSerMsgListener<ProfileInformation> future) throws CantConnectException, CantSendMessageException {
        if (!managers.containsKey(requeteerPubKey)) throw new IllegalStateException("Profile connection not established");
        ProfileInformation info = profilesManager.getProfile(CryptoBytes.fromHexToBytes(profPubKey));
        if (info!=null){
            //todo: add TTL and expiration -> info.getLastUpdateTime().
            // if it's not valid go to CAN.
            future.onMessageReceive(0,info);
        }else {
            // CAN FLOW


            //
            MsgListenerFuture<IopProfileServer.GetProfileInformationResponse> getFuture = new MsgListenerFuture<>();
            getFuture.setListener(new BaseMsgFuture.Listener<IopProfileServer.GetProfileInformationResponse>() {
                @Override
                public void onAction(int messageId, IopProfileServer.GetProfileInformationResponse message) {
                    IopProfileServer.ProfileInformation signedProfile = message.getSignedProfile().getProfile();
                    ProfileInformationImp profileInformation = new ProfileInformationImp();
                    profileInformation.setVersion(signedProfile.getVersion().toByteArray());
                    profileInformation.setPubKey(signedProfile.getPublicKey().toByteArray());
                    profileInformation.setName(signedProfile.getName());
                    profileInformation.setType(signedProfile.getType());
                    profileInformation.setImgHash(signedProfile.getProfileImageHash().toByteArray());
                    profileInformation.setTumbnailImgHash(signedProfile.getThumbnailImageHash().toByteArray());
                    profileInformation.setLatitude(signedProfile.getLatitude());
                    profileInformation.setLongitude(signedProfile.getLongitude());
                    profileInformation.setExtraData(signedProfile.getExtraData());
                    profileInformation.setIsOnline(message.getIsOnline());
                    profileInformation.setUpdateTimestamp(System.currentTimeMillis());

                    for (int i = 0; i < message.getApplicationServicesCount(); i++) {
                        profileInformation.addAppService(message.getApplicationServices(i));
                    }
                    // save unknown profile
                    profilesManager.saveProfile(profileInformation);

                    future.onMessageReceive(messageId, profileInformation);
                }

                @Override
                public void onFail(int messageId, int status, String statusDetail) {
                    future.onMsgFail(messageId, status, statusDetail);
                }
            });
            managers.get(requeteerPubKey).getProfileInformation(profPubKey, true, false, true, getFuture);
        }
    }

    private ProfileServerConfigurations createEmptyProfileServerConf(){
        return context.createProfSerConfig();
    }

    /**
     * Load a profile server configuration from one profile
     * @param profPk
     * @return
     */
    private ProfileServerConfigurations loadProfileServerConf(String profPk){
        return null;
    }


    public List<ProfileInformation> getKnownProfiles(byte[] pubKey){
        return profilesManager.listConnectedProfiles(pubKey);
    }

    public ProfileInformation getKnownProfile(byte[] pubKey) {
        return profilesManager.getProfile(pubKey);
    }

    public void stop() {
        for (Map.Entry<String, RedtoothProfileConnection> stringRedtoothProfileConnectionEntry : managers.entrySet()) {
            try {
                stringRedtoothProfileConnectionEntry.getValue().stop();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void requestProfileConnection(byte[] publicKey, byte[] remotePubKey) {

    }
}
