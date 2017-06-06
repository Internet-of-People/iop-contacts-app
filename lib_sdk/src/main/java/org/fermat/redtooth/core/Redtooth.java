package org.fermat.redtooth.core;

import org.fermat.redtooth.core.services.DefaultServices;
import org.fermat.redtooth.core.services.pairing.PairingMsg;
import org.fermat.redtooth.core.services.pairing.PairingMsgTypes;
import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.crypto.CryptoWrapper;
import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.ProfileServerConfigurations;
import org.fermat.redtooth.profile_server.SslContextFactory;
import org.fermat.redtooth.profile_server.engine.CallProfileAppService;
import org.fermat.redtooth.profile_server.engine.EngineListener;
import org.fermat.redtooth.profile_server.engine.futures.BaseMsgFuture;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.imp.ProfileInformationImp;
import org.fermat.redtooth.profile_server.model.KeyEd25519;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;
import org.fermat.redtooth.profiles_manager.PairingRequest;
import org.fermat.redtooth.profiles_manager.PairingRequestsManager;
import org.fermat.redtooth.profiles_manager.ProfilesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by mati on 17/05/17.
 * todo: clase encargada de crear perfiles, agregar aplication services y hablar con las capas superiores.
 */

public class Redtooth {

    private final Logger logger = LoggerFactory.getLogger(Redtooth.class);

    /** Profiles connection manager */
    private ConcurrentMap<String,RedtoothProfileConnection> managers;
    /** Enviroment context */
    private RedtoothContext context;
    /** Profiles manager db */
    private ProfilesManager profilesManager;
    /** Pairing request manager db  */
    private PairingRequestsManager pairingRequestsManager;
    /** Crypto platform implementation */
    private CryptoWrapper cryptoWrapper;
    /** Socket factory */
    private SslContextFactory sslContextFactory;

    public Redtooth(RedtoothContext contextWrapper, CryptoWrapper cryptoWrapper, SslContextFactory sslContextFactory, ProfilesManager profilesManager, PairingRequestsManager pairingRequestsManager) {
        this.context = contextWrapper;
        this.cryptoWrapper = cryptoWrapper;
        this.sslContextFactory = sslContextFactory;
        this.managers = new ConcurrentHashMap<>();
        this.profilesManager = profilesManager;
        this.pairingRequestsManager = pairingRequestsManager;
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

    public void connectProfileSync(String profilePublicKey,EngineListener engineListener,byte[] ownerChallenge) throws Exception {
        if (!managers.containsKey(profilePublicKey)){
            ProfileServerConfigurations profileServerConfigurations = createEmptyProfileServerConf();
            KeyEd25519 keyEd25519 = (KeyEd25519) profileServerConfigurations.getUserKeys();
            if (keyEd25519==null) throw new IllegalStateException("no pubkey saved");
            addConnection(profileServerConfigurations,keyEd25519,engineListener);
        }
        MsgListenerFuture<Boolean> initFuture = new MsgListenerFuture<Boolean>();
        getProfileConnection(profilePublicKey).init(initFuture);
        initFuture.get();
    }

    public int updateProfile(Profile profile, ProfSerMsgListener msgListener) throws Exception {
        return getProfileConnection(profile.getHexPublicKey()).updateProfile(profile.getVersion(),profile.getName(),profile.getImg(),profile.getLatitude(),profile.getLongitude(),profile.getExtraData(),msgListener);
    }

    private RedtoothProfileConnection addConnection(ProfileServerConfigurations profileServerConfigurations,KeyEd25519 keyEd25519,EngineListener profServerEngineListener){
        // profile connection
        RedtoothProfileConnection redtoothProfileConnection = new RedtoothProfileConnection(context,profileServerConfigurations,cryptoWrapper,sslContextFactory);
        redtoothProfileConnection.setProfServerEngineListener(profServerEngineListener);
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

    /**
     * Send a request pair notification to a remote profile
     *
     * @param pairingRequest
     * @param listener
     */
    public void requestPairingProfile(final PairingRequest pairingRequest, final ProfSerMsgListener<Integer> listener) {
        logger.info("requestPairingProfile, remote: "+pairingRequest.getRemotePubKey());
        final RedtoothProfileConnection connection = getProfileConnection(pairingRequest.getSenderPubKey());
        // first the call
        MsgListenerFuture<CallProfileAppService> callListener = new MsgListenerFuture();
        callListener.setListener(new BaseMsgFuture.Listener<CallProfileAppService>() {
            @Override
            public void onAction(int messageId, final CallProfileAppService call) {
                try {
                    logger.info("call establish, remote: " + call.getRemotePubKey());
                    // now send the pairing message
                    MsgListenerFuture<Boolean> pairingMsgFuture = new MsgListenerFuture();
                    pairingMsgFuture.setListener(new BaseMsgFuture.Listener<Boolean>() {
                        @Override
                        public void onAction(int messageId, Boolean res) {
                            logger.info("pairing msg sent, remote: " + call.getRemotePubKey());
                            int pairingRequestId = pairingRequestsManager.savePairingRequest(pairingRequest);
                            listener.onMessageReceive(messageId,pairingRequestId);
                        }

                        @Override
                        public void onFail(int messageId, int status, String statusDetail) {
                            logger.info("pairing msg fail, remote: " + call.getRemotePubKey());
                            listener.onMsgFail(messageId,status,statusDetail);
                        }
                    });
                    PairingMsg pairingMsg = new PairingMsg(pairingRequest.getSenderName());
                    call.sendMsg(pairingMsg, pairingMsgFuture);

                } catch (CantSendMessageException e) {
                    e.printStackTrace();
                    listener.onMsgFail(messageId,400,e.getMessage());
                } catch (CantConnectException e) {
                    e.printStackTrace();
                    listener.onMsgFail(messageId,400,e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onMsgFail(messageId,400,e.getMessage());
                }
            }

            @Override
            public void onFail(int messageId, int status, String statusDetail) {
                logger.info("call fail, remote: "+statusDetail);
                listener.onMsgFail(messageId,status,statusDetail);
            }
        });
        connection.callProfileAppService(pairingRequest.getRemotePubKey(), DefaultServices.PROFILE_PAIRING.getName(),false,callListener);
    }

    /**
     * Send a pair acceptance
     *
     * @param senderHexPublicKey
     * @param profileServerId
     * @param publicKey
     */
    public void acceptPairingRequest(String senderHexPublicKey, byte[] profileServerId, byte[] publicKey) {
        try {
            String remotePubKeyHex = CryptoBytes.toHexString(publicKey);
            logger.info("acceptPairingRequest, remote: " + remotePubKeyHex);
            final RedtoothProfileConnection connection = getProfileConnection(senderHexPublicKey);
            CallProfileAppService call = connection.getActiveAppCallService(remotePubKeyHex);
            final MsgListenerFuture<Boolean> future = new MsgListenerFuture();
            //future.setListener(); -> todo: add future listener and save acceptPairing sent
            if (call != null) {
                call.sendMsg(PairingMsgTypes.PAIR_ACCEPT.getType(), future);
            }else {
                MsgListenerFuture<CallProfileAppService> callFuture = new MsgListenerFuture<>();
                callFuture.setListener(new BaseMsgFuture.Listener<CallProfileAppService>() {
                    @Override
                    public void onAction(int messageId, CallProfileAppService call) {
                        try {
                            call.sendMsg(PairingMsgTypes.PAIR_ACCEPT.getType(), future);
                        } catch (Exception e) {
                            logger.error("call sendMsg error",e);
                            future.onMsgFail(messageId,400,e.getMessage());
                        }
                    }

                    @Override
                    public void onFail(int messageId, int status, String statusDetail) {
                        logger.error("call sendMsg fail",statusDetail);
                        future.onMsgFail(messageId,status,statusDetail);
                    }
                });
                connection.callProfileAppService(remotePubKeyHex,DefaultServices.PROFILE_PAIRING.getName(),true,callFuture);
            }
            // todo: here i have to add the pair request db and tick this as done. and save the profile with paired true.
            profilesManager.updatePaired(publicKey, ProfileInformationImp.PairStatus.PAIRED);
            pairingRequestsManager.updateStatus(remotePubKeyHex,senderHexPublicKey,PairingMsgTypes.PAIR_ACCEPT);
            // requestsDbManager.removeRequest(remotePubKeyHex);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private ProfileServerConfigurations createEmptyProfileServerConf(){
        return context.createProfSerConfig();
    }

    private RedtoothProfileConnection getProfileConnection(String profPubKey){
        if (!managers.containsKey(profPubKey)) throw new IllegalStateException("Profile connection not established");
        return managers.get(profPubKey);
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

}
