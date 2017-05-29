package org.fermat.redtooth.profile_server.engine;

import org.fermat.redtooth.IoHandler;
import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.crypto.CryptoWrapper;
import org.fermat.redtooth.global.ContextWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//import iop_sdk.IoHandler;
//import iop_sdk.crypto.CryptoBytes;
//import iop_sdk.crypto.CryptoWrapper;
//import iop_sdk.global.ContextWrapper;
import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.IoSession;
import org.fermat.redtooth.profile_server.SslContextFactory;
import org.fermat.redtooth.profile_server.client.ProfNodeConnection;
import org.fermat.redtooth.profile_server.client.ProfSerImp;
import org.fermat.redtooth.profile_server.client.ProfSerRequest;
import org.fermat.redtooth.profile_server.client.ProfileServer;
import org.fermat.redtooth.profile_server.db.ProfServerDbFile;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListenerBase;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerPartSearchListener;
import org.fermat.redtooth.profile_server.model.ProfServerData;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.processors.MessageProcessor;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;

import static org.fermat.redtooth.profile_server.engine.ProfSerConnectionState.CHECK_IN;
import static org.fermat.redtooth.profile_server.engine.ProfSerConnectionState.HAS_ROLE_LIST;
import static org.fermat.redtooth.profile_server.engine.ProfSerConnectionState.HOME_NODE_REQUEST;
import static org.fermat.redtooth.profile_server.engine.ProfSerConnectionState.NO_SERVER;
import static org.fermat.redtooth.profile_server.engine.ProfSerConnectionState.START_CONVERSATION_CL;
import static org.fermat.redtooth.profile_server.engine.ProfSerConnectionState.START_CONVERSATION_NON_CL;
import static org.fermat.redtooth.profile_server.engine.ProfSerConnectionState.WAITING_HOME_NODE_REQUEST;
import static org.fermat.redtooth.profile_server.engine.ProfSerConnectionState.WAITING_START_CL;
import static org.fermat.redtooth.profile_server.engine.ProfSerConnectionState.WAITING_START_NON_CL;

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
    private ProfileServer profileServer;
    /** Server configuration data */
    private ProfServerData profServerData;
    /** Profile connected cached class */
    private ProfNodeConnection profNodeConnection;
    /** Crypto wrapper implementation */
    private CryptoWrapper crypto;
    /** Db */
    private ProfSerDb profSerDb;
    /** Internal server handler */
    private ProfileServerHanlder handler;
    /** Engine listenerv*/
    private final CopyOnWriteArrayList<EngineListener> engineListeners = new CopyOnWriteArrayList<>();
    /** Messages listeners:  id -> listner */
    private final ConcurrentMap<Integer,ProfSerMsgListenerBase> msgListeners = new ConcurrentHashMap<>();
    private final ConcurrentMap<String,SearchProfilesQuery> profilesQuery = new ConcurrentHashMap<>();
    /***/
    private ExecutorService executor;

    /**
     *
     * @param profServerData
     * @param profile -> use a profile for the restriction of 1 per connection that the server have.
     */
    public ProfSerEngine(ContextWrapper contextWrapper, ProfSerDb profSerDb , ProfServerData profServerData, Profile profile, CryptoWrapper crypto, SslContextFactory sslContextFactory) throws Exception {
        this.profServerData = profServerData;
        this.crypto = crypto;
        this.profSerDb = profSerDb;
        this.profSerConnectionState=NO_SERVER;
        this.profNodeConnection = new ProfNodeConnection(
                profile,
                profSerDb.isRegistered(
                        profServerData.getHost(),
                        profile.getHexPublicKey()),
                randomChallenge()
        );
        handler = new ProfileServerHanlder();
        this.profileServer = new ProfSerImp(contextWrapper,profServerData,sslContextFactory,handler);
    }

    /**
     *
     * @param profServerData
     * @param profile -> use a profile for the restriction of 1 per connection that the server have.
     */
//    public ProfSerEngine(ContextWrapper contextWrapper,ProfServerData profServerData, Profile profile, CryptoWrapper crypto,SslContextFactory sslContextFactory) throws Exception {
//        this(contextWrapper,new ProfServerDbFile(contextWrapper.getDirPrivateMode("/")),profServerData,profile,crypto,sslContextFactory);
//    }

    /**
     * Creates a random challenge for the connection.
     * @return
     */
    private byte[] randomChallenge(){
        byte[] connChallenge = new byte[32];
        crypto.random(connChallenge,32);
        return connChallenge;
    }


    /**
     *
     * @param listener
     */
    public void addEngineListener(EngineListener listener){
        this.engineListeners.add(listener);
    }

    public void removeEngineListener(EngineListener listener){
        this.engineListeners.remove(listener);
    }

    /**
     * Start
     */
    public void start(){

        executor = Executors.newFixedThreadPool(3);

        executor.submit(new Runnable() {
            @Override
            public void run() {
                engine();
            }
        });

    }

    /**
     * Stop
     */
    public void stop(){

        executor.shutdown();
        executor = null;

    }


    /**
     * Public methods
     */

    /**
     * Update the existing profile in the server
     */
    public int updateProfile(byte[] version,String name,byte[] img,int lat,int lon,String extraData,ProfSerMsgListenerBase listener){
        LOG.info("updateProfile, state: "+profSerConnectionState);
        int msgId = 0;
        if (profSerConnectionState == CHECK_IN){
            try{
                ProfSerRequest profSerRequest = profileServer.updateProfileRequest(
                        profNodeConnection.getProfile(),
                        version,
                        name,
                        img,
                        lat,
                        lon,
                        extraData
                );
                msgId = profSerRequest.getMessageId();
                msgListeners.put(msgId,listener);
                profSerRequest.send();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return msgId;
    }

    public int addApplicationService(String applicationService){
        profNodeConnection.getProfile().addApplicationService(applicationService);
        return addApplicationServiceRequest(applicationService);
    }

    /**
     * //todo: hace falta dividir la lectura de la escritura.
     * @param name
     * @param listener
     */
    public void searchProfileByName(String name, ProfSerMsgListener<List<IopProfileServer.IdentityNetworkProfileInformation>> listener){
        try {
            ProfSerRequest request = profileServer.searchProfilesRequest(false,false,100,10000,null,name,null);
            msgListeners.put(
                    request.getMessageId(),
                    listener);
            request.send();
        } catch (CantConnectException e) {
            e.printStackTrace();
        } catch (CantSendMessageException e) {
            e.printStackTrace();
        }
    }

    public void searchProfileByNameAndType(String name,String type, ProfSerMsgListener<List<IopProfileServer.IdentityNetworkProfileInformation>> listener){
        try {
            ProfSerRequest request = profileServer.searchProfilesRequest(false,false,100,10000,type,name,null);
            msgListeners.put(
                    request.getMessageId(),
                    listener
            );
            request.send();
        } catch (CantConnectException e) {
            e.printStackTrace();
        } catch (CantSendMessageException e) {
            e.printStackTrace();
        }
    }

    /**
     * todo: fijarse si a este searchProfilesQuery le deberia poner un id para busquedas siguientes..
     * @param searchProfilesQuery
     * @param listener
     */
    public void searchProfiles(SearchProfilesQuery searchProfilesQuery, ProfSerMsgListener<List<IopProfileServer.IdentityNetworkProfileInformation>> listener){
        try {
            cacheSearch(searchProfilesQuery);
            ProfSerRequest request = profileServer.searchProfilesRequest(
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
            msgListeners.put(
                    request.getMessageId(),
                    listener
            );
            request.send();
        } catch (CantConnectException e) {
            e.printStackTrace();
        } catch (CantSendMessageException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method works after call searchProfile when the previous amount of result is less than the maxTotalRecordCount. Responding with a part of the entire search.
     *
     */
    public void searchSubsequentProfiles(SearchProfilesQuery searchProfilesQuery,ProfSerPartSearchListener<List<IopProfileServer.IdentityNetworkProfileInformation>> listener){
        searchProfilesQuery.setRecordIndex(searchProfilesQuery.getRecordIndex()+1);
        updateCacheSearch(searchProfilesQuery);
        try{
            ProfSerRequest request = profileServer.searchProfilePartRequest(searchProfilesQuery.getRecordIndex(),searchProfilesQuery.getRecordCount());
            msgListeners.put(
                    request.getMessageId(),
                    listener
            );
            request.send();
        } catch (CantSendMessageException e) {
            e.printStackTrace();
        } catch (CantConnectException e) {
            e.printStackTrace();
        }
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
     * Main method to init the connection with the server.
     */
    private void engine(){
        try {

            LOG.info("Engine");

            if (!profNodeConnection.isRegistered()){

                if (profSerConnectionState == NO_SERVER) {
                    // get the availables roles..
                    requestRoleList();
                }

                // init conversation to request a home request (if it not logged)
                startConverNonClPort();
                // Request home node request
                requestHomeNodeRequest();
            }else {
                profSerConnectionState = HAS_ROLE_LIST;
            }

            if (profNodeConnection.isRegistered()){
                // Start conversation with customer port
                startConverClPort();
            }

            // Client connected, now the identity have to do the check in
            requestCheckin();


        } catch (InvalidStateException e) {
            e.printStackTrace();
        }

    }

    /**
     * Request roles list to the server
     */
    private void requestRoleList() throws InvalidStateException {
        LOG.info("requestRoleList for host: "+profServerData.getHost());
        if (profSerConnectionState != NO_SERVER) throw new InvalidStateException(profSerConnectionState.toString(),NO_SERVER.toString());
            profSerConnectionState = ProfSerConnectionState.GETTING_ROLE_LIST;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        profileServer.listRolesRequest();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

    }

    /**
     * Start conversation with nonCustomerPort to make a HomeNodeRequest
     */
    private void startConverNonClPort(){
        if (profSerConnectionState == HAS_ROLE_LIST) {
            LOG.info("startConverNonClPort");
            if (profServerData.getNonCustPort()<=0) throw new RuntimeException("Non customer port <= 0!!");
            profSerConnectionState = WAITING_START_NON_CL;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        profileServer.startConversationNonCl(
                                profNodeConnection.getProfile().getPublicKey(),
                                profNodeConnection.getConnectionChallenge()
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }


    /**
     * Request a home node request to the server
     */
    private void requestHomeNodeRequest(){
        if (profSerConnectionState == START_CONVERSATION_NON_CL){
            LOG.info("requestHomeNodeRequest");
            profSerConnectionState = WAITING_HOME_NODE_REQUEST;

            try {
                profileServer.homeNodeRequest(
                        profNodeConnection.getProfile().getPublicKey(),
                        profNodeConnection.getProfile().getType()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Start a conversation with the customer port to make the check-in
     */
    private void startConverClPort(){
        if (profSerConnectionState == HOME_NODE_REQUEST || profSerConnectionState == HAS_ROLE_LIST) {
            LOG.info("startConverClPort");
            profSerConnectionState = WAITING_START_CL;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        profileServer.startConversationCl(
                                profNodeConnection.getProfile().getPublicKey(),
                                profNodeConnection.getConnectionChallenge()
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }


    /**
     * Do the check in to the server
     */
    private void requestCheckin(){
        if (profSerConnectionState==START_CONVERSATION_CL){
            try {
                // se le manda el challenge del nodo + la firma de dicho challenge en el campo de signature
                profileServer.checkIn(profNodeConnection.getNodeChallenge(), profNodeConnection.getProfile());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Add application service
     *
     * @param applicationService
     * @return
     */
    private int addApplicationServiceRequest(String applicationService){
        if (!profNodeConnection.isRegistered()) throw new InvalidStateException("profile is not registered in the server");
        int msgId = 0;
        try {
            msgId = profileServer.addApplcationService(applicationService);
        } catch (CantSendMessageException e) {
            e.printStackTrace();
        } catch (CantConnectException e) {
            e.printStackTrace();
        }
        return msgId;
    }


    private void initProfile(){
        try {
            Profile profile = profNodeConnection.getProfile();

            // update data
            int msgId = updateProfile(
                    profile.getVersion(),
                    profile.getName(),
                    profile.getImg(),
                    profile.getLatitude(),
                    profile.getLongitude(),
                    profile.getExtraData(),
                    new ProfSerMsgListenerBase() {
                        @Override
                        public void onMsgFail(int messageId, int statusValue, String details) {
                            LOG.error("update profile fail, detail: "+details);
                        }
                    }
            );
            // register application services
            for (String service : profile.getApplicationServices()) {
                addApplicationServiceRequest(service);
            }

            profNodeConnection.setNeedRegisterProfile(false);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /** Messages processors  */

    public class ProfileServerHanlder implements IoHandler<IopProfileServer.Message> {

        private static final String TAG = "ProfileServerHanlder";

        public static final int LIST_ROLES_PROCESSOR = 1;
        public static final int START_CONVERSATION_NON_CL_PROCESSOR = 2;
        private static final int HOME_NODE_REQUEST_PROCESSOR = 3;
        private static final int HOME_START_CONVERSATION_CL_PROCESSOR = 4;
        private static final int HOME_CHECK_IN_PROCESSOR = 5;
        private static final int HOME_UPDATE_PROFILE_PROCESSOR = 6;
        private static final int HOME_PROFILE_SEARCH_PROCESSOR = 7;
        private static final int HOME_ADD_APPLICATION_SERVICE_PROCESSOR = 8;
        private static final int HOME_PART_PROFILE_SEARCH_PROCESSOR = 9;

        private Map<Integer,MessageProcessor> processors;

        public ProfileServerHanlder() {
            processors = new HashMap<>();
            processors.put(LIST_ROLES_PROCESSOR,new ListRolesProcessor());
            processors.put(START_CONVERSATION_NON_CL_PROCESSOR,new StartConversationNonClProcessor());
            processors.put(HOME_NODE_REQUEST_PROCESSOR,new HomeNodeRequestProcessor());
            processors.put(HOME_START_CONVERSATION_CL_PROCESSOR,new StartConversationClProcessor());
            processors.put(HOME_CHECK_IN_PROCESSOR,new CheckinConversationProcessor());
            processors.put(HOME_UPDATE_PROFILE_PROCESSOR,new UpdateProfileConversationProcessor());
            processors.put(HOME_PROFILE_SEARCH_PROCESSOR,new ProfileSearchProcessor());
            processors.put(HOME_ADD_APPLICATION_SERVICE_PROCESSOR,new AddApplicationServiceProcessor());
            processors.put(HOME_PART_PROFILE_SEARCH_PROCESSOR,new PartProfileSearchProcessor());
        }

        @Override
        public void sessionCreated(IoSession session) throws Exception {

        }

        @Override
        public void sessionOpened(IoSession session) throws Exception {

        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {

        }

        @Override
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {

        }

        @Override
        public void messageReceived(IoSession session, IopProfileServer.Message message) throws Exception {
            switch (message.getMessageTypeCase()) {
                case REQUEST:

                    break;

                case RESPONSE:
                    try {
                        dispatchResponse(session,message.getId(), message.getResponse());
                    }catch (Exception e){
                        LOG.info("Message id fail: "+message.getId());
                        e.printStackTrace();
                    }
                    break;
            }
        }

        @Override
        public void messageSent(IoSession session, IopProfileServer.Message message) throws Exception {

        }

        @Override
        public void inputClosed(IoSession session) throws Exception {

        }

        private void dispatchResponse(IoSession session, int messageId, IopProfileServer.Response response) throws Exception {
            switch (response.getConversationTypeCase()){

                case CONVERSATIONRESPONSE:
                    dispatchConversationResponse(session,messageId,response.getConversationResponse());
                    break;
                case SINGLERESPONSE:
                    dispatchSingleResponse(session,messageId,response.getSingleResponse());
                    break;

                case CONVERSATIONTYPE_NOT_SET:
                    IopProfileServer.Status status = response.getStatus();
                    switch (status){
                        // this happen when the connection is active and i send a startConversation or something else that i have to see..
                        case ERROR_BAD_CONVERSATION_STATUS:
                            LOG.info("Message id: "+messageId+", response: "+response.toString()+", engine state: "+profSerConnectionState.toString());
//                            profSerConnectionState = START_CONVERSATION_NON_CL;
                            break;
                        // this happen whe the identity already exist or when the cl and non-cl port are the same in the StartConversation message
                        case ERROR_ALREADY_EXISTS:
                            LOG.info("response: "+response.toString());
                            if (profSerConnectionState == WAITING_START_CL)
                                profSerConnectionState = START_CONVERSATION_CL;
                            else profSerConnectionState = HOME_NODE_REQUEST;
                            break;

                        default:
                            msgListeners.get(messageId).onMsgFail(messageId,response.getStatusValue(),response.getDetails());
                            throw new Exception("response with CONVERSATIONTYPE_NOT_SET, response: "+response.toString()+", message id: "+messageId);
                    }
                    // engine
                    engine();
                    break;

            }
        }

        private void dispatchSingleResponse(IoSession session, int messageId, IopProfileServer.SingleResponse singleResponse){
            switch (singleResponse.getResponseTypeCase()){

                case LISTROLES:
                    LOG.info("ListRoles received");
                    processors.get(LIST_ROLES_PROCESSOR).execute(messageId,singleResponse.getListRoles());
                    break;

                default:
                    LOG.info("algo llegó y no lo estoy controlando..");
                    break;
            }

        }

        private void dispatchConversationResponse(IoSession session, int messageId, IopProfileServer.ConversationResponse conversationResponse) throws Exception {
            switch (conversationResponse.getResponseTypeCase()){

                case START:
                    LOG.info("init conversation received in port: "+session.getPortType());
                    // saving the challenge signed..
                    byte[] signedChallenge = conversationResponse.getSignature().toByteArray();
                    profNodeConnection.setSignedConnectionChallenge(signedChallenge);
                    LOG.info("challenge signed: "+ CryptoBytes.toHexString(signedChallenge));

                    if (profSerConnectionState==WAITING_START_NON_CL) processors.get(START_CONVERSATION_NON_CL_PROCESSOR).execute(messageId,conversationResponse.getStart());
                    else processors.get(HOME_START_CONVERSATION_CL_PROCESSOR).execute(messageId,conversationResponse.getStart());
                    break;
                case REGISTERHOSTING:
                    LOG.info("home node response received in port: "+session.getPortType());
                    processors.get(HOME_NODE_REQUEST_PROCESSOR).execute(messageId,conversationResponse.getRegisterHosting());
                    break;
                case CHECKIN:
                    LOG.info("check in response ");
                    processors.get(HOME_CHECK_IN_PROCESSOR).execute(messageId,conversationResponse.getCheckIn());
                    break;
                case UPDATEPROFILE:
                    if (verifyIdentity(conversationResponse.getSignature().toByteArray(),conversationResponse.toByteArray())) {
                        processors.get(HOME_UPDATE_PROFILE_PROCESSOR).execute(messageId,conversationResponse.getUpdateProfile());
                    }else {
                        throw new Exception("El nodo no es quien dice, acá tengo que desconectar todo");
                    }
                    break;
                case PROFILESEARCH:
                    LOG.info("profile search response ");
                    processors.get(HOME_PROFILE_SEARCH_PROCESSOR).execute(messageId,conversationResponse.getProfileSearch());
                    break;
                case PROFILESEARCHPART:
                    processors.get(HOME_PART_PROFILE_SEARCH_PROCESSOR).execute(messageId,conversationResponse.getProfileSearchPart());
                    break;
                case APPLICATIONSERVICEADD:
                    LOG.info("add application service");
                    processors.get(HOME_ADD_APPLICATION_SERVICE_PROCESSOR).execute(messageId,conversationResponse.getApplicationServiceAdd());

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


    /**
     * Process a list roles response message
     */
    private class ListRolesProcessor implements MessageProcessor<IopProfileServer.ListRolesResponse> {


        @Override
        public void execute(int messageId,IopProfileServer.ListRolesResponse message) {
            LOG.info("ListRolesProcessor execute..");
            for (IopProfileServer.ServerRole serverRole : message.getRolesList()) {
                switch (serverRole.getRole()){
                    case CL_NON_CUSTOMER:
                        profServerData.setNonCustPort(serverRole.getPort());
                        break;
                    case CL_CUSTOMER:
                        profServerData.setCustPort(serverRole.getPort());
                        break;
                    case PRIMARY:
                        // nothing
                        break;
                    default:
                        //nothing
                        break;
                }
            }
            profSerConnectionState = HAS_ROLE_LIST;
            try {
                profileServer.closePort(IopProfileServer.ServerRoleType.PRIMARY);
            }catch (Exception e){
                e.printStackTrace();
            }
            // save ports
            profSerDb.setClPort(profServerData.getCustPort());
            profSerDb.setNonClPort(profServerData.getNonCustPort());

            LOG.info("ListRolesProcessor no cl port: "+ profServerData.getNonCustPort());
            engine();
        }
    }


    /**
     * Process start conversation to non customer port response
     */
    private class StartConversationNonClProcessor implements MessageProcessor<IopProfileServer.StartConversationResponse>{

        @Override
        public void execute(int messageId,IopProfileServer.StartConversationResponse message) {
            LOG.info("StartNonClProcessor execute..");
            //todo: ver todos los get que tiene esto, el challenge y demás...
            profSerConnectionState = START_CONVERSATION_NON_CL;
            // set the node challenge
            profNodeConnection.setNodeChallenge(message.getChallenge().toByteArray());
            engine();
        }
    }

    /**
     * Process Register hosting response
     */
    private class HomeNodeRequestProcessor implements MessageProcessor<IopProfileServer.RegisterHostingResponse>{

        @Override
        public void execute(int messageId,IopProfileServer.RegisterHostingResponse message) {
            LOG.info("HomeNodeRequestProcessor execute..");

            //todo: ver que parametros utilizar de este homeNodeResponse..
            profSerConnectionState = HOME_NODE_REQUEST;
            // save data
            profSerDb.setProfileRegistered(profServerData.getHost(),profNodeConnection.getProfile().getHexPublicKey());
            profNodeConnection.setIsRegistered(true);
            profNodeConnection.setNeedRegisterProfile(true);
            engine();
        }
    }

    /**
     * Process Start conversation with the customer port
     */
    private class StartConversationClProcessor implements MessageProcessor<IopProfileServer.StartConversationResponse>{

        @Override
        public void execute(int messageId,IopProfileServer.StartConversationResponse message) {
            LOG.info("StartConversationClProcessor execute..");
            //todo: ver todos los get que tiene esto, el challenge y demás...
            // set the node challenge
            profNodeConnection.setNodeChallenge(message.getChallenge().toByteArray());
            profSerConnectionState = START_CONVERSATION_CL;
            engine();
        }
    }


    /**
     * Process the CheckIn message response
     */
    private class CheckinConversationProcessor implements MessageProcessor<IopProfileServer.CheckInResponse>{

        @Override
        public void execute(int messageId,IopProfileServer.CheckInResponse message) {
            LOG.info("CheckinProcessor execute..");
            profSerConnectionState = CHECK_IN;
            LOG.info("#### Check in completed!!  ####");

            // if the profile is just registered i have to initialize it
            if (profNodeConnection.isNeedRegisterProfile()){
                initProfile();
            }
            // notify check-in
            for (EngineListener engineListener : engineListeners) {
                engineListener.onCheckInCompleted(profNodeConnection.getProfile());
            }
        }
    }


    private class UpdateProfileConversationProcessor implements MessageProcessor<IopProfileServer.UpdateProfileResponse>{

        @Override
        public void execute(int messageId,IopProfileServer.UpdateProfileResponse message) {
            LOG.info("UpdateProfileProcessor execute..");
            LOG.info("UpdateProfileProcessor update works..");

        }
    }

    private class ProfileSearchProcessor implements MessageProcessor<IopProfileServer.ProfileSearchResponse>{

        @Override
        public void execute(int messageId,IopProfileServer.ProfileSearchResponse message) {
            LOG.info("ProfileSearchProcessor execute..");
            LOG.info("Profile search count: "+message.getProfilesCount());
            StringBuilder stringBuilder = new StringBuilder();
            for (IopProfileServer.IdentityNetworkProfileInformation identityNetworkProfileInformation : message.getProfilesList()) {
                stringBuilder
                        .append("PK: "+identityNetworkProfileInformation.getIdentityPublicKey().toStringUtf8())
                        .append("\n")
                        .append("Name: "+identityNetworkProfileInformation.getName())
                        .append("\n");
            }
            LOG.info(stringBuilder.toString());

            ((ProfSerMsgListener)msgListeners.get(messageId)).onMessageReceive(messageId,message.getProfilesList());
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

    private class PartProfileSearchProcessor implements MessageProcessor<IopProfileServer.ProfileSearchPartResponse>{

        @Override
        public void execute(int messageId, IopProfileServer.ProfileSearchPartResponse message) {
            LOG.info("PartProfileSearchProcessor execute..");
            ((ProfSerPartSearchListener)msgListeners.get(messageId)).onMessageReceive(messageId,message.getProfilesList(),message.getRecordIndex(),message.getRecordCount());
        }
    }

    private class AddApplicationServiceProcessor implements MessageProcessor<IopProfileServer.ApplicationServiceAddResponse>{


        @Override
        public void execute(int messageId,IopProfileServer.ApplicationServiceAddResponse message) {
            LOG.info("AddApplicationServiceProcessor");
            // todo: acá deberia chequear si fueron listados bien los applicationServices..

        }
    }

    
}
