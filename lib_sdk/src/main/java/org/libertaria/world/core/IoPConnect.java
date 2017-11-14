package org.libertaria.world.core;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.google.protobuf.ByteString;

import org.libertaria.world.connection.DeviceNetworkConnection;
import org.libertaria.world.connection.ReconnectionManager;
import org.libertaria.world.core.exceptions.ConnectionAlreadyInitializedException;
import org.libertaria.world.core.exceptions.ProfileNotConectedException;
import org.libertaria.world.core.services.pairing.PairingAppService;
import org.libertaria.world.crypto.Crypto;
import org.libertaria.world.crypto.CryptoBytes;
import org.libertaria.world.crypto.CryptoWrapper;
import org.libertaria.world.global.DeviceLocation;
import org.libertaria.world.global.Version;
import org.libertaria.world.locnet.Explorer;
import org.libertaria.world.profile_server.CantConnectException;
import org.libertaria.world.profile_server.CantSendMessageException;
import org.libertaria.world.profile_server.ProfileInformation;
import org.libertaria.world.profile_server.ProfileServerConfigurations;
import org.libertaria.world.profile_server.SslContextFactory;
import org.libertaria.world.profile_server.engine.MessageQueueManager;
import org.libertaria.world.profile_server.engine.app_services.AppService;
import org.libertaria.world.profile_server.engine.app_services.BaseMsg;
import org.libertaria.world.profile_server.engine.app_services.CallProfileAppService;
import org.libertaria.world.profile_server.engine.futures.BaseMsgFuture;
import org.libertaria.world.profile_server.engine.futures.ConnectionFuture;
import org.libertaria.world.profile_server.engine.futures.MsgListenerFuture;
import org.libertaria.world.profile_server.engine.listeners.ConnectionListener;
import org.libertaria.world.profile_server.engine.listeners.EngineListener;
import org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener;
import org.libertaria.world.profile_server.imp.ProfileInformationImp;
import org.libertaria.world.profile_server.model.KeyEd25519;
import org.libertaria.world.profile_server.model.ProfServerData;
import org.libertaria.world.profile_server.model.Profile;
import org.libertaria.world.profile_server.protocol.IopProfileServer;
import org.libertaria.world.profiles_manager.LocalProfilesDao;
import org.libertaria.world.profiles_manager.PairingRequestsManager;
import org.libertaria.world.profiles_manager.ProfileOuterClass;
import org.libertaria.world.profiles_manager.ProfilesManager;
import org.libertaria.world.services.EnabledServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.FutureTask;

/**
 * Created by mati on 17/05/17.
 * todo: clase encargada de crear perfiles, agregar aplication services y hablar con las capas superiores.
 */

public class IoPConnect implements ConnectionListener {

    private final Logger logger = LoggerFactory.getLogger(IoPConnect.class);

    /**
     * Reconnect time in seconds
     */
    private static final long RECONNECT_TIME = 15;

    /**
     * Map of local device profiles pubKey connected to the home PS, profile public key -> host PS manager
     */
    private ConcurrentMap<String, IoPProfileConnection> managers;
    /**
     * Map of device profiles connected to remote PS
     */
    private ConcurrentMap<PsKey, IoPProfileConnection> remoteManagers = new ConcurrentHashMap<>();
    /**
     * Cached local profiles
     */
    private Map<String, Profile> localProfiles = new HashMap<>();
    /**
     * Enviroment context
     */
    private IoPConnectContext context;
    /**
     * Local profiles db
     */
    private LocalProfilesDao localProfilesDao;
    /**
     * Profiles manager db
     */
    private ProfilesManager profilesManager;
    /**
     * Pairing request manager db
     */
    private PairingRequestsManager pairingRequestsManager;
    /**
     * Device network connection
     */
    private DeviceNetworkConnection deviceNetworkConnection;
    /**
     * Gps
     */
    private DeviceLocation deviceLocation;
    /**
     * Crypto platform implementation
     */
    private CryptoWrapper cryptoWrapper;
    /**
     * Socket factory
     */
    private SslContextFactory sslContextFactory;
    private EngineListener engineListener;
    private final ReconnectionManager reconnectionManager;
    private final MessageQueueManager messageQueueManager;

    private class PsKey {

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

    public IoPConnect(IoPConnectContext contextWrapper,
                      CryptoWrapper cryptoWrapper,
                      SslContextFactory sslContextFactory,
                      LocalProfilesDao localProfilesDao,
                      ProfilesManager profilesManager,
                      PairingRequestsManager pairingRequestsManager,
                      DeviceLocation deviceLocation,
                      DeviceNetworkConnection deviceNetworkConnection,
                      MessageQueueManager messageQueueManager) {
        this.context = contextWrapper;
        this.cryptoWrapper = cryptoWrapper;
        this.sslContextFactory = sslContextFactory;
        this.managers = new ConcurrentHashMap<>();
        this.profilesManager = profilesManager;
        this.localProfilesDao = localProfilesDao;
        this.pairingRequestsManager = pairingRequestsManager;
        this.deviceLocation = deviceLocation;
        this.deviceNetworkConnection = deviceNetworkConnection;
        this.reconnectionManager = new ReconnectionManager();
        this.messageQueueManager = messageQueueManager;
    }

    public void setEngineListener(EngineListener engineListener) {
        this.engineListener = engineListener;
    }

    /**
     * Init
     */
    public void start() {
        for (Profile profile : this.localProfilesDao.list()) {
            // add app services
            for (String service : profile.getAppServices()) {
                if (engineListener != null) {
                    if (!service.equals("prof_pair"))
                        profile.addApplicationService(engineListener.appServiceInitializer(service));
                }
            }
            localProfiles.put(profile.getHexPublicKey(), profile);
        }
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
        profileServerConfigurations.setProfileRegistered(host, CryptoBytes.toHexString(contract.getIdentityPublicKey().toByteArray()));
    }

    @Override
    public void onNonClConnectionStablished(String host) {

    }

    @Override
    public void onConnectionLost(final Profile localProfile, final String psHost, final IopProfileServer.ServerRoleType portType, final String callId, final String tokenId) {
        if (deviceNetworkConnection.isConnected()) {
            if (managers.containsKey(localProfile.getHexPublicKey())) {
                // The connection is one of the connections to the Home server
                // Let's check now if this is the main connection
                if (portType == IopProfileServer.ServerRoleType.CL_CUSTOMER) {
                    try {
                        managers.remove(localProfile.getHexPublicKey()).stop();
                    } catch (Exception e) {
                        // remove connection
                        e.printStackTrace();
                    }
                    // todo: notify the disconnection from the main PS to the upper layer..
                    if (engineListener != null)
                        engineListener.onDisconnect(localProfile.getHexPublicKey());
                    // if the main connection is out we try to reconnect on a fixed period for now. (Later should increase the reconnection time exponentially..)
                    final ConnectionFuture connectionFuture = new ConnectionFuture();
                    connectionFuture.setListener(new BaseMsgFuture.Listener<Boolean>() {
                        @Override
                        public void onAction(int messageId, Boolean object) {
                            logger.info("Main home host connected again!");
                            //todo: launch notification to the users
                            if (engineListener != null)
                                engineListener.onCheckInCompleted(localProfile.getHexPublicKey());
                        }

                        @Override
                        public void onFail(int messageId, int status, String statusDetail) {
                            logger.info("Main home host reconnected fail");
                            if (engineListener != null) {
                                engineListener.onDisconnect(localProfile.getHexPublicKey());
                            }
                            logger.warn("reconnection fail and the engine listener is null.. please check this..");
                            reconnectionManager.scheduleReconnection(localProfile, psHost, portType, callId, tokenId, IoPConnect.this);
                            logger.info("Reconnection scheduled in: {} seconds", reconnectionManager.getCurrentWaitingTime());
                        }
                    });
                    try {
                        connectProfile(
                                localProfile.getHexPublicKey(),
                                null,
                                connectionFuture
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                        reconnectionManager.scheduleReconnection(localProfile, psHost, portType, callId, tokenId, IoPConnect.this);
                        logger.info("Reconnection scheduled in: {} seconds", reconnectionManager.getCurrentWaitingTime());
                    }
                }
            }
        } else {
            /*
              If we are not connected to internet then let's retry in a while...
             */
            reconnectionManager.scheduleReconnection(localProfile, psHost, portType, callId, tokenId, IoPConnect.this);
            logger.info("Reconnection scheduled in: {} seconds", reconnectionManager.getCurrentWaitingTime());
        }
    }


    /**
     * Create a profile inside the redtooth
     *
     * @param profileOwnerChallenge -> the owner of the profile must sign his messages
     * @param name
     * @param type
     * @param extraData
     * @param secretPassword        -> encription password for the profile keys
     * @return profile pubKey
     */
    public Profile createProfile(byte[] profileOwnerChallenge, String name, String type, byte[] img, String extraData, String secretPassword) {
        Version version = new Version((byte) 1, (byte) 0, (byte) 0);
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

        // profile basic services
        //setupProfile(profile,profileServerConfigurations);


        // start moving this to the db
        localProfilesDao.save(profile);
        localProfiles.put(profile.getHexPublicKey(), profile);
        // todo: return profile connection pk
        return profile;
    }

    private void setupProfile(Profile profile, ProfileServerConfigurations profileServerConfigurations) {
        if (!profile.containsAppService(EnabledServices.PROFILE_PAIRING.getName())) {
            // pairing default
            PairingAppService appService = new PairingAppService(
                    profile,
                    pairingRequestsManager,
                    profilesManager,
                    engineListener.initializePairing(),
                    this
            );
            String backupProfilePath = null;
            if ((backupProfilePath = profileServerConfigurations.getBackupProfilePath()) != null) {
                appService.setBackupProfile(backupProfilePath, profileServerConfigurations.getBackupPassword());
            }
            profile.addApplicationService(
                    appService
            );
        }

    }

    public Profile createProfile(Profile profile) {
        ProfileServerConfigurations profileServerConfigurations = createEmptyProfileServerConf();
        profileServerConfigurations.saveProfile(profile);
        profileServerConfigurations.saveUserKeys(profile.getKey());
        profileServerConfigurations.setIsCreated(true);
        // start moving this to the db
        localProfilesDao.save(profile);
        localProfiles.put(profile.getHexPublicKey(), profile);
        return profile;
    }

    /**
     * todo: improve this..
     *
     * @param localProfilePubKey
     * @param appService
     */
    public void addService(String localProfilePubKey, AppService appService) {
        Profile profile = localProfiles.get(localProfilePubKey);
        profile.addApplicationService(appService);
        // start moving this to the db
        localProfilesDao.updateProfile(profile);
        IoPProfileConnection connection = getProfileConnection(profile.getHexPublicKey());
        if (connection.isReady() || connection.isConnecting()) {
            connection.addApplicationService(appService);
        } else {
            throw new IllegalStateException("Connection is not ready or connecting to register a service");
        }

    }

    public void connectProfile(String profilePublicKey, byte[] ownerChallenge, ConnectionFuture future) throws Exception {
        if (!localProfiles.containsKey(profilePublicKey))
            throw new ProfileNotRegisteredException("Profile not registered " + profilePublicKey);

        Profile profile = localProfiles.get(profilePublicKey);
        // profile basic services
        setupProfile(profile, createEmptyProfileServerConf());

        if (managers.containsKey(profilePublicKey)) {
            IoPProfileConnection connection = getProfileConnection(profilePublicKey);
            if (connection.hasFail()) {
                future.setProfServerData(createEmptyProfileServerConf().getMainProfileServer());
                connection.init(future, this);
            } else if (connection.isReady()) {
                throw new ConnectionAlreadyInitializedException("Connection already initialized and running, profKey: " + profilePublicKey);
            } else {
                throw new ConnectionAlreadyInitializedException("Connection already initialized and trying to check-in the profile, profKey: " + profilePublicKey);
            }
        } else {
            ProfileServerConfigurations profileServerConfigurations = createEmptyProfileServerConf();
            ProfServerData profServerData = null;
            if (profileServerConfigurations.getMainProfileServerContract() == null) {
                // search in LOC for a profile server or use a trusted one from the user.
                // todo: here i have to do the LOC Network flow.
                // Sync explore profile servers around Argentina
                if (false) {
                    Explorer explorer = new Explorer(org.libertaria.world.locnet.NodeInfo.ServiceType.Profile, deviceLocation.getDeviceLocation(), 10000, 10);
                    FutureTask<List<org.libertaria.world.locnet.NodeInfo>> task = new FutureTask<>(explorer);
                    task.run();
                    List<org.libertaria.world.locnet.NodeInfo> resultNodes = task.get();
                    // chose the first one - closest
                    if (!resultNodes.isEmpty()) {
                        org.libertaria.world.locnet.NodeInfo selectedNode = resultNodes.get(0);
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
            profileServerConfigurations.saveMainProfileServer(profServerData);
            future.setProfServerData(profServerData);

            addConnection(profServerData, profile).init(future, this);
        }
    }

    public void updateProfile(Profile localProfile, boolean updatePs, ProfSerMsgListener<Boolean> msgListener) {
        // update db
        localProfilesDao.updateProfile(localProfile);
        if (updatePs) {
            updatePs(localProfile, msgListener);
        }
    }

    private void updatePs(Profile localProfile, ProfSerMsgListener<Boolean> msgListener) {
        IoPProfileConnection connection = getProfileConnection(localProfile.getHexPublicKey());
        if (connection != null && connection.isReady()) {
            connection.updateProfile(
                    localProfile.getVersion(),
                    localProfile.getName(),
                    localProfile.getImg(),
                    localProfile.getLatitude(),
                    localProfile.getLongitude(),
                    localProfile.getExtraData(),
                    msgListener);
        }
    }

    public void updateProfile(String localProfilePubKey, String name, byte[] img, int latitude, int longitude, String extraData, ProfSerMsgListener<Boolean> msgListener) throws Exception {
        Profile localProfile = this.localProfiles.get(localProfilePubKey);

        if (name != null && !localProfile.getName().equals(name)) {
            localProfile.setName(name);
        }

        if (img != null && !Arrays.equals(localProfile.getImg(), img)) {
            localProfile.setImg(img);
        }

        if (latitude != 0 && localProfile.getLatitude() != latitude) {
            localProfile.setLatitude(latitude);
        }

        if (latitude != 0 && localProfile.getLongitude() != longitude) {
            localProfile.setLongitude(longitude);
        }

        if (extraData != null && !localProfile.getExtraData().equals(extraData)) {
            localProfile.setExtraData(extraData);
        }

        updateProfile(localProfile, true, msgListener);
    }

    /**
     * Add PS home connection
     *
     * @param profConn
     * @param profile
     * @return
     */
    private IoPProfileConnection addConnection(ProfServerData profConn, Profile profile) {
        // profile connection
        IoPProfileConnection ioPProfileConnection = new IoPProfileConnection(
                context,
                profile,
                profConn,
                cryptoWrapper,
                sslContextFactory,
                deviceLocation,
                messageQueueManager,
                this,
                profilesManager);
        // map the profile connection with his public key
        managers.put(profile.getHexPublicKey(), ioPProfileConnection);
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
    private IoPProfileConnection addConnection(ProfServerData profConn, Profile deviceProfile, PsKey psKey) {
        // profile connection
        IoPProfileConnection ioPProfileConnection = new IoPProfileConnection(
                context,
                deviceProfile,
                profConn,
                cryptoWrapper,
                sslContextFactory,
                deviceLocation,
                messageQueueManager,
                this,
                profilesManager);
        // map the profile connection with his public key
        remoteManagers.put(psKey, ioPProfileConnection);
        return ioPProfileConnection;
    }


    /**
     * Search based on CAN, could be LOC and Profile server.
     *
     * @param requeteerPubKey
     * @param profPubKey
     * @param getInfo         -> if you are sure that you want to get the info of the profile
     * @param future
     * @throws CantConnectException
     * @throws CantSendMessageException
     */
    public void searchAndGetProfile(final String requeteerPubKey, String profPubKey, boolean getInfo, final ProfSerMsgListener<ProfileInformation> future) throws CantConnectException, CantSendMessageException {
        if (!managers.containsKey(requeteerPubKey))
            throw new IllegalStateException("Profile connection not established");
        final ProfileInformation info = profilesManager.getProfile(requeteerPubKey, profPubKey);
        if (info != null && !getInfo) {
            //todo: add TTL and expiration -> info.getLastUpdateTime().
            // if it's not valid go to CAN.
            future.onMessageReceive(0, info);
        } else {
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
                    if (info != null) {
                        profileInformation.setHomeHost(info.getHomeHost());
                        profileInformation.setProfileServerId(info.getProfileServerId());
                    }

                    for (int i = 0; i < message.getApplicationServicesCount(); i++) {
                        profileInformation.addAppService(message.getApplicationServices(i));
                    }
                    // save or update profile
                    profilesManager.saveOrUpdateProfile(requeteerPubKey, profileInformation);

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

    public void callService(
            final String serviceName,
            final String localProfilePubKey,
            final ProfileInformation remoteProfile,
            final boolean tryUpdateRemoteServices,
            final ProfSerMsgListener<CallProfileAppService> readyListener,
            final BaseMsg baseMsg,
            final boolean enqueueMessage) {
        final ProfSerMsgListener<CallProfileAppService> newListener;
        if (enqueueMessage) {
            newListener = new ProfSerMsgListener<CallProfileAppService>() {
                @Override
                public void onMessageReceive(int messageId, CallProfileAppService message) {
                    readyListener.onMessageReceive(messageId, message);
                }

                @Override
                public void onMsgFail(int messageId, int statusValue, String details) {
                    messageQueueManager.enqueueMessage(serviceName, localProfilePubKey, remoteProfile.getHexPublicKey(), baseMsg, tryUpdateRemoteServices);
                    readyListener.onMsgFail(messageId, statusValue, details);
                }

                @Override
                public String getMessageName() {
                    return readyListener.getMessageName();
                }
            };
        } else {
            newListener = readyListener;
        }
        logger.info("RunService, remote: " + remoteProfile.getHexPublicKey());
        try {
            if (!localProfiles.containsKey(localProfilePubKey))
                throw new ProfileNotRegisteredException(localProfilePubKey);
            final Profile localProfile = localProfiles.get(localProfilePubKey);
            if (!localProfile.hasService(serviceName))
                throw new IllegalStateException("App service " + serviceName + " is not enabled on local profile " + localProfilePubKey);
            final AppService appService = localProfile.getAppService(serviceName);
            // now i stablish the call if it's not exists
            final IoPProfileConnection connection = getOrStablishConnection(localProfile.getHomeHost(), localProfile.getHexPublicKey(), remoteProfile.getHomeHost());

            CallProfileAppService activeCall = connection.getActiveAppCallService(remoteProfile.getHexPublicKey());
            if (activeCall != null) {
                // call is active
                newListener.onMessageReceive(0, activeCall);
            } else {
                // first the call
                MsgListenerFuture<CallProfileAppService> callListener = new MsgListenerFuture<>();
                callListener.setListener(new BaseMsgFuture.Listener<CallProfileAppService>() {
                    @Override
                    public void onAction(int messageId, final CallProfileAppService call) {
                        try {
                            if (call.isStablished()) {
                                logger.info("call establish, remote: " + call.getRemotePubKey());
                                if (tryUpdateRemoteServices) {
                                    // update remote profile
                                    // todo: update more than just the services..
                                    profilesManager.updateRemoteServices(
                                            call.getLocalProfile().getHexPublicKey(),
                                            call.getRemotePubKey(),
                                            call.getRemoteProfile().getServices());
                                    //profilesManager.updateProfile(localProfile.getHexPublicKey(),call.getRemoteProfile());
                                }
                                appService.onCallConnected(localProfile, remoteProfile, call.isCallCreator());

                                // notify upper layers
                                newListener.onMessageReceive(messageId, call);
                            } else {
                                logger.info("call fail with status: " + call.getStatus() + ", error: " + call.getErrorStatus());
                                newListener.onMsgFail(messageId, 0, call.getStatus().toString() + " " + call.getErrorStatus());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            newListener.onMsgFail(messageId, 400, e.getMessage());
                        }
                    }

                    @Override
                    public void onFail(int messageId, int status, String statusDetail) {
                        logger.info("call fail, remote: " + statusDetail);
                        newListener.onMsgFail(messageId, status, statusDetail);
                    }
                });
                connection.callProfileAppService(remoteProfile.getHexPublicKey(), serviceName, false, true, callListener);
            }
        } catch (Exception e) {
            e.printStackTrace();
            newListener.onMsgFail(0, 400, e.getMessage());
        }
    }

    public void callService(
            final String serviceName,
            final String localProfilePubKey,
            final ProfileInformation remoteProfile,
            final boolean tryUpdateRemoteServices,
            final ProfSerMsgListener<CallProfileAppService> readyListener,
            final boolean enqueueMessage) {
        callService(serviceName, localProfilePubKey, remoteProfile, tryUpdateRemoteServices, readyListener, null, enqueueMessage);
    }

    /**
     * Call app profile service
     *
     * @param serviceName
     * @param localProfilePubKey
     * @param remoteProfile
     * @param tryUpdateRemoteServices -> param to update the profile information first and then request the call.
     * @param readyListener
     */
    public void callService(
            String serviceName,
            final String localProfilePubKey,
            final ProfileInformation remoteProfile,
            final boolean tryUpdateRemoteServices,
            final ProfSerMsgListener<CallProfileAppService> readyListener) {
        callService(serviceName, localProfilePubKey, remoteProfile, tryUpdateRemoteServices, readyListener, false);
    }

    private ProfileServerConfigurations createEmptyProfileServerConf() {
        return context.createProfSerConfig();
    }

    private IoPProfileConnection getProfileConnection(String profPubKey) throws ProfileNotConectedException {
        if (!managers.containsKey(profPubKey))
            throw new ProfileNotConectedException("Profile connection not established");
        return managers.get(profPubKey);
    }

    /**
     * If the remote ps host is the same as the home node return the main connection if not create another one to the remote server.
     *
     * @param localPsHost
     * @param localProfPubKey
     * @param remotePsHost
     * @return
     * @throws Exception
     */
    private IoPProfileConnection getOrStablishConnection(String localPsHost, String localProfPubKey, String remotePsHost) throws Exception {
        if (remotePsHost == null) throw new IllegalArgumentException("remotePsHost cannot be null");
        IoPProfileConnection connection = null;
        if (localPsHost.equals(remotePsHost)) {
            connection = getProfileConnection(localProfPubKey);
        } else {
            PsKey psKey = new PsKey(localProfPubKey, remotePsHost);
            if (remoteManagers.containsKey(psKey)) {
                connection = remoteManagers.get(psKey);
            } else {
                ProfServerData profServerData = new ProfServerData(remotePsHost);
                Profile profile = createEmptyProfileServerConf().getProfile();
                connection = addConnection(profServerData, profile, psKey);
                connection.init(this);
            }
        }
        return connection;
    }

    public List<ProfileInformation> getKnownProfiles(String pubKey) {
        return profilesManager.listConnectedProfiles(pubKey);
    }

    public ProfileInformation getKnownProfile(String contactOwnerPubKey, String pubKey) {
        return profilesManager.getProfile(contactOwnerPubKey, pubKey);
    }

    /**
     * If the profile is connected to his home node
     *
     * @return
     */
    public boolean isProfileConnectedOrConnecting(String hexProfileKey) {
        IoPProfileConnection connection = managers.get(hexProfileKey);
        if (connection != null) {
            return connection.isReady() || !connection.hasFail() || connection.isConnecting();
        } else
            return false;
    }

    public Profile getProfile(String localProfilePubKey) {
        return localProfiles.get(localProfilePubKey);
    }

    public Map<String, Profile> getLocalProfiles() {
        return localProfiles;
    }

    public AppService getProfileAppService(String localProfilePubKey, EnabledServices service) {
        return (localProfiles.containsKey(localProfilePubKey)) ?
                localProfiles.get(localProfilePubKey).getAppService(service.getName())
                :
                null;
    }

    /**
     * Backup the profile on a single encrypted file.
     * <p>
     * Including keys, connections, pairing requests
     * to be restored in other device.
     *
     * @param profile
     * @param externalFile
     */
    public synchronized void backupProfile(Profile profile, File externalFile, String password) throws IOException {
        if (!externalFile.exists()) {
            externalFile.getParentFile().mkdirs();
        } else {
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

        if (profile.getExtraData() != null) {
            mainInfo.setExtraData(profile.getExtraData());
        }
        if (profile.getImg() != null) {
            mainInfo.setImg(ByteString.copyFrom(profile.getImg()));
        }
        ProfileOuterClass.Profile.Builder mainProfile = org.libertaria.world.profiles_manager.ProfileOuterClass.Profile.newBuilder()
                .setProfileInfo(mainInfo)
                .setPrivKey(ByteString.copyFrom(profile.getPrivKey()));

        // Then connections
        List<ProfileOuterClass.ProfileInfo> remoteBackups = new ArrayList<>();
        List<ProfileInformation> profileInformationList = profilesManager.listAll(profile.getHexPublicKey());
        for (ProfileInformation profileInformation : profileInformationList) {
            ProfileOuterClass.ProfileInfo.Builder profileInfoBuilder = org.libertaria.world.profiles_manager.ProfileOuterClass.ProfileInfo.newBuilder()
                    .setName(profileInformation.getName())
                    .setPubKey(ByteString.copyFrom(CryptoBytes.fromHexToBytes(profileInformation.getHexPublicKey())));
            if (profileInformation.getVersion() != null) {
                profileInfoBuilder.setVersion(ByteString.copyFrom(profileInformation.getVersion().toByteArray()));
            }

            if (profileInformation.getHomeHost() != null) {
                profileInfoBuilder.setHomeHost(profileInformation.getHomeHost());
            }
            if (profileInformation.getType() != null) {
                profileInfoBuilder.setType(profileInformation.getType());
            }
            if (profileInformation.getExtraData() != null) {
                profileInfoBuilder.setExtraData(profile.getExtraData());
            }
            if (profileInformation.getImg() != null) {
                profileInfoBuilder.setImg(ByteString.copyFrom(profile.getImg()));
            }
            remoteBackups.add(profileInfoBuilder.build());
        }

        org.libertaria.world.profiles_manager.ProfileOuterClass.Wrapper wrapper = org.libertaria.world.profiles_manager.ProfileOuterClass.Wrapper.newBuilder()
                .setProfile(mainProfile)
                .addAllProfilesInfo(remoteBackups)
                .build();

        byte[] writeBuffer = wrapper.toByteArray();
        if (!Strings.isNullOrEmpty(password)) {
            writeBuffer = Crypto.encrypt(writeBuffer, password.toCharArray()).getBytes();
        }

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(externalFile);
            fileOutputStream.write(writeBuffer);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("backup succeed");
    }

    /**
     * todo: remove the old profile and add the new one if this methos is called from the settings.
     *
     * @param backupFile
     * @param password
     * @param platformSerializer
     * @return
     */
    public ProfileRestored restoreFromBackup(File backupFile, String password, org.libertaria.world.global.PlatformSerializer platformSerializer) throws InvalidCipherTextException {
        try (FileInputStream inputStream = new FileInputStream(backupFile)) {
            String fileContent = CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
            byte[] byteResults = fileContent.getBytes();
            if (!Strings.isNullOrEmpty(password)) {
                byteResults = Crypto.decryptBytes(fileContent, password.toCharArray());
            }
            org.libertaria.world.profiles_manager.ProfileOuterClass.Wrapper wrapper = org.libertaria.world.profiles_manager.ProfileOuterClass.Wrapper.parseFrom(byteResults);
            org.libertaria.world.profiles_manager.ProfileOuterClass.Profile mainProfile = wrapper.getProfile();
            org.libertaria.world.profiles_manager.ProfileOuterClass.ProfileInfo mainProfileInfo = mainProfile.getProfileInfo();
            byte[] versionArray = mainProfileInfo.getVersion().toByteArray();
            Profile profile = new Profile(
                    (versionArray != null && versionArray.length == 3) ? Version.fromByteArray(versionArray) : Version.newProtocolAcceptedVersion(),
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
            for (int i = 0; i < wrapper.getProfilesInfoCount(); i++) {
                org.libertaria.world.profiles_manager.ProfileOuterClass.ProfileInfo profileInfo = wrapper.getProfilesInfo(i);
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

            ProfileRestored profileRestored = new ProfileRestored(profile, list);
            logger.info("Profile restored: " + profileRestored.toString());

            // clean db
            pairingRequestsManager.truncate();
            profilesManager.truncate();

            // re start
            profilesManager.saveAllProfiles(profile.getHexPublicKey(), profileRestored.getProfileInformationList());

            return profileRestored;
        } catch (IOException e) {
            if (e.getCause().getClass().equals(InvalidCipherTextException.class)) {
                throw (InvalidCipherTextException) e.getCause();
            }
        }
        return null;
    }

    /**
     * Improve this for multiple profiles..
     *
     * @param profPubKey
     * @return
     */
    public boolean isProfileBackupScheduled(String profPubKey) {
        return createEmptyProfileServerConf().getBackupProfilePath() != null;
    }

    public static class ProfileRestored {

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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        managers.clear();
        for (Map.Entry<PsKey, IoPProfileConnection> psKeyIoPProfileConnectionEntry : remoteManagers.entrySet()) {
            try {
                psKeyIoPProfileConnectionEntry.getValue().stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        remoteManagers.clear();
    }

}
