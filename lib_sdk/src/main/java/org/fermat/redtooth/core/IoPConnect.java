package org.fermat.redtooth.core;

import com.google.protobuf.ByteString;

import org.fermat.redtooth.core.services.AppServiceListener;
import org.fermat.redtooth.global.Version;
import org.fermat.redtooth.profile_server.engine.app_services.AppServiceMsg;
import org.fermat.redtooth.profile_server.engine.listeners.EngineListener;
import org.fermat.redtooth.services.EnabledServices;
import org.fermat.redtooth.core.services.pairing.PairingAppService;
import org.fermat.redtooth.core.services.pairing.PairingMsg;
import org.fermat.redtooth.core.services.pairing.PairingMsgTypes;
import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.crypto.CryptoWrapper;
import org.fermat.redtooth.global.DeviceLocation;
import org.fermat.redtooth.global.PlatformSerializer;
import org.fermat.redtooth.locnet.Explorer;
import org.fermat.redtooth.locnet.NodeInfo;
import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.ProfileServerConfigurations;
import org.fermat.redtooth.profile_server.SslContextFactory;
import org.fermat.redtooth.profile_server.engine.app_services.AppService;
import org.fermat.redtooth.profile_server.engine.app_services.CallProfileAppService;
import org.fermat.redtooth.profile_server.engine.app_services.PairingListener;
import org.fermat.redtooth.profile_server.engine.futures.ConnectionFuture;
import org.fermat.redtooth.profile_server.engine.listeners.ConnectionListener;
import org.fermat.redtooth.profile_server.engine.futures.BaseMsgFuture;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.imp.ProfileInformationImp;
import org.fermat.redtooth.profile_server.model.KeyEd25519;
import org.fermat.redtooth.profile_server.model.ProfServerData;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;
import org.fermat.redtooth.profiles_manager.PairingRequest;
import org.fermat.redtooth.profiles_manager.PairingRequestsManager;
import org.fermat.redtooth.profiles_manager.ProfileOuterClass;
import org.fermat.redtooth.profiles_manager.ProfilesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by mati on 17/05/17.
 * todo: clase encargada de crear perfiles, agregar aplication services y hablar con las capas superiores.
 */

public class IoPConnect implements ConnectionListener {

    private final Logger logger = LoggerFactory.getLogger(IoPConnect.class);

    /** Reconnect time in seconds */
    private static final long RECONNECT_TIME = 15;

    /** Map of local device profiles pubKey connected to the home PS, profile public key -> host PS manager*/
    private ConcurrentMap<String,IoPProfileConnection> managers;
    /** Map of device profiles connected to remote PS */
    private ConcurrentMap<PsKey,IoPProfileConnection> remoteManagers = new ConcurrentHashMap<>();
    /** Enviroment context */
    private IoPConnectContext context;
    /** Profiles manager db */
    private ProfilesManager profilesManager;
    /** Pairing request manager db  */
    private PairingRequestsManager pairingRequestsManager;
    /** Gps */
    private DeviceLocation deviceLocation;
    /** Crypto platform implementation */
    private CryptoWrapper cryptoWrapper;
    /** Socket factory */
    private SslContextFactory sslContextFactory;

    private EngineListener engineListener;

    private class PsKey{

        private String deviceProfPubKey;
        private String psHost;

        public PsKey(String deviceProfPubKey, String psHost) {
            this.deviceProfPubKey = deviceProfPubKey;
            this.psHost = psHost;
        }

        public String getDeviceProfPubKey() {
            return deviceProfPubKey;
        }

        public String getPsHost() {
            return psHost;
        }
    }

    public IoPConnect(IoPConnectContext contextWrapper, CryptoWrapper cryptoWrapper, SslContextFactory sslContextFactory, ProfilesManager profilesManager, PairingRequestsManager pairingRequestsManager,DeviceLocation deviceLocation) {
        this.context = contextWrapper;
        this.cryptoWrapper = cryptoWrapper;
        this.sslContextFactory = sslContextFactory;
        this.managers = new ConcurrentHashMap<>();
        this.profilesManager = profilesManager;
        this.pairingRequestsManager = pairingRequestsManager;
        this.deviceLocation = deviceLocation;
    }

    public void setEngineListener(EngineListener engineListener) {
        this.engineListener = engineListener;
    }

    @Override
    public void onPortsReceived(String psHost, int nonClPort, int clPort, int appSerPort) {
        ProfileServerConfigurations profileServerConfigurations = createEmptyProfileServerConf();
        // todo: implement a db for profile servers..
        // But for now i'm lazy.. save this in my own profile server
        profileServerConfigurations.setMainPfClPort(clPort);
        profileServerConfigurations.setMainPsNonClPort(nonClPort);
        profileServerConfigurations.setMainAppServicePort(appSerPort);
    }

    @Override
    public void onHostingPlanReceived(String host, IopProfileServer.HostingPlanContract contract) {
        ProfileServerConfigurations profileServerConfigurations = createEmptyProfileServerConf();
        // todo: implement this for multi profiles..
        // for now i don't have to worry, i just have one profile.
        profileServerConfigurations.setProfileRegistered(host,CryptoBytes.toHexString(contract.getIdentityPublicKey().toByteArray()));
    }

    @Override
    public void onNonClConnectionStablished(String host) {

    }

    @Override
    public void onConnectionLoose(final Profile localProfile, final String psHost, final IopProfileServer.ServerRoleType portType, final String tokenId) {
        if (managers.containsKey(localProfile.getHexPublicKey())){
            // The connection is one of the connections to the Home server
            // Let's check now if this is the main connection
            if (portType == IopProfileServer.ServerRoleType.CL_CUSTOMER){
                try {
                    managers.remove(localProfile.getHexPublicKey()).stop();
                }catch (Exception e){
                    // remove connection
                    e.printStackTrace();
                }
                // todo: notify the disconnection from the main PS to the upper layer..
                if (engineListener!=null)
                    engineListener.onDisconnect(localProfile.getHexPublicKey());
                    // if the main connection is out we try to reconnect on a fixed period for now. (Later should increase the reconnection time exponentially..)
                    ConnectionFuture connectionFuture = new ConnectionFuture();
                    connectionFuture.setListener(new BaseMsgFuture.Listener<Boolean>() {
                        @Override
                        public void onAction(int messageId, Boolean object) {
                            logger.info("Main home host connected again!");
                            //todo: launch notification to the users
                            if (engineListener!=null)
                                engineListener.onCheckInCompleted(localProfile.getHexPublicKey());
                        }

                        @Override
                        public void onFail(int messageId, int status, String statusDetail) {
                            logger.info("Main home host reconnected fail");
                            //todo: try to connect again.
                            if (engineListener!=null) {
                                engineListener.onDisconnect(localProfile.getHexPublicKey());
                                // try to reconnect
                                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                                executor.schedule(new Runnable() {
                                    @Override
                                    public void run() {
                                        onConnectionLoose(localProfile,psHost,portType,tokenId);
                                    }
                                },RECONNECT_TIME,TimeUnit.SECONDS);
                                executor.shutdown();
                            }else {
                                logger.warn("reconnection fail and the engine listener is null.. please check this..");
                            }
                        }
                    });
                try {
                    PairingAppService pairingAppService = localProfile.getAppService(EnabledServices.PROFILE_PAIRING.getName(), PairingAppService.class);
                    connectProfile(
                            localProfile.getHexPublicKey(),
                            pairingAppService.getPairingListener(),
                            null,
                            connectionFuture
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.info("Problems trying to reconnect to the main PS after 5 seconds..",e);
                    // retryng after certain time.
                    try {
                        TimeUnit.SECONDS.sleep(5);
                        onConnectionLoose(localProfile,psHost,portType,tokenId);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
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
    public Profile createProfile(byte[] profileOwnerChallenge,String name,String type,byte[] img,String extraData,String secretPassword){
        Version version = new Version((byte) 1,(byte)0,(byte)0);
        ProfileServerConfigurations profileServerConfigurations = createEmptyProfileServerConf();
        KeyEd25519 keyEd25519 = profileServerConfigurations.createNewUserKeys();
        //     public Profile(Version version, String name, String type, String extraData, byte[] img, String homeHost, KeyEd25519 keyEd25519) {
        Profile profile = new Profile(
                version,
                name,
                type,
                "none",
                img,
                profileServerConfigurations.getMainProfileServer().getHost(),
                keyEd25519);
        profile.setExtraData(extraData);
        // save
        profileServerConfigurations.saveUserKeys(profile.getKey());
        profileServerConfigurations.setIsCreated(true);
        // save profile
        profileServerConfigurations.saveProfile(profile);
        // todo: return profile connection pk
        return profile;
    }

    public Profile createProfile(Profile profile){
        ProfileServerConfigurations profileServerConfigurations = createEmptyProfileServerConf();
        profileServerConfigurations.saveProfile(profile);
        profileServerConfigurations.saveUserKeys(profile.getKey());
        profileServerConfigurations.setIsCreated(true);
        return profile;
    }

    /**
     * todo: improve this..
     * @param profile
     * @param appService
     */
    public void addService(Profile profile, AppService appService) {
        profile.addApplicationService(appService);
        IoPProfileConnection connection = getProfileConnection(profile.getHexPublicKey());
        if (connection.isReady() || connection.isConnecting()){
            connection.addApplicationService(appService);
        }else {
            throw new IllegalStateException("Connection is not ready or connecting to register a service");
        }

    }

    public void connectProfile(String profilePublicKey, PairingListener pairingListener, byte[] ownerChallenge,ConnectionFuture future) throws Exception {
        if (managers.containsKey(profilePublicKey)){
            IoPProfileConnection connection = getProfileConnection(profilePublicKey);
            if (connection.hasFail()){
                future.setProfServerData(createEmptyProfileServerConf().getMainProfileServer());
                connection.init(future,this);
            }else if (connection.isReady()){
                throw new IllegalStateException("Connection already initilized and running, profKey: "+profilePublicKey);
            }else {
                throw new IllegalStateException("Connection already initilized and trying to check-in the profile, profKey: "+profilePublicKey);
            }
        }else {
            ProfileServerConfigurations profileServerConfigurations = createEmptyProfileServerConf();
            ProfServerData profServerData = null;
            if (profileServerConfigurations.getMainProfileServerContract() == null) {
                // search in LOC for a profile server or use a trusted one from the user.
                // todo: here i have to do the LOC Network flow.
                // Sync explore profile servers around Argentina
                if (false) {
                    Explorer explorer = new Explorer(NodeInfo.ServiceType.Profile, deviceLocation.getDeviceLocation(), 10000, 10);
                    FutureTask<List<NodeInfo>> task = new FutureTask<>(explorer);
                    task.run();
                    List<NodeInfo> resultNodes = task.get();
                    // chose the first one - closest
                    if (!resultNodes.isEmpty()) {
                        NodeInfo selectedNode = resultNodes.get(0);
                        profServerData = new ProfServerData(
                                selectedNode.getNodeId(),
                                selectedNode.getContact().getAddress().getHostAddress(),
                                selectedNode.getContact().getPort(),
                                selectedNode.getLocation().getLatitude(),
                                selectedNode.getLocation().getLongitude()
                        );
                    }
                } else {
                    profServerData = profileServerConfigurations.getMainProfileServer();
                }
            }
            KeyEd25519 keyEd25519 = (KeyEd25519) profileServerConfigurations.getUserKeys();
            profileServerConfigurations.saveMainProfileServer(profServerData);
            if (keyEd25519 == null) throw new IllegalStateException("no pubkey saved");
            future.setProfServerData(profServerData);
            addConnection(profileServerConfigurations, profServerData, keyEd25519, pairingListener).init(future, this);
        }
    }

    public int updateProfile(Profile profile, ProfSerMsgListener<Boolean> msgListener) throws Exception {
        IoPProfileConnection connection = getProfileConnection(profile.getHexPublicKey());
        if (connection != null && connection.isReady()) {
            return connection.updateProfile(profile.getVersion(), profile.getName(), profile.getImg(), profile.getLatitude(), profile.getLongitude(), profile.getExtraData(), msgListener);
        }else {
            throw new IllegalStateException("Main profile connection is not open");
        }

    }

    /**
     *
     * Add PS home connection
     *
     * @param profileServerConfigurations
     * @param profConn
     * @param profKey
     * @param pairingListener
     * @return
     */
    private IoPProfileConnection addConnection(ProfileServerConfigurations profileServerConfigurations,ProfServerData profConn, KeyEd25519 profKey, PairingListener pairingListener){
        // profile connection
        IoPProfileConnection ioPProfileConnection = new IoPProfileConnection(
                context,
                initClientData(profileServerConfigurations,pairingListener),
                profConn,
                cryptoWrapper,
                sslContextFactory,
                deviceLocation);
        // map the profile connection with his public key
        managers.put(profKey.getPublicKeyHex(), ioPProfileConnection);
        return ioPProfileConnection;
    }

    /**
     * Add PS guest connection looking for a remote profile.
     *
     * @param profConn
     * @param deviceProfile
     * @param psKey
     * @return
     */
    private IoPProfileConnection addConnection(ProfServerData profConn, Profile deviceProfile ,PsKey psKey){
        // profile connection
        IoPProfileConnection ioPProfileConnection = new IoPProfileConnection(
                context,
                deviceProfile,
                profConn,
                cryptoWrapper,
                sslContextFactory,
                deviceLocation);
        // map the profile connection with his public key
        remoteManagers.put(psKey, ioPProfileConnection);
        return ioPProfileConnection;
    }

    private Profile initClientData(ProfileServerConfigurations profileServerConfigurations, PairingListener pairingListener) {
        //todo: esto lo tengo que hacer cuando guarde la privkey encriptada.., por ahora lo dejo asI. Este es el profile que va a crear el usuario, está acá de ejemplo.
        Profile profile = null;
        if (profileServerConfigurations.isIdentityCreated()) {
            // load profileCache
            profile = profileServerConfigurations.getProfile();
        } else {
            // create and save
            //     public Profile(Version version, String name, String type, String extraData, byte[] img, String homeHost, KeyEd25519 keyEd25519) {

            KeyEd25519 keyEd25519 = profileServerConfigurations.createUserKeys();
            profile = profileServerConfigurations.getProfile();
            profile.setKey(keyEd25519);
            // save
            profileServerConfigurations.saveUserKeys(profile.getKey());
        }
        // pairing default
        if(profileServerConfigurations.isPairingEnable()){
            if (pairingListener==null) throw new IllegalArgumentException("Pairing listener cannot be null if configurations pairing is enabled");
            PairingAppService appService = new PairingAppService(
                    profile,
                    pairingRequestsManager,
                    profilesManager,
                    pairingListener,
                    this
            );
            String backupProfilePath = null;
            if ((backupProfilePath = profileServerConfigurations.getBackupProfilePath())!=null)
                appService.setBackupProfile(backupProfilePath,profileServerConfigurations.getBackupPassword());
            profile.addApplicationService(
                    appService
            );
        }
        return profile;
    }

    /**
     * Search based on CAN, could be LOC and Profile server.
     *
     * @param requeteerPubKey
     * @param profPubKey
     * @param getInfo -> if you are sure that you want to get the info of the profile
     * @param future
     * @throws CantConnectException
     * @throws CantSendMessageException
     */
    public void searchAndGetProfile(final String requeteerPubKey, String profPubKey, boolean getInfo ,final ProfSerMsgListener<ProfileInformation> future) throws CantConnectException, CantSendMessageException {
        if (!managers.containsKey(requeteerPubKey)) throw new IllegalStateException("Profile connection not established");
        final ProfileInformation info = profilesManager.getProfile(requeteerPubKey,profPubKey);
        if (info!=null && !getInfo){
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
                    profileInformation.setVersion(Version.fromByteArray(signedProfile.getVersion().toByteArray()));
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
                    //todo: improve with input flags and not just hardcoded
                    profileInformation.setImg(message.getProfileImage().toByteArray());
                    profileInformation.setThumbnailImg(message.getThumbnailImage().toByteArray());
                    if (info!=null) {
                        profileInformation.setHomeHost(info.getHomeHost());
                        profileInformation.setProfileServerId(info.getProfileServerId());
                    }

                    for (int i = 0; i < message.getApplicationServicesCount(); i++) {
                        profileInformation.addAppService(message.getApplicationServices(i));
                    }
                    // save or update profile
                    profilesManager.saveOrUpdateProfile(requeteerPubKey,profileInformation);

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
     * Call app profile service
     * @param serviceName
     * @param localProfile
     * @param remoteProfile
     * @param tryUpdateRemoteServices -> param to update the profile information first and then request the call.
     * @param readyListener
     * @param args
     */
    public void callService(String serviceName, final Profile localProfile, final ProfileInformation remoteProfile, final boolean tryUpdateRemoteServices , final ProfSerMsgListener<Boolean> readyListener , Object... args) {
        logger.info("RunService, remote: " + remoteProfile.getHexPublicKey());
        try {
            final AppService appService = localProfile.getAppService(serviceName);
            appService.onPreCall();
            // now i stablish the call if it's not exists
            final IoPProfileConnection connection = getOrStablishConnection(localProfile.getHomeHost(), localProfile.getHexPublicKey(), remoteProfile.getHomeHost());
            // first the call
            MsgListenerFuture<CallProfileAppService> callListener = new MsgListenerFuture();
            callListener.setListener(new BaseMsgFuture.Listener<CallProfileAppService>() {
                @Override
                public void onAction(int messageId, final CallProfileAppService call) {
                    try {
                        if (call.isStablished()) {
                            logger.info("call establish, remote: " + call.getRemotePubKey());
                            if (tryUpdateRemoteServices){
                                // update remote profile
                                // todo: update more than just the services..
                                profilesManager.updateRemoteServices(
                                        call.getLocalProfile().getHexPublicKey(),
                                        call.getRemotePubKey(),
                                        call.getRemoteProfile().getServices());
                                //profilesManager.updateProfile(localProfile.getHexPublicKey(),call.getRemoteProfile());
                            }
                            appService.onCallConnected(localProfile,remoteProfile,call.isCallCreator());
                        } else {
                            logger.info("call fail with status: " + call.getStatus() + ", error: " + call.getErrorStatus());
                            readyListener.onMsgFail(messageId, 0, call.getStatus().toString() + " " + call.getErrorStatus());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        readyListener.onMsgFail(messageId, 400, e.getMessage());
                    }
                }

                @Override
                public void onFail(int messageId, int status, String statusDetail) {
                    logger.info("call fail, remote: " + statusDetail);
                    readyListener.onMsgFail(messageId, status, statusDetail);
                }
            });
            connection.callProfileAppService(remoteProfile.getHexPublicKey(), serviceName, false, true, callListener);
        } catch (Exception e) {
            e.printStackTrace();
            readyListener.onMsgFail(0,400,e.getMessage());
        }
    }

    /**
     * Send a request pair notification to a remote profile
     *
     * @param pairingRequest
     * @param listener -> returns the pairing request id
     */
    public void requestPairingProfile(final PairingRequest pairingRequest, final ProfSerMsgListener<ProfileInformation> listener) throws Exception {
        logger.info("requestPairingProfile, remote: " + pairingRequest.getRemotePubKey());
        // save request
        final int pairingRequestId = pairingRequestsManager.savePairingRequest(pairingRequest);
        pairingRequest.setId(pairingRequestId);
        // Connection
        final IoPProfileConnection connection = getOrStablishConnection(pairingRequest.getRemotePsHost(),pairingRequest.getSenderPubKey(),pairingRequest.getSenderPsHost());
        // first the call
        MsgListenerFuture<CallProfileAppService> callListener = new MsgListenerFuture();
        callListener.setListener(new BaseMsgFuture.Listener<CallProfileAppService>() {
            @Override
            public void onAction(int messageId, final CallProfileAppService call) {
                try {
                    if (call.isStablished()) {
                        logger.info("call establish, remote: " + call.getRemotePubKey());
                        // now send the pairing message
                        MsgListenerFuture<Boolean> pairingMsgFuture = new MsgListenerFuture();
                        pairingMsgFuture.setListener(new BaseMsgFuture.Listener<Boolean>() {
                            @Override
                            public void onAction(int messageId, Boolean res) {
                                logger.info("pairing msg sent, remote: " + call.getRemotePubKey());
                                listener.onMessageReceive(messageId, call.getRemoteProfile());
                            }

                            @Override
                            public void onFail(int messageId, int status, String statusDetail) {
                                logger.info("pairing msg fail, remote: " + call.getRemotePubKey());
                                listener.onMsgFail(messageId, status, statusDetail);
                            }
                        });
                        PairingMsg pairingMsg = new PairingMsg(pairingRequest.getSenderName(),pairingRequest.getSenderPsHost());
                        call.sendMsg(pairingMsg, pairingMsgFuture);
                    } else {
                        logger.info("call fail with status: " + call.getStatus() + ", error: " + call.getErrorStatus());
                        listener.onMsgFail(messageId, 0, call.getStatus().toString() + " " + call.getErrorStatus());
                    }

                } catch (CantSendMessageException e) {
                    e.printStackTrace();
                    listener.onMsgFail(messageId, 400, e.getMessage());
                } catch (CantConnectException e) {
                    e.printStackTrace();
                    listener.onMsgFail(messageId, 400, e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onMsgFail(messageId, 400, e.getMessage());
                }
            }

            @Override
            public void onFail(int messageId, int status, String statusDetail) {
                logger.info("call fail, remote: " + statusDetail);
                listener.onMsgFail(messageId, status, statusDetail);
            }
        });
        connection.callProfileAppService(pairingRequest.getRemotePubKey(), EnabledServices.PROFILE_PAIRING.getName(), false, false, callListener);
    }

    /**
     * Send a pair acceptance
     *
     * @param pairingRequest
     */
    public void acceptPairingRequest(PairingRequest pairingRequest, final ProfSerMsgListener<Boolean> callback) throws Exception {
        // Remember that here the local device is the pairingRequest.getSender()
        final String remotePubKeyHex =  pairingRequest.getSenderPubKey();
        final String localPubKeyHex = pairingRequest.getRemotePubKey();
        logger.info("acceptPairingRequest, remote: " + remotePubKeyHex);
        // Notify the other side if it's connected.
        // first check if i have a connection with the server hosting the pairing sender
        // tengo que ver si el remote profile tiene como home host la conexion principal de el sender profile al PS
        // si no la tiene abro otra conexion.
        final IoPProfileConnection connection = getOrStablishConnection(pairingRequest.getRemotePsHost(),pairingRequest.getRemotePubKey(),pairingRequest.getSenderPsHost());
        final CallProfileAppService call = connection.getActiveAppCallService(remotePubKeyHex);
        final MsgListenerFuture<Boolean> future = new MsgListenerFuture<>();
        // Add listener -> todo: add future listener and save acceptPairing sent
        future.setListener(new BaseMsgFuture.Listener<Boolean>() {
            @Override
            public void onAction(int messageId, Boolean object) {
                logger.info("PairAccept sent");
                if (call!=null)
                    call.dispose();
                else
                    logger.warn("call null trying to dispose pairing app service. Check this");

                // update in db the acceptance
                profilesManager.updatePaired(
                        localPubKeyHex,
                        remotePubKeyHex,
                        ProfileInformationImp.PairStatus.PAIRED);
                pairingRequestsManager.updateStatus(
                        remotePubKeyHex,
                        localPubKeyHex,
                        PairingMsgTypes.PAIR_ACCEPT,
                        ProfileInformationImp.PairStatus.PAIRED
                );

                // notify
                if (callback!=null)
                    callback.onMessageReceive(messageId,object);

            }

            @Override
            public void onFail(int messageId, int status, String statusDetail) {
                logger.info("PairAccept fail, "+status+", detail: "+statusDetail);
                //todo: schedule and re try
                if (call!=null)
                    call.dispose();
                else
                    logger.warn("call null trying to dispose pairing app service. Check this");

                // notify
                if (callback!=null)
                    callback.onMsgFail(messageId,status,statusDetail);
            }
        });
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
            connection.callProfileAppService(remotePubKeyHex, EnabledServices.PROFILE_PAIRING.getName(),true,false,callFuture);
        }
    }

    public void cancelPairingRequest(PairingRequest pairingRequest) {
        pairingRequestsManager.delete(pairingRequest.getId());
    }


    private ProfileServerConfigurations createEmptyProfileServerConf(){
        return context.createProfSerConfig();
    }

    private IoPProfileConnection getProfileConnection(String profPubKey) throws org.fermat.redtooth.core.exceptions.ProfileNotConectedException {
        if (!managers.containsKey(profPubKey)) throw new org.fermat.redtooth.core.exceptions.ProfileNotConectedException("Profile connection not established");
        return managers.get(profPubKey);
    }

    /**
     *
     * If the remote ps host is the same as the home node return the main connection if not create another one to the remote server.
     *
     * @param localPsHost
     * @param localProfPubKey
     * @param remotePsHost
     * @return
     * @throws Exception
     */
    private IoPProfileConnection getOrStablishConnection(String localPsHost,String localProfPubKey,String remotePsHost) throws Exception {
        if (remotePsHost==null) throw new IllegalArgumentException("remotePsHost cannot be null");
        IoPProfileConnection connection = null;
        if (localPsHost.equals(remotePsHost)) {
            connection = getProfileConnection(localProfPubKey);
        }else {
            PsKey psKey = new PsKey(localProfPubKey,remotePsHost);
            if(remoteManagers.containsKey(psKey)){
                connection = remoteManagers.get(psKey);
            }else {
                ProfServerData profServerData = new ProfServerData(remotePsHost);
                Profile profile = createEmptyProfileServerConf().getProfile();
                connection = addConnection(profServerData,profile,psKey);
                connection.init(this);
            }
        }
        return connection;
    }

    public List<ProfileInformation> getKnownProfiles(String pubKey){
        return profilesManager.listConnectedProfiles(pubKey);
    }

    public ProfileInformation getKnownProfile(String contactOwnerPubKey,String pubKey) {
        return profilesManager.getProfile(contactOwnerPubKey,pubKey);
    }

    /**
     * If the profile is connected to his home node
     * @return
     */
    public boolean isProfileConnectedOrConnecting(String hexProfileKey){
        IoPProfileConnection connection = managers.get(hexProfileKey);
        if (connection!=null){
            return connection.isReady() || !connection.hasFail() || connection.isConnecting();
        }else
            return false;
    }

    /**
     * Backup the profile on a single encrypted file.
     *
     * Including keys, connections, pairing requests
     * to be restored in other device.
     *
     * @param profile
     * @param externalFile
     */
    public synchronized void backupProfile(Profile profile,File externalFile,String password) throws IOException {
        if (!externalFile.exists()){
            externalFile.getParentFile().mkdirs();
        }else {
            externalFile.delete();
        }
        externalFile.createNewFile();
        // The file is going to be built in this way:
        // First the main profile
        ProfileOuterClass.ProfileInfo.Builder mainInfo = ProfileOuterClass.ProfileInfo.newBuilder()
                .setVersion(ByteString.copyFrom(profile.getVersion().toByteArray()))
                .setName(profile.getName())
                .setType(profile.getType())
                .setHomeHost(profile.getHomeHost())
                .setPubKey(ByteString.copyFrom(profile.getPublicKey()));

        if (profile.getExtraData()!=null){
            mainInfo.setExtraData(profile.getExtraData());
        }
        if (profile.getImg()!=null){
            mainInfo.setImg(ByteString.copyFrom(profile.getImg()));
        }
        ProfileOuterClass.Profile.Builder mainProfile = ProfileOuterClass.Profile.newBuilder()
                .setProfileInfo(mainInfo)
                .setPrivKey(ByteString.copyFrom(profile.getPrivKey()));

        // Then connections
        List<ProfileOuterClass.ProfileInfo> remoteBackups = new ArrayList<>();
        List<ProfileInformation> profileInformationList = profilesManager.listAll(profile.getHexPublicKey());
        for (ProfileInformation profileInformation : profileInformationList) {
            ProfileOuterClass.ProfileInfo.Builder profileInfoBuilder = ProfileOuterClass.ProfileInfo.newBuilder()
                    .setName(profileInformation.getName())
                    .setPubKey(ByteString.copyFrom(CryptoBytes.fromHexToBytes(profileInformation.getHexPublicKey())))
                    ;
            if (profileInformation.getVersion()!=null){
                profileInfoBuilder.setVersion(ByteString.copyFrom(profileInformation.getVersion().toByteArray()));
            }

            if (profileInformation.getHomeHost()!=null){
                profileInfoBuilder.setHomeHost(profileInformation.getHomeHost());
            }
            if (profileInformation.getType()!=null){
                profileInfoBuilder.setType(profileInformation.getType());
            }
            if (profileInformation.getExtraData()!=null){
                profileInfoBuilder.setExtraData(profile.getExtraData());
            }
            if (profileInformation.getImg()!=null){
                profileInfoBuilder.setImg(ByteString.copyFrom(profile.getImg()));
            }
            remoteBackups.add(profileInfoBuilder.build());
        }

        ProfileOuterClass.Wrapper wrapper = ProfileOuterClass.Wrapper.newBuilder()
                .setProfile(mainProfile)
                .addAllProfilesInfo(remoteBackups)
                .build();

        byte[] writeBuffer = wrapper.toByteArray();
        // todo: encrypt this with the password..

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(externalFile);
            fileOutputStream.write(writeBuffer);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("backup succed");
    }

    /**
     * Todo: decrypt with the password this..
     * todo: remove the old profile and add the new one if this methos is called from the settings.
     * @param backupFile
     * @param password
     * @param platformSerializer
     * @return
     */
    public ProfileRestored restoreFromBackup(File backupFile, String password, PlatformSerializer platformSerializer){
        try {
            FileInputStream inputStream = new FileInputStream(backupFile);
            ProfileOuterClass.Wrapper wrapper = ProfileOuterClass.Wrapper.parseFrom(inputStream);
            inputStream.close();
            ProfileOuterClass.Profile mainProfile = wrapper.getProfile();
            ProfileOuterClass.ProfileInfo mainProfileInfo = mainProfile.getProfileInfo();
            byte[] versionArray = mainProfileInfo.getVersion().toByteArray();
            Profile profile = new Profile(
                    (versionArray!=null && versionArray.length==3)?Version.fromByteArray(versionArray):Version.newProtocolAcceptedVersion(),
                    mainProfileInfo.getName(),
                    mainProfileInfo.getType(),
                    mainProfileInfo.getExtraData(),
                    mainProfileInfo.getImg().toByteArray(),
                    mainProfileInfo.getHomeHost(),
                    platformSerializer.toPlatformKey(
                            mainProfile.getPrivKey().toByteArray(),
                            mainProfileInfo.getPubKey().toByteArray()
                    )
            );
            List<ProfileInformation> list = new ArrayList<>();
            for (int i=0;i<wrapper.getProfilesInfoCount();i++){
                ProfileOuterClass.ProfileInfo profileInfo = wrapper.getProfilesInfo(i);
                ProfileInformation profileInformation = new ProfileInformationImp(
                        Version.fromByteArray(profileInfo.getVersion().toByteArray()),
                        profileInfo.getPubKey().toByteArray(),
                        profileInfo.getName(),
                        profileInfo.getType(),
                        profileInfo.getExtraData(),
                        profileInfo.getImg().toByteArray(),
                        profileInfo.getHomeHost()
                );
                profileInformation.setPairStatus(ProfileInformationImp.PairStatus.PAIRED);
                list.add(
                        profileInformation
                );
            }

            ProfileRestored profileRestored = new ProfileRestored(profile,list);
            logger.info("Profile restored: "+profileRestored.toString());

            // clean db
            pairingRequestsManager.truncate();
            profilesManager.truncate();

            // re start
            profilesManager.saveAllProfiles(profile.getHexPublicKey(),profileRestored.getProfileInformationList());

            return profileRestored;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Improve this for multiple profiles..
     * @param profPubKey
     * @return
     */
    public boolean isProfileBackupScheduled(String profPubKey) {
        return createEmptyProfileServerConf().getBackupProfilePath()!=null;
    }

    public static class ProfileRestored{

        private Profile profile;
        private List<ProfileInformation> profileInformationList;

        public ProfileRestored(Profile profile, List<ProfileInformation> profileInformationList) {
            this.profile = profile;
            this.profileInformationList = profileInformationList;
        }

        public Profile getProfile() {
            return profile;
        }

        public List<ProfileInformation> getProfileInformationList() {
            return profileInformationList;
        }

        @Override
        public String toString() {
            return "ProfileRestored{" +
                    "profile=" + profile +
                    ", profileInformationList=" + Arrays.toString(profileInformationList.toArray()) +
                    '}';
        }
    }

    /**
     * Stop every single profile connection.
     */
    public void stop() {
        for (Map.Entry<String, IoPProfileConnection> stringRedtoothProfileConnectionEntry : managers.entrySet()) {
            try {
                stringRedtoothProfileConnectionEntry.getValue().stop();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        managers.clear();
        for (Map.Entry<PsKey, IoPProfileConnection> psKeyIoPProfileConnectionEntry : remoteManagers.entrySet()) {
            try {
                psKeyIoPProfileConnectionEntry.getValue().stop();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        remoteManagers.clear();
    }


}
