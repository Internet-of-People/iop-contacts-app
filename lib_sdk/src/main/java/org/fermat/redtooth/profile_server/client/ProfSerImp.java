package org.fermat.redtooth.profile_server.client;


import com.google.protobuf.ByteString;

import org.fermat.redtooth.core.IoPConnectContext;
import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.profile_server.protocol.CanStoreMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.Signer;
import org.fermat.redtooth.profile_server.SslContextFactory;
import org.fermat.redtooth.profile_server.engine.ProfSerRequestImp;
import org.fermat.redtooth.profile_server.model.ProfServerData;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;
import org.fermat.redtooth.profile_server.protocol.MessageFactory;

import static org.fermat.redtooth.profile_server.protocol.MessageFactory.NO_LOCATION;

/**
 * Created by mati on 08/11/16.
 *
 * This class is in charge of build the messages in a protocol server language.
 *
 */

public class ProfSerImp implements ProfileServer {

    private static final Logger logger = LoggerFactory.getLogger(ProfSerImp.class);

    private ProfSerConnectionManager profSerConnectionManager;

    private ProfServerData configurations;


    public ProfSerImp(IoPConnectContext context, ProfServerData configurations, SslContextFactory sslContextFactory, PsSocketHandler<IopProfileServer.Message> handler) {
        this.configurations = configurations;
        profSerConnectionManager = new ProfSerConnectionManager(configurations.getHost(),sslContextFactory,handler);
    }


    @Override
    public ProfSerRequest ping(IopProfileServer.ServerRoleType portType) throws CantConnectException,CantSendMessageException{
        return ping(portType,null);
    }

    @Override
    public ProfSerRequest ping(IopProfileServer.ServerRoleType portType,String token) throws CantConnectException,CantSendMessageException {
        IopProfileServer.Message message = MessageFactory.buildPingRequestMessage(
                ByteString.copyFromUtf8("hi").toByteArray(),
                configurations.getProtocolVersion());
        switch (portType){
            case CL_CUSTOMER:
                return buildRequestToCustomerPort(message);
            case CL_NON_CUSTOMER:
                return buildRequestToNonCustomerPort(message);
            case PRIMARY:
                return buildRequestToPrimaryPort(message);
            case CL_APP_SERVICE:
                if (token==null) throw new IllegalArgumentException("token cannot be null on a appServicePortRequest");
                return buildRequestToAppServicePort(token,message);
            case SR_NEIGHBOR:
                throw new IllegalArgumentException("app service port not implemented");
            case UNRECOGNIZED:
                throw new IllegalArgumentException("portType UNRECOGNIZED");
            default:
                throw new IllegalArgumentException("portType not found");
        }
    }



    @Override
    public ProfSerRequest listRolesRequest() throws CantConnectException,CantSendMessageException {
        IopProfileServer.Message message = MessageFactory.buildServerListRolesRequestMessage(configurations.getProtocolVersion());
        return buildRequestToPrimaryPort(message);
    }

    @Override
    public ProfSerRequest registerHostRequest(Signer signer,byte[] identityPk, String identityType) throws CantConnectException,CantSendMessageException {
        IopProfileServer.Message message = MessageFactory.buildRegisterHostRequest(identityPk,identityType,System.currentTimeMillis(),null,signer);
        return buildRequestToNonCustomerPort(message);
    }

    @Override
    public ProfSerRequest startConversationNonCl(byte[] clientPk, byte[] challenge) throws CantConnectException,CantSendMessageException {
        logger.info("startConversationNonCl, clientPK bytes count: "+clientPk.length+", challenge bytes count: "+challenge.length);
        logger.info("clientPK: "+ Arrays.toString(clientPk));
        logger.info("challenge: "+ Arrays.toString(challenge));
        IopProfileServer.Message message = MessageFactory.buildStartConversationRequest(clientPk,challenge,configurations.getProtocolVersion());
        logger.info("startConversationNonCl message id: "+message.getId());
        return buildRequestToNonCustomerPort(message);
    }

    @Override
    public ProfSerRequest startConversationNonCl(String clientPk, String challenge) throws CantConnectException,CantSendMessageException {
        return startConversationNonCl(ByteString.copyFromUtf8(clientPk).toByteArray(),ByteString.copyFromUtf8(challenge).toByteArray());
    }

    @Override
    public ProfSerRequest startConversationCl(byte[] clientPk, byte[] challenge) throws CantConnectException,CantSendMessageException {
        IopProfileServer.Message message = MessageFactory.buildStartConversationRequest(clientPk,challenge,configurations.getProtocolVersion());
        logger.info("startConversationCl message id: "+message.getId());
        return buildRequestToCustomerPort(message);
    }
    // metodo rápido..
    @Override
    public ProfSerRequest startConversationCl(String clientPk, String challenge) throws CantConnectException,CantSendMessageException{
       return startConversationCl(ByteString.copyFromUtf8(clientPk).toByteArray(),ByteString.copyFromUtf8(challenge).toByteArray());
    }

    @Override
    public ProfSerRequest checkIn(byte[] nodeChallenge,Signer signer) throws CantConnectException,CantSendMessageException {
        IopProfileServer.Message message = MessageFactory.buildCheckInRequest(nodeChallenge,signer);
        logger.info("checkIn message id: "+message.getId());
        return buildRequestToCustomerPort(message);
    }

    /**
     * todo: ver si hace falta que se firme este mensaje
     *
     * @param version
     * @param name
     * @param img
     * @param latitude
     * @param longitude
     * @param extraData
     * @return
     */
    @Override
    public ProfSerRequest updateProfileRequest(Signer signer,byte[] profilePublicKey,String profType, byte[] version, String name, byte[] img, byte[] imgHash, int latitude, int longitude, String extraData) {
        final IopProfileServer.Message message = MessageFactory.buildUpdateProfileRequest(
                signer,
                profilePublicKey,
                profType,
                version,
                name,
                img,
                imgHash,
                latitude,
                longitude,
                extraData
        );
        return buildRequestToCustomerPort(message);
    }

    /*@Override
    public ProfSerRequest updateExtraData(Signer signer,byte[] profilePublicKey,String profType,String extraData) throws CantConnectException,CantSendMessageException{
        logger.info("UpdateExtraData, extra data: "+extraData);
        IopProfileServer.Message message = MessageFactory.buildUpdateProfileRequest(signer,profilePublicKey,profType,null,null,null,0,0,extraData);
        return buildRequestToCustomerPort(message);
    }*/

    @Override
    public ProfSerRequest storeCanDataRequest(CanStoreMap canStoreMap) {
        logger.info("storeCanDataRequest");
        IopProfileServer.Message message = MessageFactory.buildCanStoreDataRequest(configurations.getNetworkId(),canStoreMap);

        return null;
    }

    @Override
    public ProfSerRequest searchProfilesRequest(boolean onlyHostedProfiles, boolean includeThumbnailImages, int maxResponseRecordCount, int maxTotalRecordCount, String profileType, String profileName, String extraData) throws CantConnectException, CantSendMessageException {
        logger.info("searchProfilesRequest");
        return searchProfilesRequest(onlyHostedProfiles,includeThumbnailImages,maxResponseRecordCount,maxTotalRecordCount,profileType,profileName,NO_LOCATION,NO_LOCATION,0,extraData);
    }

    @Override
    public ProfSerRequest searchProfilesRequest(boolean onlyHostedProfiles, boolean includeThumbnailImages, int maxResponseRecordCount, int maxTotalRecordCount, String profileType, String profileName, int latitude, int longitude, int radius, String extraData) throws CantConnectException, CantSendMessageException {
        logger.info("searchProfilesRequest");
        IopProfileServer.Message message = MessageFactory.buildProfileSearchRequest(
                onlyHostedProfiles,
                includeThumbnailImages,
                maxResponseRecordCount,
                maxTotalRecordCount,
                profileType,
                profileName,
                latitude,
                longitude,
                radius,
                extraData
        );
        return buildRequestToNonCustomerPort(message);
    }

    @Override
    public ProfSerRequest searchProfilePartRequest(int recordIndex, int recordCount) throws CantConnectException, CantSendMessageException {
        logger.info("searchProfilePartRequest");
        IopProfileServer.Message msg = MessageFactory.buildSearcProfilePartRequest(recordIndex,recordCount);
        return buildRequestToCustomerPort(msg);
    }

    @Override
    public ProfSerRequest addApplicationService(String applicationService) throws CantConnectException, CantSendMessageException {
        logger.info("addApplicationService");
        IopProfileServer.Message message = MessageFactory.buildApplicationServiceAddRequest(applicationService);
        return buildRequestToCustomerPort(message);
    }

    @Override
    public ProfSerRequest getProfileInformationRequest(byte[] profileNetworkId, boolean applicationServices, boolean thumbnail, boolean profileImage) throws CantConnectException, CantSendMessageException {
        logger.info("getProfileInformationRequest");
        IopProfileServer.Message message = MessageFactory.buildGetIdentityInformationRequest(profileNetworkId,applicationServices,thumbnail,profileImage);
        return (configurations.isHome()) ? buildRequestToCustomerPort(message) : buildRequestToNonCustomerPort(message);
    }

    @Override
    public ProfSerRequest callIdentityApplicationServiceRequest(byte[] profileNetworkId, String appService) {
        logger.info("callIdentityApplicationServiceRequest");
        IopProfileServer.Message message = MessageFactory.buildCallIdentityApplicationServiceRequest(profileNetworkId,appService);
        return (configurations.isHome()) ? buildRequestToCustomerPort(message) : buildRequestToNonCustomerPort(message);
    }

    /**
     *
     *  This request is sent by a client to the profile server in order to deliver a message to the other client
     *  over the opened application service call channel.
     *
     *  After the client connects to clAppService port, it sends an initialization message using this request
     *  to inform the profile server about its identity. This initialization message is not delivered to the other
     *  party and the profile server responds to this initialization message only after the other party is also
     *  connected. In the initialization message, the 'message' field is ignored. If the other party fails to join
     *  the channel within 30 seconds, the profile server closes the existing connection to the connected client.
     *
     *  Until the client receives a reply from the profile server to its initialization message, it is not allowed
     *  to send other ApplicationServiceSendMessageRequest. This would be an error and the profile server would
     *  destroy the channel.
     *
     *  If neither of clients connects to clAppService port or sends an inititial message within 30 seconds after
     *  the call was initiated, the profile server destroys the channel.
     *
     *  Note that the clients are allowed to disconnect from clNonCustomer/clCustomer port once the caller receives
     *  CallIdentityApplicationServiceResponse and the callee sends IncomingCallNotificationResponse.
     *
     *  Each client is only allowed to have 20 ApplicationServiceSendMessageRequest messages pending, which means
     *  that there was no ApplicationServiceSendMessageResponse sent to the client. If a client attempts to send
     *  another message while having 20 pending messages, the profile server destroys the call channel.
     *
     *  Roles: clAppService
     *
     * @param token
     * @param msg
     * @return
     */
    @Override
    public ProfSerRequest appServiceSendMessageRequest(byte[] token,byte[] msg) {
        IopProfileServer.Message message = MessageFactory.buildApplicationServiceSendMessageRequest(token,msg);
        logger.info("appServiceSendMessageRequest, msg id: "+message.getId());
        return buildRequestToAppServicePort(CryptoBytes.toHexString(token),message);
    }

    @Override
    public ProfSerRequest incomingCallNotificationResponse(int msgId) {
        logger.info("incomingCallNotificationResponse");
        IopProfileServer.Message message = MessageFactory.buildIncomingCallNotificationResponse(msgId);
        return buildRequestToCustomerPort(message);
    }

    @Override
    public ProfSerRequest appServiceReceiveMessageNotificationResponse(String token,int msgId) {
        logger.info("appServiceReceiveMessageNotificationResponse");
        IopProfileServer.Message message = MessageFactory.buildApplicationServiceReceiveMessageNotificationRequest(msgId);
        return buildRequestToAppServicePort(token,message);
    }

    @Override
    public void addHandler(PsSocketHandler handler) {
        profSerConnectionManager.setHandler(handler);
    }

    @Override
    public void closePort(IopProfileServer.ServerRoleType portType) throws IOException {
        profSerConnectionManager.close(portType);
    }

    @Override
    public void closeCallChannel(String callToken) throws IOException {
        profSerConnectionManager.close(callToken);
    }

    @Override
    public void shutdown() throws IOException {
        profSerConnectionManager.shutdown();
    }

    private int getPort(IopProfileServer.ServerRoleType portType){
        int port = 0;
        switch (portType){
            case CL_CUSTOMER:
                port = configurations.getCustPort();
                break;
            case PRIMARY:
                port = configurations.getpPort();
                break;
            case CL_NON_CUSTOMER:
                port = configurations.getNonCustPort();
                break;
        }
        return port;
    }

    public void writeToPrimaryPort(IopProfileServer.Message message) throws CantConnectException, CantSendMessageException {
        write(message,IopProfileServer.ServerRoleType.PRIMARY,configurations.getpPort());
    }

    public void writeToNonCustomerPort(IopProfileServer.Message message) throws CantConnectException, CantSendMessageException {
        write(message,IopProfileServer.ServerRoleType.CL_NON_CUSTOMER,configurations.getNonCustPort());
    }

    public void writeToCustomerPort(IopProfileServer.Message message) throws CantConnectException, CantSendMessageException {
        write(message,IopProfileServer.ServerRoleType.CL_CUSTOMER,configurations.getCustPort());
    }

    public void writeToAppServicePort(String token,IopProfileServer.Message message) throws CantConnectException, CantSendMessageException{
        write(message, IopProfileServer.ServerRoleType.CL_APP_SERVICE,configurations.getAppServicePort(),token);
    }

    private void write(IopProfileServer.Message message, IopProfileServer.ServerRoleType serverRoleType,int port) throws CantConnectException, CantSendMessageException {
        profSerConnectionManager.write(
                serverRoleType,
                port,
                message
        );
    }

    private void write(IopProfileServer.Message message, IopProfileServer.ServerRoleType serverRoleType,int port,String token) throws CantConnectException, CantSendMessageException {
        profSerConnectionManager.writeToAppServiceCall(
                serverRoleType,
                port,
                message,
                token
        );
    }

    private ProfSerRequest buildRequestToPrimaryPort(final IopProfileServer.Message message){
        return new ProfSerRequestImp(message.getId()) {
            @Override
            public void send() throws CantConnectException, CantSendMessageException {
                writeToPrimaryPort(message);
            }
        };
    }

    private ProfSerRequest buildRequestToNonCustomerPort(final IopProfileServer.Message message){
        return new ProfSerRequestImp(message.getId()) {
            @Override
            public void send() throws CantConnectException, CantSendMessageException {
                writeToNonCustomerPort(message);
            }
        };
    }

    private ProfSerRequest buildRequestToCustomerPort(final IopProfileServer.Message message){
        return new ProfSerRequestImp(message.getId()) {
            @Override
            public void send() throws CantConnectException, CantSendMessageException {
                writeToCustomerPort(message);
            }
        };
    }

    private ProfSerRequest buildRequestToAppServicePort(final String token, final IopProfileServer.Message message) {
        return new ProfSerRequestImp(message.getId()) {
            @Override
            public void send() throws CantConnectException, CantSendMessageException {
                writeToAppServicePort(token,message);
            }
        };
    }

    /**
     * Ping tast to mantain alive the customer connection.
     */
    private class PingTask implements Runnable{

        IopProfileServer.ServerRoleType portType;

        public PingTask(IopProfileServer.ServerRoleType portType) {
            this.portType = portType;
        }

        @Override
        public void run() {

            try {
                ping(portType);
            } catch (Exception e) {
                e.printStackTrace();
                logger.info("PING FAIL, ver si tengo que reconectar acá..");
            }
        }
    }
}
