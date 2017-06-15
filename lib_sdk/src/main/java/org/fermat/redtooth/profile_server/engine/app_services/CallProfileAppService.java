package org.fermat.redtooth.profile_server.engine.app_services;

import com.google.protobuf.ByteString;

import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.engine.ProfSerEngine;
import org.fermat.redtooth.profile_server.engine.crypto.CryptoAlgo;
import org.fermat.redtooth.profile_server.engine.futures.BaseMsgFuture;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fermat.redtooth.profile_server.engine.app_services.CallProfileAppService.Status.CALL_AS_ESTABLISH;
import static org.fermat.redtooth.profile_server.engine.app_services.CallProfileAppService.Status.NO_INFORMATION;

/**
 * Created by mati on 18/05/17.
 */

public class CallProfileAppService {

    private static final Logger logger = LoggerFactory.getLogger(CallProfileAppService.class);

    public enum Status{

        // no information
        NO_INFORMATION,

        // Call creator flow:
        PENDING_AS_INFO,  // getProfileInformationRequest sent, waiting for response
        AS_INFO, // getProfileInformationRequest available app services data
        PENDING_CALL_AS, // CallIdentityApplicationServiceRequest sent, waiting for response -> there are two possibilities: if the remote profile is not connected the server will response with an error

        // Call receiver flow
        PENDING_INCOMING_CALL, // this is the initial status if the profile is not the creator, redtooth will launch a notification to the user to accept or deny the call
        INCOMING_CALL_ACCEPTED, // call accepted for the profile


        // connection stablish
        PENDING_INIT_MESSAGE, // init message sent, waiting for counterparty init message to receive a response and establish the call
        CALL_AS_ESTABLISH,  // if the profile is the creator of the call -> CallIdentityApplicationServiceResponse received with the remote token.
                            // if the profile is the receiver ->  the call archive this status when the profile response with the IncomingCallNotificationResponse

        CALL_FAIL // when the call fail for somethings -> profile should check the error status.
    }

    public interface CallMessagesListener{

        void onMessage(byte[] msg);

    }

    /** App service used to connect both profiles  */
    private String appService;
    /** Local profile public key */
    private Profile localProfile;
    /** Remote profile public key */
    private String remoteProfilePk;
    /** Identifier token of the call in the server */
    private byte[] callToken;
    /** Call status */
    private Status status = NO_INFORMATION;
    /** Call error status */
    private String errorStatus;
    /** If the local profile is the creator of the call */
    private boolean isCallCreator;
    /** If the call is encryted by the parties */
    private boolean isEncrypted;
    /** Algorithm used to encrypt and decrypt the messages */
    private CryptoAlgo cryptoAlgo;
    /** App call message listener */
    private CallMessagesListener msgListener;

    private ProfSerEngine profSerEngine;

    public CallProfileAppService(String appService, Profile localProfile,String remotePubKey,ProfSerEngine profSerEngine,CryptoAlgo cryptoAlgo) {
        this(appService,localProfile,remotePubKey,profSerEngine);
        this.cryptoAlgo = cryptoAlgo;
        this.isEncrypted = true;
    }

    public CallProfileAppService(String appService, Profile localProfile,String remotePubKey,ProfSerEngine profSerEngine) {
        this.appService = appService;
        this.localProfile = localProfile;
        this.remoteProfilePk = remotePubKey;
        this.profSerEngine = profSerEngine;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setErrorStatus(String errorStatus) {
        this.errorStatus = errorStatus;
    }

    public String getRemotePubKey() {
        return remoteProfilePk;
    }

    public String getAppService() {
        return appService;
    }

    public byte[] getCallToken() {
        return callToken;
    }

    public void setCallToken(byte[] callToken) {
        this.callToken = callToken;
    }

    public void setMsgListener(CallMessagesListener msgListener) {
        this.msgListener = msgListener;
    }

    public void setCryptoAlgo(CryptoAlgo cryptoAlgo){
        this.cryptoAlgo = cryptoAlgo;
        this.isEncrypted = true;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    /**
     *
     *
     *
     * @param msg
     * @throws CantConnectException
     * @throws CantSendMessageException
     */

    public void sendMsgStr(String msg, final MsgListenerFuture<Boolean> sendListener) throws CantConnectException, CantSendMessageException {
        if (this.status!=CALL_AS_ESTABLISH) throw new IllegalStateException("Call is not ready to send messages");
        byte[] msgBytes = ByteString.copyFromUtf8(msg).toByteArray();
        MsgListenerFuture<IopProfileServer.ApplicationServiceSendMessageResponse> msgListenerFuture = new MsgListenerFuture();
        msgListenerFuture.setListener(new BaseMsgFuture.Listener<IopProfileServer.ApplicationServiceSendMessageResponse>() {
            @Override
            public void onAction(int messageId, IopProfileServer.ApplicationServiceSendMessageResponse object) {
                logger.info("App service message sent!");
                sendListener.onMessageReceive(messageId,true);
            }

            @Override
            public void onFail(int messageId, int status, String statusDetail) {
                logger.info("App service message fail");
                sendListener.onMsgFail(messageId,status,statusDetail);
            }
        });
        profSerEngine.sendAppServiceMsg(callToken,msgBytes,msgListenerFuture);
    }

    public void sendMsg(BaseMsg msg,final MsgListenerFuture<Boolean> sendListener) throws Exception {
        MsgWrapper msgWrapper = new MsgWrapper(msg,msg.getType());
        wrapAndSend(msgWrapper,sendListener);
    }

    public void sendMsg(String type,final MsgListenerFuture<Boolean> sendListener) throws Exception {
        MsgWrapper msgWrapper = new MsgWrapper(null,type);
        wrapAndSend(msgWrapper,sendListener);
    }

    private void wrapAndSend(MsgWrapper msgWrapper,final MsgListenerFuture<Boolean> sendListener) throws Exception {
        sendMsg(msgWrapper.encode(),sendListener);
    }

    public void sendMsg(byte[] msg, final MsgListenerFuture<Boolean> sendListener) throws CantConnectException, CantSendMessageException {
        if (this.status!=CALL_AS_ESTABLISH) throw new IllegalStateException("Call is not ready to send messages");
        MsgListenerFuture<IopProfileServer.ApplicationServiceSendMessageResponse> msgListenerFuture = new MsgListenerFuture();
        msgListenerFuture.setListener(new BaseMsgFuture.Listener<IopProfileServer.ApplicationServiceSendMessageResponse>() {
            @Override
            public void onAction(int messageId, IopProfileServer.ApplicationServiceSendMessageResponse object) {
                logger.info("App service message sent!");
                sendListener.onMessageReceive(messageId,true);
            }

            @Override
            public void onFail(int messageId, int status, String statusDetail) {
                logger.info("App service message fail");
                sendListener.onMsgFail(messageId,status,statusDetail);
            }
        });
        byte[] msgTemp = msg;
        if (isEncrypted){
            if (cryptoAlgo!=null){
                msgTemp = cryptoAlgo.digest(msgTemp,msg.length,localProfile.getPublicKey());
            }else {
                logger.error("msg encryption, crypto algo not setted");
                throw new CantSendMessageException("msg encryption, crypto algo not setted");
            }
        }
        profSerEngine.sendAppServiceMsg(callToken,msgTemp,msgListenerFuture);
    }

    /**
     *  Method to receive messages from the server
     *
     * @param msg
     */
    public void onMessageReceived(byte[] msg){
        byte[] msgTemp = msg;
        if (msgListener!=null){
            if (isEncrypted){
                if (cryptoAlgo!=null){
                    msgTemp = cryptoAlgo.open(msg,localProfile.getPublicKey(),localProfile.getPrivKey());
                }else {
                    logger.error("msg decryption, crypto algo not setted");
                }
            }
            msgListener.onMessage(msgTemp);
        }else {
            logger.warn("CallAppService msg received, not msgListener attached..");
        }
    }
}
