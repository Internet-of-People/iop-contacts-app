package org.libertaria.world.profile_server.engine;

import org.bitcoinj.core.Sha256Hash;
import org.libertaria.world.profile_server.CantSendMessageException;
import org.libertaria.world.profile_server.ProfileInformation;
import org.libertaria.world.profile_server.protocol.IopProfileServer;
import org.libertaria.world.profile_server.engine.app_services.AppServiceMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by mati on 05/02/17.
 *
 *
 * Esta clase va a ser el engine de conexión con el profile server, abstrayendo a los usuarios de su conexión.
 *
 * Por ahora el servidor solo acepta un actor por conexión por lo cual deberia tener el profile acá o armar una lista de profiles para un futuro..
 *
 */
public class ProfSerEngine {

    private Logger LOG = LoggerFactory.getLogger(ProfSerEngine.class);
    /** Connection state */
    private ProfSerConnectionState profSerConnectionState;
    /**  Profile server */
    private org.libertaria.world.profile_server.client.ProfileServer profileServer;
    /** Server configuration data */
    private org.libertaria.world.profile_server.model.ProfServerData profServerData;
    /** Profile connected cached class */
    private org.libertaria.world.profile_server.client.ProfNodeConnection profNodeConnection;
    /** Crypto wrapper implementation */
    private org.libertaria.world.crypto.CryptoWrapper crypto;
    /** Internal server handler */
    private org.libertaria.world.profile_server.client.PsSocketHandler handler;
    /** Connection listeners */
    private CopyOnWriteArrayList<org.libertaria.world.profile_server.engine.listeners.ConnectionListener> connectionListener = new CopyOnWriteArrayList<>();
    /** Listener to receive incomingCallNotifications and incomingMessages from calls */
    private org.libertaria.world.profile_server.engine.app_services.CallsListener callListener;
    /** Messages listeners:  id -> listner */
    private final ConcurrentMap<Integer, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener> msgListeners = new ConcurrentHashMap<>();
    private final ConcurrentMap<String,SearchProfilesQuery> profilesQuery = new ConcurrentHashMap<>();
    /** Executor */
    private ExecutorService executor;
    /** Ping executor */
    private Map<IopProfileServer.ServerRoleType,ScheduledExecutorService> pingExecutors;

    /**
     *
     * @param contextWrapper
     * @param profServerData -> server data
     * @param profile -> profile data
     * @param crypto
     * @param sslContextFactory
     */
    public ProfSerEngine(org.libertaria.world.core.IoPConnectContext contextWrapper, org.libertaria.world.profile_server.model.ProfServerData profServerData, org.libertaria.world.profile_server.model.Profile profile, org.libertaria.world.crypto.CryptoWrapper crypto, org.libertaria.world.profile_server.SslContextFactory sslContextFactory) {
        this.profServerData = profServerData;
        this.crypto = crypto;
        this.profSerConnectionState= ProfSerConnectionState.NO_SERVER;
        this.profNodeConnection = new org.libertaria.world.profile_server.client.ProfNodeConnection(
                profile,
                profServerData.isRegistered(),
                profServerData.isHome(),
                randomChallenge()
        );
        handler = new ProfileServerHandler();
        this.profileServer = new org.libertaria.world.profile_server.client.ProfSerImp(contextWrapper,profServerData,sslContextFactory,handler);
    }

    /**
     * Creates a random challenge for the connection.
     * @return
     */
    private byte[] randomChallenge(){
        byte[] connChallenge = new byte[32];
        crypto.random(connChallenge,32);
        return connChallenge;
    }

    public void setCallListener(org.libertaria.world.profile_server.engine.app_services.CallsListener callListener) {
        this.callListener = callListener;
    }

    public void addConnectionListener(org.libertaria.world.profile_server.engine.listeners.ConnectionListener listener){
        this.connectionListener.add(listener);
    }

    /**
     * Start
     * @param initFuture
     */
    public void start(final org.libertaria.world.profile_server.engine.futures.MsgListenerFuture<Boolean> initFuture){
        if (getProfSerConnectionState()!= ProfSerConnectionState.NO_SERVER) throw new IllegalStateException("Start already called");
        executor = Executors.newFixedThreadPool(3);
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    ProfSerConnectionEngine connectionEngine = new ProfSerConnectionEngine(ProfSerEngine.this, initFuture);
                    connectionEngine.engine();
                }catch (Exception e){
                    LOG.error("Connection engine fail",e);
                    initFuture.onMsgFail(0,400,e.getMessage());
                }
            }
        });
    }

    /**
     * Stop
     */
    public void stop(){
        executor.shutdown();
        executor = null;
        try {
            if (pingExecutors!=null) {
                for (ScheduledExecutorService service : pingExecutors.values()) {
                    service.shutdownNow();
                }
                pingExecutors.clear();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        try {
            profileServer.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void addMsgListener(int msgId, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener listener){
        msgListeners.put(msgId,listener);
    }

    private void sendRequest(org.libertaria.world.profile_server.client.ProfSerRequest profSerRequest, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener listener) throws org.libertaria.world.profile_server.CantConnectException, CantSendMessageException {
        if (listener!=null)
            addMsgListener(profSerRequest.getMessageId(),listener);
        profSerRequest.send();
    }

    /**
     * Public methods
     */

    /**
     * Request the list of ports available
     * @param listener
     * @return
     * @throws Exception
     */
    public int requestRoleList(org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener listener) throws Exception {
        LOG.info("requestRoleList");
        org.libertaria.world.profile_server.client.ProfSerRequest request = profileServer.listRolesRequest();
        sendRequest(request,listener);
        return request.getMessageId();
    }

    /**
     *
     * @param listener
     * @return
     * @throws Exception
     */
    int startConversationNonCl(org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener listener) throws Exception{
        LOG.info("startConversationNonCl");
        org.libertaria.world.profile_server.client.ProfSerRequest profSerRequest = profileServer.startConversationNonCl(
                profNodeConnection.getProfile().getPublicKey(),
                profNodeConnection.getConnectionChallenge()
        );
        sendRequest(profSerRequest,listener);
        return profSerRequest.getMessageId();
    }

    /**
     * Register the profile on the server
     *
     * Need to stablish a non customer connection first
     *
     * @param profile
     * @param listener
     * @return
     * @throws Exception
     */
    public int registerProfileRequest(org.libertaria.world.profile_server.model.Profile profile, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener listener) throws Exception {
        LOG.info("registerProfileRequest");
        org.libertaria.world.profile_server.client.ProfSerRequest profSerRequest = profileServer.registerHostRequest(
                profile,
                profile.getPublicKey(),
                profile.getType()
        );
        sendRequest(profSerRequest,listener);
        return profSerRequest.getMessageId();
    }

    /**
     *
     * @param listener
     * @return
     * @throws Exception
     */
    public int startConversationCl(org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener listener) throws Exception {
        org.libertaria.world.profile_server.client.ProfSerRequest profSerRequest = profileServer.startConversationCl(
                profNodeConnection.getProfile().getPublicKey(),
                profNodeConnection.getConnectionChallenge()
        );
        sendRequest(profSerRequest,listener);
        return profSerRequest.getMessageId();
    }

    /**
     * Check in request
     *
     * @param nodeChallenge
     * @param profile
     * @param listener
     * @return
     * @throws Exception
     */
    public int checkinRequest(byte[] nodeChallenge, org.libertaria.world.profile_server.model.Profile profile, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener listener) throws Exception {
        LOG.info("check-in for pk: "+profile.getHexPublicKey());
        org.libertaria.world.profile_server.client.ProfSerRequest request = profileServer.checkIn(nodeChallenge, profile);
        sendRequest(request,listener);
        return request.getMessageId();
    }

    /**
     * Update the existing profile in the server
     *
     * @param img -> Profile image in PNG or JPEG format, non-empty binary data, max 20,480 bytes long, or zero length binary data if the profile image is about to be erased.
     */
    public int updateProfile(org.libertaria.world.global.Version version, String name, byte[] img, byte[] imgHash, int lat, int lon, String extraData, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener listener){
        LOG.info("updateProfile, state: "+profSerConnectionState);
        int msgId = 0;
        if (profSerConnectionState == ProfSerConnectionState.CHECK_IN){
            try{
                org.libertaria.world.profile_server.client.ProfSerRequest profSerRequest = profileServer.updateProfileRequest(
                        profNodeConnection.getProfile(),
                        profNodeConnection.getProfile().getPublicKey(),
                        profNodeConnection.getProfile().getType(),
                        version.toByteArray(),
                        name,
                        img,
                        imgHash,
                        lat,
                        lon,
                        extraData
                );
                msgId = profSerRequest.getMessageId();
                sendRequest(profSerRequest,listener);
            }catch (Exception e){
                LOG.error("updateProfileException",e);
            }
        }else {
            throw new IllegalStateException("Profile server not ready to use.");
        }
        return msgId;
    }

    /**
     * Store can profile
      * @return
     */
    public int storeCanProfile(org.libertaria.world.profile_server.model.Profile profile, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener listener) throws org.libertaria.world.profile_server.CantConnectException, CantSendMessageException {
        org.libertaria.world.profile_server.protocol.CanStoreMap canStoreMap = new org.libertaria.world.profile_server.protocol.CanStoreMap();
        // todo: complete this when after check the PS.
        canStoreMap.addValue("pubKey",profile.getPublicKey());
        org.libertaria.world.profile_server.client.ProfSerRequest request = profileServer.storeCanDataRequest(canStoreMap);
        sendRequest(request,listener);
        return request.getMessageId();
    }


    /**
     * Add application service
     *
     * @param appService
     * @return
     */
    public int addApplicationService(org.libertaria.world.profile_server.engine.app_services.AppService appService){
        LOG.info("addApplicationService, "+appService);
        profNodeConnection.getProfile().addApplicationService(appService);
        // If the connection is stablished sent the message, if not the profile is going to be registered once the register engine finishes.
        if (profSerConnectionState== ProfSerConnectionState.CHECK_IN)
            return addApplicationServiceRequest(appService.getName(),appService);
        else
            return 0;
    }

    /**
     * //todo: hace falta dividir la lectura de la escritura.
     * @param name
     * @param listener
     */
    public void searchProfileByName(String name, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<List<IopProfileServer.ProfileQueryInformation>> listener){
        try {
            org.libertaria.world.profile_server.client.ProfSerRequest request = profileServer.searchProfilesRequest(false,false,100,10000,null,name,null);
            sendRequest(request,listener);
        } catch (org.libertaria.world.profile_server.CantConnectException e) {
            e.printStackTrace();
        } catch (CantSendMessageException e) {
            e.printStackTrace();
        }
    }

    public void searchProfileByNameAndType(String name,String type, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<List<IopProfileServer.ProfileQueryInformation>> listener){
        try {
            org.libertaria.world.profile_server.client.ProfSerRequest request = profileServer.searchProfilesRequest(false,false,100,10000,type,name,null);
            sendRequest(request,listener);
        } catch (org.libertaria.world.profile_server.CantConnectException e) {
            e.printStackTrace();
        } catch (CantSendMessageException e) {
            e.printStackTrace();
        }
    }

    /**
     * todo: fijarse si a este searchProfilesQuery le deberia poner un id para busquedas siguientes..
     * // todo: ver tema de los covered servers..
     *
     * @param searchProfilesQuery
     * @param listener
     */
    public void searchProfiles(SearchProfilesQuery searchProfilesQuery, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<List<IopProfileServer.ProfileQueryInformation>> listener){
        try {
            cacheSearch(searchProfilesQuery);
            org.libertaria.world.profile_server.client.ProfSerRequest request = profileServer.searchProfilesRequest(
                    searchProfilesQuery.isOnlyHostedProfiles(),
                    searchProfilesQuery.isIncludeThumbnailImages(),
                    searchProfilesQuery.getMaxResponseRecordCount(),
                    searchProfilesQuery.getMaxTotalRecordCount(),
                    searchProfilesQuery.getProfileType(),
                    searchProfilesQuery.getProfileName(),
                    searchProfilesQuery.getLatitude(),
                    searchProfilesQuery.getLongitude(),
                    searchProfilesQuery.getRadius(),
                    searchProfilesQuery.getExtraData()
            );
            sendRequest(request,listener);
        } catch (org.libertaria.world.profile_server.CantConnectException e) {
            e.printStackTrace();
        } catch (CantSendMessageException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method works after call searchProfile when the previous amount of result is less than the maxTotalRecordCount. Responding with a part of the entire search.
     *
     */
    public void searchSubsequentProfiles(SearchProfilesQuery searchProfilesQuery, org.libertaria.world.profile_server.engine.listeners.ProfSerPartSearchListener<List<IopProfileServer.ProfileQueryInformation>> listener){
        searchProfilesQuery.setRecordIndex(searchProfilesQuery.getRecordIndex()+1);
        updateCacheSearch(searchProfilesQuery);
        try{
            org.libertaria.world.profile_server.client.ProfSerRequest request = profileServer.searchProfilePartRequest(searchProfilesQuery.getRecordIndex(),searchProfilesQuery.getRecordCount());
            sendRequest(request,listener);
        } catch (CantSendMessageException e) {
            e.printStackTrace();
        } catch (org.libertaria.world.profile_server.CantConnectException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * Request a single profile hosted on the server
     *
     * @param pubKey
     * @param includeProfileImage
     * @param includeThumbnailImage
     * @param includeApplicationServices
     * @param listener
     */
    public void getProfileInformation(String pubKey,boolean includeProfileImage, boolean includeThumbnailImage, boolean includeApplicationServices, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<ProfileInformation> listener) throws org.libertaria.world.profile_server.CantConnectException, CantSendMessageException {
        // hash of the public key
        LOG.info("getProfileInformation "+pubKey);
        byte[] profileNetworkId = Sha256Hash.hash(org.libertaria.world.crypto.CryptoBytes.fromHexToBytes(pubKey));
        org.libertaria.world.profile_server.client.ProfSerRequest profSerRequest = profileServer.getProfileInformationRequest(profileNetworkId,includeApplicationServices,includeThumbnailImage,includeProfileImage);
        sendRequest(profSerRequest,listener);
    }

    /**
     *  Request to establish a bridged connection between a requestor (the caller) and an identity (the callee)
     *  hosted on the profile server via one of its supported application service. The callee has to be online,
     *  otherwise the request will fail.
     *
     *  The profile server informs the callee about the incoming call and issues a token pair (caller's and
     *  callee's tokens) to identify the caller and the callee on the Application Service Interface. The callee's
     *  token is sent to the callee with the information about the incoming call. If the callee wants to accept
     *  the call, the profile server informs the caller and sends it the caller's token. Both clients are then
     *  expected to establish new connections to the profile server's Application Service Interface and use their
     *  tokens to send a message to the other client.
     *
     *  Roles: clNonCustomer, clCustomer
     *
     *  Conversation status: Verified
     *
     *  @param profilePubKey -> remote profile public key
     *  @param appService -> appService name
     */
    public void callProfileAppService(String profilePubKey, String appService, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener listener) throws org.libertaria.world.profile_server.CantConnectException, CantSendMessageException {
        byte[] profileNetworkId = Sha256Hash.hash(org.libertaria.world.crypto.CryptoBytes.fromHexToBytes(profilePubKey));
        org.libertaria.world.profile_server.client.ProfSerRequest profSerRequest = profileServer.callIdentityApplicationServiceRequest(profileNetworkId,appService);
        sendRequest(profSerRequest,listener);
    }

    /**
     * Accept an incoming call
     *
     * @param msgId
     * @throws org.libertaria.world.profile_server.CantConnectException
     * @throws CantSendMessageException
     */
    public void acceptCall(int msgId) throws org.libertaria.world.profile_server.CantConnectException, CantSendMessageException {
        org.libertaria.world.profile_server.client.ProfSerRequest request = profileServer.incomingCallNotificationResponse(msgId);
        request.send();
    }

    /**
     *
     * The msg flow is:
     *
     * 1)  Sender send ApplicationServiceSendMessageRequest and wait for ApplicationServiceSendMessageResponse confirmation (channel setup opening a new socket)
     * 2)  Server send to receiver a ApplicationServiceReceiveMessageNotificationRequest
     * 3)  Receiver signs and response with a ApplicationServiceReceiveMessageNotificationResponse
     * 4)  Sender receive an ApplicationServiceSendMessageResponse
     *
     *
     * @param token
     * @param msg
     * @param listener
     * @throws org.libertaria.world.profile_server.CantConnectException
     * @throws CantSendMessageException
     */
    public void sendAppServiceMsg(byte[] token, byte[] msg, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<IopProfileServer.ApplicationServiceSendMessageResponse> listener) throws org.libertaria.world.profile_server.CantConnectException, CantSendMessageException {
        org.libertaria.world.profile_server.client.ProfSerRequest request = profileServer.appServiceSendMessageRequest(token,msg);
        sendRequest(request,listener);
    }

    public void pingAppService(String token, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<IopProfileServer.ApplicationServiceSendMessageResponse> listener) throws org.libertaria.world.profile_server.CantConnectException, CantSendMessageException {
        org.libertaria.world.profile_server.client.ProfSerRequest request = profileServer.ping(IopProfileServer.ServerRoleType.CL_APP_SERVICE,token);
        sendRequest(request,listener);
    }

    /**
     *
     * @param msgToRespond
     * @throws org.libertaria.world.profile_server.CantConnectException
     * @throws CantSendMessageException
     */
    public void respondAppServiceReceiveMsg(String token,int msgToRespond) throws org.libertaria.world.profile_server.CantConnectException, CantSendMessageException {
        org.libertaria.world.profile_server.client.ProfSerRequest request = profileServer.appServiceReceiveMessageNotificationResponse(token,msgToRespond);
        request.send();
    }

    private void cacheSearch(SearchProfilesQuery searchProfilesQuery){
        String id = UUID.randomUUID().toString();
        searchProfilesQuery.setId(id);
        profilesQuery.put(id,searchProfilesQuery);
    }

    private void updateCacheSearch(SearchProfilesQuery searchProfilesQuery){
        if (profilesQuery.containsKey(searchProfilesQuery.getId())){
            profilesQuery.remove(searchProfilesQuery.getId());
            profilesQuery.put(searchProfilesQuery.getId(),searchProfilesQuery);
        }
    }

    /**
     * Add application service
     *
     * @param applicationService
     * @return
     */
    private int addApplicationServiceRequest(String applicationService, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener profSerMsgListener){
        if (!profNodeConnection.isRegistered()) throw new InvalidStateException("profile is not registered in the server");
        if (!isClConnectionReady()) throw new IllegalStateException("connection is not ready to send messages yet");
        int msgId = 0;
        try {
            org.libertaria.world.profile_server.client.ProfSerRequest request = profileServer.addApplicationService(applicationService);
            sendRequest(request,profSerMsgListener);
        } catch (CantSendMessageException e) {
            e.printStackTrace();
        } catch (org.libertaria.world.profile_server.CantConnectException e) {
            e.printStackTrace();
        }
        return msgId;
    }


    void initProfile(){
        try {
            final org.libertaria.world.profile_server.model.Profile profile = profNodeConnection.getProfile();
            // update data
            int msgId = updateProfile(
                    profile.getVersion(),
                    profile.getName(),
                    profile.getImg(),
                    profile.getImgHash(),
                    profile.getLatitude(),
                    profile.getLongitude(),
                    profile.getExtraData(),
                    new org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener() {
                        @Override
                        public void onMessageReceive(int messageId, Object message) {
                            // Nothing..
                            LOG.error("update profile init succed");
                        }

                        @Override
                        public void onMsgFail(int messageId, int statusValue, String details) {
                            LOG.error("update profile fail, detail: "+details);
                        }

                        @Override
                        public String getMessageName() {
                            return "update profile {init}";
                        }
                    }
            );
            if (!profNodeConnection.getProfile().getApplicationServices().isEmpty()){
                for (org.libertaria.world.profile_server.engine.app_services.AppService appService : profNodeConnection.getProfile().getApplicationServices().values()) {
                    addApplicationServiceRequest(appService.getName(),appService);
                }
            }
            profNodeConnection.setNeedRegisterProfile(false);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public ProfSerConnectionState getProfSerConnectionState() {
        return profSerConnectionState;
    }

    public org.libertaria.world.profile_server.client.ProfNodeConnection getProfNodeConnection() {
        return profNodeConnection;
    }

    public void setProfSerConnectionState(ProfSerConnectionState profSerConnection) {
        this.profSerConnectionState = profSerConnection;
    }

    public org.libertaria.world.profile_server.model.ProfServerData getProfServerData() {
        return profServerData;
    }

    /**
     * Close a specific port
     * @param port
     * @throws IOException
     */
    public void closePort(IopProfileServer.ServerRoleType port) throws IOException {
        stopPing(port);
        profileServer.closePort(port);
    }

    public void closeChannel(String callToken) throws IOException {
        profileServer.closeCallChannel(callToken);
    }


    public void startPing(final IopProfileServer.ServerRoleType portType) {
        LOG.info("startPing on port: "+portType);
        if (pingExecutors==null){
            pingExecutors = new HashMap<>();
        }
        if (pingExecutors.containsKey(portType)) throw new IllegalStateException("Ping agent already initilized for: "+portType);
        final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    LOG.info("sending ping");
                    profileServer.ping(portType).send();

                }catch (CantSendMessageException e){
                    e.printStackTrace();
                    service.shutdownNow();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        },10,15, TimeUnit.SECONDS);
        pingExecutors.put(portType,service);
    }

    public void stopPing(final IopProfileServer.ServerRoleType portType){
        try {
            LOG.info("stop ping for: "+portType);
            ScheduledExecutorService executor = pingExecutors.get(portType);
            executor.shutdownNow();
        }catch (Exception e){
            // nothing..
        }
    }

    public CopyOnWriteArrayList<org.libertaria.world.profile_server.engine.listeners.ConnectionListener> getConnectionListeners() {
        return connectionListener;
    }

    public boolean hasClConnectionFail() {
        return profSerConnectionState == ProfSerConnectionState.CONNECTION_FAIL;
    }

    public boolean isClConnectionConnecting(){
        return profSerConnectionState!= ProfSerConnectionState.CONNECTION_FAIL && profSerConnectionState!= ProfSerConnectionState.CHECK_IN;
    }

    /**
     * if the customer connection is ready
     *
     * @return
     */
    public boolean isClConnectionReady() {
        return getProfSerConnectionState() == ProfSerConnectionState.CHECK_IN;
    }


    /** Messages processors  */

    public class ProfileServerHandler implements org.libertaria.world.profile_server.client.PsSocketHandler<IopProfileServer.Message> {

        private static final String TAG = "ProfileServerHandler";

        // Responses:

        public static final int PING_PROCESSOR = 0;
        public static final int LIST_ROLES_PROCESSOR = 1;
        public static final int START_CONVERSATION_NON_CL_PROCESSOR = 2;
        private static final int HOME_NODE_REQUEST_PROCESSOR = 3;
        private static final int HOME_START_CONVERSATION_CL_PROCESSOR = 4;
        private static final int HOME_CHECK_IN_PROCESSOR = 5;
        private static final int HOME_UPDATE_PROFILE_PROCESSOR = 6;
        private static final int HOME_PROFILE_SEARCH_PROCESSOR = 7;
        private static final int HOME_ADD_APPLICATION_SERVICE_PROCESSOR = 8;
        private static final int HOME_PART_PROFILE_SEARCH_PROCESSOR = 9;
        private static final int GET_PROFILE_INFORMATION_PROCESSOR = 10;
        private static final int CALL_PROFILE_APP_SERVICE_PROCESSOR = 11;
        private static final int APP_SERVICE_SEND_MESSAGE_PROCESSOR = 12;
        private static final int CAN_STORE_DATA_PROCESSOR = 13;
        private static final int CAN_PUBLISH_IPNS_RECORD_PROCESSOR = 14;

        // Requests:

        private static final int INCOMING_CALL_NOTIFICATION = 101;
        private static final int INCOMING_APP_SERVICE_MSG_NOTIFICATION = 102;

        private Map<Integer, org.libertaria.world.profile_server.processors.MessageProcessor> processors;

        public ProfileServerHandler() {
            processors = new HashMap<>();
            processors.put(PING_PROCESSOR,new PingProcessor());
            processors.put(LIST_ROLES_PROCESSOR,new ListRolesProcessor());
            processors.put(START_CONVERSATION_NON_CL_PROCESSOR,new StartConversationNonClProcessor());
            processors.put(HOME_NODE_REQUEST_PROCESSOR,new HomeNodeRequestProcessor());
            processors.put(HOME_START_CONVERSATION_CL_PROCESSOR,new StartConversationClProcessor());
            processors.put(HOME_CHECK_IN_PROCESSOR,new CheckinConversationProcessor());
            processors.put(HOME_UPDATE_PROFILE_PROCESSOR,new UpdateProfileConversationProcessor());
            processors.put(HOME_PROFILE_SEARCH_PROCESSOR,new ProfileSearchProcessor());
            processors.put(HOME_ADD_APPLICATION_SERVICE_PROCESSOR,new AddApplicationServiceProcessor());
            processors.put(HOME_PART_PROFILE_SEARCH_PROCESSOR,new PartProfileSearchProcessor());
            processors.put(GET_PROFILE_INFORMATION_PROCESSOR,new GetProfileInformationProcessor());
            processors.put(CALL_PROFILE_APP_SERVICE_PROCESSOR,new CallIdentityApplicationServiceProcessor());
            processors.put(APP_SERVICE_SEND_MESSAGE_PROCESSOR, new ApplicationServiceSendMessageProcessor());
            processors.put(CAN_STORE_DATA_PROCESSOR,new CanStoreDataProcessor());
            processors.put(CAN_PUBLISH_IPNS_RECORD_PROCESSOR,new CanPublishIpnsRecordProcessor());


            // requests
            processors.put(INCOMING_CALL_NOTIFICATION, new IncomingCallNotificationProcessor());
            processors.put(INCOMING_APP_SERVICE_MSG_NOTIFICATION,new ApplicationServiceReceiveMessageNotificationRequestProcessor());
        }

        @Override
        public void sessionCreated(org.libertaria.world.profile_server.IoSession session) throws Exception {

        }

        @Override
        public void sessionOpened(org.libertaria.world.profile_server.IoSession session) throws Exception {

        }

        @Override
        public void sessionClosed(org.libertaria.world.profile_server.IoSession session) throws Exception {
            LOG.info("sessionClosed: "+session.toString());
            // notify upper layers
            for (org.libertaria.world.profile_server.engine.listeners.ConnectionListener listener : connectionListener) {
                listener.onConnectionLost(
                        profNodeConnection.getProfile(),
                        profServerData.getHost(),
                        session.getPortType(),
                        session.getSessionTokenId()
                );
            }
        }

        @Override
        public void exceptionCaught(org.libertaria.world.profile_server.IoSession session, Throwable cause) throws Exception {

        }

        @Override
        public void messageReceived(final org.libertaria.world.profile_server.IoSession session, final IopProfileServer.Message message) throws Exception {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        switch (message.getMessageTypeCase()) {
                            case REQUEST:
                                try{
                                    dispatchRequest(session,message.getId(),message.getRequest());
                                }catch (Exception e){
                                    LOG.info("Request, Message id fail: " + message.getId());
                                    e.printStackTrace();
                                }
                                break;

                            case RESPONSE:
                                try {
                                    dispatchResponse(session, message.getId(), message.getResponse());
                                } catch (Exception e) {
                                    LOG.info("Response Message id fail: " + message.getId());
                                    e.printStackTrace();
                                }
                                break;
                            default:
                                throw new IllegalArgumentException("message type not correspond to nothing, "+message);
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });

        }

        @Override
        public void portStarted(IopProfileServer.ServerRoleType portType) {

        }

        @Override
        public void messageSent(org.libertaria.world.profile_server.IoSession session, IopProfileServer.Message message) throws Exception {

        }

        @Override
        public void inputClosed(org.libertaria.world.profile_server.IoSession session) throws Exception {

        }

        private void dispatchRequest(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.Request request){

            switch (request.getConversationTypeCase()){

                case SINGLEREQUEST:
                    LOG.info("single request: with msg id: "+messageId);
                    dispatchRequest(session,messageId,request.getSingleRequest());
                    break;
                case CONVERSATIONREQUEST:
                    LOG.info("conversation request arrived: with msg id: "+messageId);
                    dispatchRequest(session,messageId,request.getConversationRequest());
                    break;
                case CONVERSATIONTYPE_NOT_SET:
                    LOG.error("request: with msg id: "+messageId+" "+request.toString());
                    break;
                default:
                    LOG.error("request: with msg id: "+messageId+" "+request.toString());
                    break;

            }

        }

        private void dispatchRequest(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.ConversationRequest request){

            switch (request.getRequestTypeCase()){

                case INCOMINGCALLNOTIFICATION:
                    processors.get(INCOMING_CALL_NOTIFICATION).execute(session, messageId,request.getIncomingCallNotification());
                    break;
                default:
                    LOG.error("Request not implemented");
                    break;

            }

        }


        private void dispatchRequest(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.SingleRequest request){

            switch (request.getRequestTypeCase()){

                case APPLICATIONSERVICERECEIVEMESSAGENOTIFICATION:
                    processors.get(INCOMING_APP_SERVICE_MSG_NOTIFICATION).execute(session,messageId,request.getApplicationServiceReceiveMessageNotification());
                    break;
                default:
                    LOG.error("Request not implemented");
                    break;

            }

        }


        private void dispatchResponse(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.Response response) throws Exception {
            switch (response.getConversationTypeCase()){

                case CONVERSATIONRESPONSE:
                    dispatchConversationResponse(session,messageId,response.getConversationResponse());
                    break;
                case SINGLERESPONSE:
                    dispatchSingleResponse(session,messageId,response.getSingleResponse());
                    break;

                case CONVERSATIONTYPE_NOT_SET:
                    org.libertaria.world.profile_server.protocol.IopShared.Status status = response.getStatus();
                    switch (status){
                        // this happen when the connection is active and i send a startConversation or something else that i have to see..
                        case ERROR_BAD_CONVERSATION_STATUS:
                            LOG.info("Message id: "+messageId+", response: "+response.toString()+", engine state: "+profSerConnectionState.toString());
//                            profSerConnectionState = START_CONVERSATION_NON_CL;
                            break;
                        // this happen whe the identity already exist or when the cl and non-cl port are the same in the StartConversation message
                        case ERROR_ALREADY_EXISTS:
                            LOG.info("response: "+response.toString());
                            // todo: for some reason this is happening... fix me please
                            if (profSerConnectionState == ProfSerConnectionState.WAITING_START_CL)
                                profSerConnectionState = ProfSerConnectionState.START_CONVERSATION_CL;
                            else profSerConnectionState = ProfSerConnectionState.HOME_NODE_REQUEST;
                            profNodeConnection.setIsRegistered(true);
                            msgListeners.get(messageId).onMsgFail(messageId,response.getStatusValue(),"ERROR_ALREADY_EXISTS, profile already exist on the server to request the home node request");
                            break;
                        case ERROR_INVALID_SIGNATURE:
                            LOG.error("response to msg id: "+messageId+" "+response.toString());

                            break;

                        case ERROR_NOT_AVAILABLE:
                            LOG.error("response: to msg id: "+messageId+" ERROR_NOT_AVAILABLE");
                            msgListeners.get(messageId).onMsgFail(messageId,response.getStatusValue(),"remote profile not available");
                            break;
                        case ERROR_NOT_FOUND:
                            LOG.error("response: to msg id: "+messageId+" ERROR_NOT_FOUND");
                            msgListeners.get(messageId).onMsgFail(messageId,response.getStatusValue(),"remote profile not found");
                            break;
                        case ERROR_INVALID_VALUE:
                            LOG.error("response: to msg id: "+messageId+" ERROR_INVALID_VALUE, "+response.getDetails());
                            msgListeners.get(messageId).onMsgFail(messageId,response.getStatusValue(),response.getDetails());
                            break;
                        case ERROR_UNINITIALIZED:
                            LOG.error("response: to msg id: "+messageId+" ERROR_UNINITIALIZED, "+response.getDetails());
                            msgListeners.get(messageId).onMsgFail(messageId,response.getStatusValue(),response.getDetails());
                            break;
                        case ERROR_UNAUTHORIZED:
                            LOG.error("response: to msg id: "+messageId+" ERROR_UNAUTHORIZED, "+response.getDetails());
                            msgListeners.get(messageId).onMsgFail(messageId,response.getStatusValue(),response.getDetails());
                            break;
                        case ERROR_PROTOCOL_VIOLATION:
                            // this should not happen..
                            LOG.error("response: to msg id: "+messageId+" ERROR_PROTOCOL_VIOLATION, "+response.getDetails());
                            LOG.error("Closing session for bad protocol: "+session.toString());
                            session.closeNow();
                            break;
                        default:
                            LOG.error("response: to msg id: "+messageId+" "+response.toString());
                            msgListeners.get(messageId).onMsgFail(messageId,response.getStatusValue(),response.getDetails());
                            throw new Exception("response with CONVERSATIONTYPE_NOT_SET, response: "+response.toString()+", message id: "+messageId);
                    }
                    break;

            }
        }

        private void dispatchSingleResponse(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.SingleResponse singleResponse){
            switch (singleResponse.getResponseTypeCase()){
                case PING:
                    processors.get(PING_PROCESSOR).execute(session, messageId,singleResponse.getPing());
                    break;
                case LISTROLES:
                    LOG.info("ListRoles received");
                    processors.get(LIST_ROLES_PROCESSOR).execute(session, messageId,singleResponse.getListRoles());
                    break;
                case GETPROFILEINFORMATION:
                    LOG.info("getProfileInformation received");
                    processors.get(GET_PROFILE_INFORMATION_PROCESSOR).execute(session, messageId,singleResponse.getGetProfileInformation());
                    break;

                case APPLICATIONSERVICESENDMESSAGE:
                    LOG.info("appServiceSendMessageResponse received");
                    processors.get(APP_SERVICE_SEND_MESSAGE_PROCESSOR).execute(session, messageId,singleResponse.getApplicationServiceSendMessage());
                    break;

                default:
                    LOG.info("algo llegó y no lo estoy controlando..");
                    break;
            }

        }

        private void dispatchConversationResponse(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.ConversationResponse conversationResponse) throws Exception {
            switch (conversationResponse.getResponseTypeCase()){

                case START:
                    LOG.info("init conversation received in port: "+session.getPortType());
                    // saving the challenge signed..
                    byte[] signedChallenge = conversationResponse.getSignature().toByteArray();
                    profNodeConnection.setSignedConnectionChallenge(signedChallenge);
                    LOG.info("challenge signed: "+ org.libertaria.world.crypto.CryptoBytes.toHexString(signedChallenge));

                    if (profSerConnectionState== ProfSerConnectionState.WAITING_START_NON_CL) processors.get(START_CONVERSATION_NON_CL_PROCESSOR).execute(session, messageId,conversationResponse.getStart());
                    else processors.get(HOME_START_CONVERSATION_CL_PROCESSOR).execute(session, messageId,conversationResponse.getStart());
                    break;
                case REGISTERHOSTING:
                    LOG.info("home node response received in port: "+session.getPortType());
                    processors.get(HOME_NODE_REQUEST_PROCESSOR).execute(session, messageId,conversationResponse.getRegisterHosting());
                    break;
                case CHECKIN:
                    LOG.info("check in response ");
                    processors.get(HOME_CHECK_IN_PROCESSOR).execute(session, messageId,conversationResponse.getCheckIn());
                    break;
                case UPDATEPROFILE:
                    if (verifyIdentity(conversationResponse.getSignature().toByteArray(),conversationResponse.toByteArray())) {
                        processors.get(HOME_UPDATE_PROFILE_PROCESSOR).execute(session, messageId,conversationResponse.getUpdateProfile());
                    }else {
                        throw new Exception("El nodo no es quien dice, acá tengo que desconectar todo");
                    }
                    break;
                case PROFILESEARCH:
                    LOG.info("profile search response ");
                    processors.get(HOME_PROFILE_SEARCH_PROCESSOR).execute(session, messageId,conversationResponse.getProfileSearch());
                    break;
                case PROFILESEARCHPART:
                    processors.get(HOME_PART_PROFILE_SEARCH_PROCESSOR).execute(session, messageId,conversationResponse.getProfileSearchPart());
                    break;
                case APPLICATIONSERVICEADD:
                    LOG.info("add application service");
                    processors.get(HOME_ADD_APPLICATION_SERVICE_PROCESSOR).execute(session, messageId,conversationResponse.getApplicationServiceAdd());
                    break;
                case CALLIDENTITYAPPLICATIONSERVICE:
                    LOG.info("call identity application service");
                    processors.get(CALL_PROFILE_APP_SERVICE_PROCESSOR).execute(session, messageId,conversationResponse.getCallIdentityApplicationService());
                    break;
                case CANSTOREDATA:
                    processors.get(CAN_STORE_DATA_PROCESSOR).execute(session, messageId,conversationResponse.getCanStoreData());
                    break;
                case CANPUBLISHIPNSRECORD:
                    processors.get(CAN_PUBLISH_IPNS_RECORD_PROCESSOR).execute(session, messageId,conversationResponse.getCanPublishIpnsRecord());
                    break;
                default:
                    LOG.info("algo llegó y no lo estoy controlando..");
                    break;
            }

        }

        private boolean verifyIdentity(byte[] signature,byte[] message) {
            //KeyEd25519.verify(signature,message,profile.getNodePubKey());
            return true;
        }
    }


    private class PingProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.PingResponse> {

        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.PingResponse message) {
            LOG.info("PingProcessor execute..");

        }
    }

    /**
     * Process a list roles response message
     */
    private class ListRolesProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.ListRolesResponse> {


        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.ListRolesResponse message) {
            LOG.info("ListRolesProcessor execute..");
            onMsgReceived(messageId,message);
        }
    }


    /**
     * Process start conversation to non customer port response
     */
    private class StartConversationNonClProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.StartConversationResponse> {

        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.StartConversationResponse message) {
            LOG.info("StartNonClProcessor execute..");
            onMsgReceived(messageId,message);
        }
    }

    /**
     * Process Register hosting response
     */
    private class HomeNodeRequestProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.RegisterHostingResponse> {

        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.RegisterHostingResponse message) {
            LOG.info("HomeNodeRequestProcessor execute.. "+ org.libertaria.world.crypto.CryptoBytes.toHexString(message.getContract().getIdentityPublicKey().toByteArray()));
            onMsgReceived(messageId,message);
        }
    }

    /**
     * Process Start conversation with the customer port
     */
    private class StartConversationClProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.StartConversationResponse> {

        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.StartConversationResponse message) {
            LOG.info("StartConversationClProcessor execute..");
            onMsgReceived(messageId,message);
        }
    }


    /**
     * Process the CheckIn message response
     */
    private class CheckinConversationProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.CheckInResponse> {

        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.CheckInResponse message) {
            LOG.info("CheckinProcessor execute..");
            onMsgReceived(messageId,message);
        }
    }


    private class UpdateProfileConversationProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.UpdateProfileResponse> {

        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.UpdateProfileResponse message) {
            LOG.info("UpdateProfileProcessor execute..");
            LOG.info("UpdateProfileProcessor update works..");
            onMsgReceived(messageId,true);
        }
    }

    private class ProfileSearchProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.ProfileSearchResponse> {

        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.ProfileSearchResponse message) {
            LOG.info("ProfileSearchProcessor execute..");
            LOG.info("Profile search count: "+message.getProfilesCount());
            StringBuilder stringBuilder = new StringBuilder();
            for (IopProfileServer.ProfileQueryInformation profileQueryInformation : message.getProfilesList()) {
                IopProfileServer.SignedProfileInformation signedProfileInformation = profileQueryInformation.getSignedProfile();
                stringBuilder
                        .append("PK: "+signedProfileInformation.getProfile().getPublicKey().toStringUtf8())
                        .append("\n")
                        .append("Name: "+signedProfileInformation.getProfile().getName())
                        .append("\n");
            }
            LOG.info(stringBuilder.toString());

            ((org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener)msgListeners.get(messageId)).onMessageReceive(messageId,message.getProfilesList());
            msgListeners.remove(messageId);

        }
    }

    /**
     *
     * Specific Error Responses:
     * ERROR_NOT_AVAILABLE - No cached search results are available. Either the client did not send ProfileSearchRequest previously
                              in this session, or its results have expired already.
     * ERROR_INVALID_VALUE
     * Response.details == "recordIndex" - 'ProfileSearchRequest.recordIndex' is not a valid index of the result.
     * Response.details == "recordCount" - 'ProfileSearchRequest.recordCount' is not a valid number of results to obtain in combination with 'ProfileSearchRequest.recordIndex'.
     *
     */

    private class PartProfileSearchProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.ProfileSearchPartResponse> {

        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.ProfileSearchPartResponse message) {
            LOG.info("PartProfileSearchProcessor execute..");
            ((org.libertaria.world.profile_server.engine.listeners.ProfSerPartSearchListener)msgListeners.get(messageId)).onMessageReceive(messageId,message.getProfilesList(),message.getRecordIndex(),message.getRecordCount());
        }
    }

    private class AddApplicationServiceProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.ApplicationServiceAddResponse> {


        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.ApplicationServiceAddResponse message) {
            LOG.info("AddApplicationServiceProcessor");
            // todo: acá deberia chequear si fueron listados bien los applicationServices..
            onMsgReceived(messageId,message);

        }
    }

    private class GetProfileInformationProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.GetProfileInformationResponse> {


        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.GetProfileInformationResponse message) {
            LOG.info("AddApplicationServiceProcessor");
            // todo: acá deberia chequear si fueron listados bien los applicationServices..
            onMsgReceived(messageId,message);

        }
    }

    private class CallIdentityApplicationServiceProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.CallIdentityApplicationServiceResponse> {

        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.CallIdentityApplicationServiceResponse message) {
            LOG.info("CallIdentityApplicationServiceProcessor");
            // todo: ver qué tengo que chequear acá
            onMsgReceived(messageId,message);
        }
    }


    /**
     *
     *  A response to ApplicationServiceSendMessageRequest. This is sent by the profile server to the client to
     *  confirm that it sent the message to the other client and the other client confirmed its arrival.
     *
     *  If the connection to one of the clients is terminated, the profile server closes the connection to the
     *  other client.
     *
     *  Specific Error Responses:
     *    * ERROR_NOT_FOUND - 'ApplicationServiceSendMessageRequest.token' is not a valid token. This can have many causes.
     *                        The token itself can have invalid format, or no such token was ever issued by the server.
     *                        However, it can also be the case that the token was valid in the past but the call channel
     *                        was closed by the server for any reason and thus the token is no longer valid.
     *
     */
    private class ApplicationServiceSendMessageProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.ApplicationServiceSendMessageResponse> {

        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.ApplicationServiceSendMessageResponse message) {
            LOG.info("ApplicationServiceSendMessageProcessor");
            // todo: ver qué tengo que chequear acá
            try {
                onMsgReceived(messageId, message);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private class IncomingCallNotificationProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.IncomingCallNotificationRequest> {

        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.IncomingCallNotificationRequest message) {
            LOG.info("IncomingCallNotificationProcessor");
            if (callListener!=null)
                callListener.incomingCallNotification(messageId,message);
            else
                LOG.error("IncomingCall arrive and no listener setted.");
        }
    }

    private class ApplicationServiceReceiveMessageNotificationRequestProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.ApplicationServiceReceiveMessageNotificationRequest> {

        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.ApplicationServiceReceiveMessageNotificationRequest message) {
            LOG.info("ApplicationServiceReceiveMessageNotificationRequestProcessor, "+session.toString());
            if (callListener!=null) {
                callListener.incomingAppServiceMessage(messageId, AppServiceMsg.wrap(session.getSessionTokenId(),message));
            }else
                LOG.error("IncomingCall arrive and no listener setted.");

        }
    }

    private class CanStoreDataProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.CanStoreDataResponse> {

        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.CanStoreDataResponse message) {
            LOG.info("CanStoreDataProcessor");
            onMsgReceived(messageId,message);
        }
    }

    private class CanPublishIpnsRecordProcessor implements org.libertaria.world.profile_server.processors.MessageProcessor<IopProfileServer.CanPublishIpnsRecordResponse> {
        @Override
        public void execute(org.libertaria.world.profile_server.IoSession session, int messageId, IopProfileServer.CanPublishIpnsRecordResponse message) {
            LOG.info("CanPublishIpnsRecordProcessor");
            onMsgReceived(messageId,message);
        }
    }

    private void onMsgReceived(int messageId, Object message){
        org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener profSerMsgListener = ((org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener)msgListeners.get(messageId));
        if (profSerMsgListener!=null){
            profSerMsgListener.onMessageReceive(messageId,message);
            msgListeners.remove(messageId);
        }else{
            throw new IllegalStateException("No msg listener for message with id: "+messageId+", "+message);
        }

    }

    
}
