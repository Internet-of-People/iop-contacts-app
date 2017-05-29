package org.fermat.redtooth.profile_server.client;


import com.google.protobuf.ByteString;

import org.fermat.redtooth.IoHandler;
import org.fermat.redtooth.global.ContextWrapper;
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


    public ProfSerImp(ContextWrapper context, ProfServerData configurations, SslContextFactory sslContextFactory, IoHandler<IopProfileServer.Message> handler) throws Exception {
        this.configurations = configurations;
        profSerConnectionManager = new ProfSerConnectionManager(configurations.getHost(),sslContextFactory,handler);
    }

    public void connect() throws IOException {
//        profSerConnectionManager.connect();
    }


    @Override
    public int ping(IopProfileServer.ServerRoleType portType) throws CantConnectException,CantSendMessageException {
        IopProfileServer.Message message = MessageFactory.buildPingRequestMessage(
                ByteString.copyFromUtf8("hi").toByteArray(),
                configurations.getProtocolVersion());
        profSerConnectionManager.write(portType,getPort(portType),message);
        return message.getId();
    }

    @Override
    public int listRolesRequest() throws CantConnectException,CantSendMessageException {
        IopProfileServer.Message message = MessageFactory.buildServerListRolesRequestMessage(configurations.getProtocolVersion());
        writeToPrimaryPort(message);
        return message.getId();
    }

    @Override
    public int homeNodeRequest(byte[] identityPk,String identityType) throws CantConnectException,CantSendMessageException {
        IopProfileServer.Message message = MessageFactory.buildHomeNodeRequestRequest(identityPk,identityType,System.currentTimeMillis(),null);
        writeToNonCustomerPort(message);
        return message.getId();
    }

    @Override
    public int startConversationNonCl(byte[] clientPk, byte[] challenge) throws CantConnectException,CantSendMessageException {
        logger.info("startConversationNonCl, clientPK bytes count: "+clientPk.length+", challenge bytes count: "+challenge.length);
        logger.info("clientPK: "+ Arrays.toString(clientPk));
        logger.info("challenge: "+ Arrays.toString(challenge));
        IopProfileServer.Message message = MessageFactory.buildStartConversationRequest(clientPk,challenge,configurations.getProtocolVersion());
        logger.info("startConversationNonCl message id: "+message.getId());
        writeToNonCustomerPort(message);
        return message.getId();
    }

    @Override
    public int startConversationNonCl(String clientPk, String challenge) throws CantConnectException,CantSendMessageException {
        return startConversationNonCl(ByteString.copyFromUtf8(clientPk).toByteArray(),ByteString.copyFromUtf8(challenge).toByteArray());

    }

    @Override
    public int startConversationCl(byte[] clientPk, byte[] challenge) throws CantConnectException,CantSendMessageException {
        IopProfileServer.Message message = MessageFactory.buildStartConversationRequest(clientPk,challenge,configurations.getProtocolVersion());
        logger.info("startConversationCl message id: "+message.getId());
        writeToCustomerPort(message);
        return message.getId();
    }
    // metodo rápido..
    @Override
    public int startConversationCl(String clientPk, String challenge) throws CantConnectException,CantSendMessageException{
       return startConversationCl(ByteString.copyFromUtf8(clientPk).toByteArray(),ByteString.copyFromUtf8(challenge).toByteArray());
    }

    @Override
    public int checkIn(byte[] nodeChallenge,Signer signer) throws CantConnectException,CantSendMessageException {
        IopProfileServer.Message message = MessageFactory.buildCheckInRequest(nodeChallenge,signer);
        logger.info("checkIn message id: "+message.getId());
        writeToCustomerPort(message);
        return message.getId();
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
    public ProfSerRequest updateProfileRequest(Signer signer, byte[] version, String name, byte[] img, int latitude, int longitude, String extraData) {
        final IopProfileServer.Message message = MessageFactory.buildUpdateProfileRequest(signer,version,name,img,latitude,longitude,extraData);
        logger.info("updateProfile, message: "+message.toString());
        return buildRequestToCustomerPort(message);
    }

    @Override
    public ProfSerRequest updateExtraData(Signer signer,String extraData) throws CantConnectException,CantSendMessageException{
        logger.info("UpdateExtraData, extra data: "+extraData);
        IopProfileServer.Message message = MessageFactory.buildUpdateProfileRequest(signer,null,null,null,0,0,extraData);
        return buildRequestToCustomerPort(message);
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
    public int addApplcationService(String applicationService) throws CantConnectException, CantSendMessageException {
        logger.info("addApplcationService");
        IopProfileServer.Message message = MessageFactory.buildApplicationServiceAddRequest(applicationService);
        writeToCustomerPort(message);
        return message.getId();
    }

    @Override
    public int getIdentityInformationRequest(String profilePk,boolean applicationServices, boolean thumbnail, boolean profileImage) throws CantConnectException, CantSendMessageException {
        logger.info("GetIdentityInformationRequest");
        IopProfileServer.Message message = MessageFactory.buildGetIdentityInformationRequest(profilePk,applicationServices,thumbnail,profileImage);
        writeToNonCustomerPort(message);
        return message.getId();
    }

    @Override
    public void addHandler(IoHandler hanlder) {
        profSerConnectionManager.setHandler(hanlder);
    }

    @Override
    public void closePort(IopProfileServer.ServerRoleType portType) throws IOException {
        profSerConnectionManager.close(portType);
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

    private void write(IopProfileServer.Message message, IopProfileServer.ServerRoleType serverRoleType,int port) throws CantConnectException, CantSendMessageException {
        profSerConnectionManager.write(
                serverRoleType,
                port,
                message
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
