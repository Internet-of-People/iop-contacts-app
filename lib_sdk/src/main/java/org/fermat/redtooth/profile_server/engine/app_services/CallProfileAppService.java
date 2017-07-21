package org.fermat.redtooth.profile_server.engine.app_services;

import com.google.protobuf.ByteString;

import org.fermat.redtooth.crypto.Crypto;
import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.client.AppServiceCallNotAvailableException;
import org.fermat.redtooth.profile_server.engine.ProfSerEngine;
import org.fermat.redtooth.profile_server.engine.crypto.BoxAlgo;
import org.fermat.redtooth.profile_server.engine.crypto.CryptoAlgo;
import org.fermat.redtooth.profile_server.engine.futures.BaseMsgFuture;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.fermat.redtooth.profile_server.engine.app_services.CallProfileAppService.Status.CALL_AS_ESTABLISH;
import static org.fermat.redtooth.profile_server.engine.app_services.CallProfileAppService.Status.CALL_FAIL;
import static org.fermat.redtooth.profile_server.engine.app_services.CallProfileAppService.Status.CALL_FINISHED;
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

        CALL_FAIL, // when the call fail for somethings -> profile should check the error status.
        CALL_FINISHED
    }

    public interface CallMessagesListener{

        void onMessage(MsgWrapper msg);

    }

    public interface CallStateListener{

        void onCallFinished(CallProfileAppService callProfileAppService);

    }

    /** App service used to connect both profiles  */
    private String appService;
    /** Local profile public key */
    private Profile localProfile;
    /** Remote profile public key */
    private ProfileInformation remoteProfile;
    //private String remoteProfilePk;
    /** Identifier token of the call in the server */
    private byte[] callToken;
    private String callTokenHex;
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
    /** Call state listener */
    private CopyOnWriteArrayList<CallStateListener> callStateListeners = new CopyOnWriteArrayList<>();
    /** Engine wrapped */
    private ProfSerEngine profSerEngine;

    public CallProfileAppService(String appService, Profile localProfile,ProfileInformation remoteProfile,ProfSerEngine profSerEngine,CryptoAlgo cryptoAlgo) {
        this(appService,localProfile,remoteProfile,profSerEngine);
        this.cryptoAlgo = cryptoAlgo;
        this.isEncrypted = (cryptoAlgo != null);
        this.isCallCreator = true;
    }

    public CallProfileAppService(String appService, Profile localProfile,ProfileInformation remoteProfile,ProfSerEngine profSerEngine) {
        this.appService = appService;
        this.localProfile = localProfile;
        this.remoteProfile = remoteProfile;
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
        return remoteProfile.getHexPublicKey();
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

    public ProfileInformation getRemoteProfile() {
        return remoteProfile;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public boolean isStablished() {
        return status == CALL_AS_ESTABLISH;
    }

    public boolean isDone() {
        return status==CALL_FINISHED;
    }

    public boolean isFail() {
        return status==CALL_FAIL;
    }

    public String getErrorStatus() {
        return errorStatus;
    }

    public Profile getLocalProfile() {
        return localProfile;
    }

    public boolean isCallCreator() {
        return isCallCreator;
    }

    public void addCallStateListener(CallStateListener callStateListener){
        callStateListeners.add(callStateListener);
    }

    public void removeCallStateListener(CallStateListener callStateListener){
        callStateListeners.remove(callStateListener);
    }

    public String getCallTokenHex() {
        if (callTokenHex==null){
            callTokenHex = CryptoBytes.toHexString(callToken);
        }
        return callTokenHex;
    }

    /**
     *
     *
     *
     * @param msg
     * @throws CantConnectException
     * @throws CantSendMessageException
     */

    public void sendMsgStr(String msg, final ProfSerMsgListener<Boolean> sendListener) throws CantConnectException, CantSendMessageException {
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

    public void sendMsg(BaseMsg msg,final ProfSerMsgListener<Boolean> sendListener) throws Exception {
        MsgWrapper msgWrapper = new MsgWrapper(msg,msg.getType());
        wrapAndSend(msgWrapper,sendListener);
    }

    public void sendMsg(String type,final ProfSerMsgListener<Boolean> sendListener) throws Exception {
        MsgWrapper msgWrapper = new MsgWrapper(null,type);
        wrapAndSend(msgWrapper,sendListener);
    }

    private void wrapAndSend(MsgWrapper msgWrapper,final ProfSerMsgListener<Boolean> sendListener) throws Exception {
        byte[] msgTemp = msgWrapper.encode();
        if (isEncrypted && !msgWrapper.getMsgType().equals(BasicCallMessages.CRYPTO.getType())){
            if (cryptoAlgo!=null){
                // todo: encrypt
                msgTemp = msgTemp;//cryptoAlgo.digest(msgTemp,msgTemp.length,localProfile.getPublicKey());
            }else {
                logger.error("msg encryption, crypto algo not setted");
                throw new CantSendMessageException("msg encryption, crypto algo not setted");
            }
        }
        sendMsg(msgTemp,sendListener);
    }

    public void ping() throws CantConnectException, CantSendMessageException {
        if (this.status != CALL_AS_ESTABLISH)
            throw new IllegalStateException("Call is not ready to send messages");
        MsgListenerFuture msgListenerFuture = new MsgListenerFuture();
        msgListenerFuture.setListener(new BaseMsgFuture.Listener() {
            @Override
            public void onAction(int messageId, Object object) {
                logger.info("App service call ping ok");
            }

            @Override
            public void onFail(int messageId, int status, String statusDetail) {
                logger.info("App service call ping fail..");
                dispose();
            }
        });
        profSerEngine.pingAppService(getCallTokenHex(), msgListenerFuture);
    }

    private void sendMsg(byte[] msg, final ProfSerMsgListener<Boolean> sendListener) throws CantConnectException, CantSendMessageException {
        if (this.status!=CALL_AS_ESTABLISH) throw new IllegalStateException("Call is not ready to send messages");
        try {
            MsgListenerFuture<IopProfileServer.ApplicationServiceSendMessageResponse> msgListenerFuture = new MsgListenerFuture();
            msgListenerFuture.setListener(new BaseMsgFuture.Listener<IopProfileServer.ApplicationServiceSendMessageResponse>() {
                @Override
                public void onAction(int messageId, IopProfileServer.ApplicationServiceSendMessageResponse object) {
                    logger.info("App service message sent!");
                    if (sendListener!=null)
                        sendListener.onMessageReceive(messageId, true);
                }

                @Override
                public void onFail(int messageId, int status, String statusDetail) {
                    logger.info("App service message fail");
                    if (sendListener!=null)
                        sendListener.onMsgFail(messageId, status, statusDetail);
                }
            });
            profSerEngine.sendAppServiceMsg(callToken, msg, msgListenerFuture);
        }catch (AppServiceCallNotAvailableException e){
            // intercept exception to destroy this call.
            sendListener.onMsgFail(0,400,"Call is not longer available");
            localProfile.getAppService(appService).removeCall(this,"Connection is not longer available");
            throw e;
        }
    }

    /**
     *  Method to receive messages from the server
     *
     * @param msg
     */
    public void onMessageReceived(byte[] msg){
        try {
            byte[] msgTemp = msg;
            if (isEncrypted) {
                if (cryptoAlgo != null) {
                    if (msg.length>0)
                        // todo: decrypt
                        msgTemp = msg;//cryptoAlgo.open(msg, localProfile.getPublicKey(), localProfile.getPrivKey());
                } else {
                    logger.error("msg decryption, crypto algo not setted");
                }
            }
            MsgWrapper msgWrapper = MsgWrapper.decode(msgTemp);
            if (msgWrapper.getMsgType().equals(BasicCallMessages.CRYPTO.getType())){
                // todo: do a class with the supported algos instead of this lazy lazy stuff..
                if(!((CryptoMsg)(msgWrapper.getMsg())).getAlgo().equals("box")){
                    setCryptoAlgo(new BoxAlgo());
                }else {
                    logger.info("crypto msg arrived with an unknown type.. "+msgWrapper.getMsg());
                }
            }else
                if (msgListener != null) {
                    msgListener.onMessage(msgWrapper);
                } else {
                    logger.warn("CallAppService msg received, not msgListener attached..");
                }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Dispose the call channel
     */
    public void dispose() {
        try {
            profSerEngine.closeChannel(
                    (callTokenHex != null) ?
                            callTokenHex
                            :
                            CryptoBytes.toHexString(callToken)
            );
        }catch (Exception e){
            // swallow..
        }
        try {
            // Notify upper layer about the disconnection.
            localProfile.getAppService(appService).removeCall(this, "local profile close connection");
        }catch (Exception e){
            e.printStackTrace();
        }
        // set to finished in case of some reference to this object
        status = CALL_FINISHED;
        // notify
        for (CallStateListener callStateListener : callStateListeners) {
            callStateListener.onCallFinished(this);
        }
    }

    @Override
    public String toString() {
        return "CallProfileAppService{" +
                "appService='" + appService + '\'' +
                ", localProfile=" + localProfile +
                ", remoteProfilePk='" + remoteProfile.getHexPublicKey() + '\'' +
                ", callToken=" + Arrays.toString(callToken) +
                ", status=" + status +
                ", errorStatus='" + errorStatus + '\'' +
                ", isCallCreator=" + isCallCreator +
                ", isEncrypted=" + isEncrypted +
                ", cryptoAlgo=" + cryptoAlgo +
                ", msgListener=" + msgListener +
                ", profSerEngine=" + profSerEngine +
                '}';
    }
}
