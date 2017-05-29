package org.fermat.redtooth.profile_server.engine;

import com.google.protobuf.ByteString;

import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.engine.futures.BaseMsgFuture;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fermat.redtooth.profile_server.engine.CallProfileAppService.Status.CALL_AS_ESTABLISH;
import static org.fermat.redtooth.profile_server.engine.CallProfileAppService.Status.NO_INFORMATION;

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
    private String localProfilePk;
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
    /** App call message listener */
    private CallMessagesListener msgListener;

    private ProfSerEngine profSerEngine;

    public CallProfileAppService(String appService, String localProfilePk,String remotePubKey,ProfSerEngine profSerEngine) {
        this.appService = appService;
        this.localProfilePk = localProfilePk;
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

    /**
     *
     *
     *
     * @param msg
     * @throws CantConnectException
     * @throws CantSendMessageException
     */

    public void sendMsg(String msg, final MsgListenerFuture<Boolean> sendListener) throws CantConnectException, CantSendMessageException {
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

    /**
     *  Method to receive messages from the server
     *
     * @param msg
     */
    public void onMessageReceived(byte[] msg){
        if (msgListener!=null){
            msgListener.onMessage(msg);
        }else {
            logger.warn("CallAppService msg received, not msgListener attached..");
        }
    }
}
