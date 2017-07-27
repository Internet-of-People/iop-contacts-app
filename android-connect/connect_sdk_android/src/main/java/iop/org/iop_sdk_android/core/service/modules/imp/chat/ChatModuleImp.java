package iop.org.iop_sdk_android.core.service.modules.imp.chat;

import android.content.Context;
import android.content.Intent;

import org.fermat.redtooth.core.IoPConnect;
import org.fermat.redtooth.global.Version;
import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.client.AppServiceCallNotAvailableException;
import org.fermat.redtooth.profile_server.engine.app_services.BaseMsg;
import org.fermat.redtooth.profile_server.engine.app_services.CallProfileAppService;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.services.EnabledServices;
import org.fermat.redtooth.services.chat.msg.ChatAcceptMsg;
import org.fermat.redtooth.services.chat.ChatAppService;
import org.fermat.redtooth.services.chat.ChatCallAlreadyOpenException;
import org.fermat.redtooth.services.chat.msg.ChatMsg;
import org.fermat.redtooth.services.chat.ChatMsgListener;
import org.fermat.redtooth.services.chat.RequestChatException;
import org.fermat.redtooth.services.chat.msg.ChatRefuseMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import iop.org.iop_sdk_android.core.service.exceptions.ChatCallClosedException;
import iop.org.iop_sdk_android.core.service.modules.AbstractModule;
import org.fermat.redtooth.services.chat.ChatModule;

import static iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatIntentsConstants.EXTRA_INTENT_CHAT_MSG;
import static iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatIntentsConstants.EXTRA_INTENT_DETAIL;
import static iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatIntentsConstants.EXTRA_INTENT_IS_LOCAL_CREATOR;
import static iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatIntentsConstants.EXTRA_INTENT_LOCAL_PROFILE;
import static iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatIntentsConstants.EXTRA_INTENT_REMOTE_PROFILE;

/**
 * Created by furszy on 7/20/17.
 */

public class ChatModuleImp extends AbstractModule implements ChatModule,ChatMsgListener {

    private Logger logger = LoggerFactory.getLogger(ChatModuleImp.class);

    private Context context;

    public ChatModuleImp(Context context,IoPConnect ioPConnect) {
        super(context,ioPConnect,Version.newProtocolAcceptedVersion(), EnabledServices.CHAT);
    }

    @Override
    public void requestChat(final String localProfilePubKey, final ProfileInformation remoteProfileInformation, final ProfSerMsgListener<Boolean> readyListener) throws RequestChatException, ChatCallAlreadyOpenException {
        // first check if the chat is active or was requested
        CallProfileAppService call = getCall(localProfilePubKey,remoteProfileInformation.getHexPublicKey());
        if (call!=null) throw new ChatCallAlreadyOpenException("chat call already open with "+remoteProfileInformation.getHexPublicKey());
        prepareCall(localProfilePubKey,remoteProfileInformation,readyListener);
    }

    @Override
    public void acceptChatRequest(String localProfilePubKey, String remoteHexPublicKey, ProfSerMsgListener<Boolean> future) throws Exception {
        CallProfileAppService callProfileAppService = getCall(localProfilePubKey,remoteHexPublicKey);
        if (callProfileAppService!=null) {
            callProfileAppService.sendMsg(new ChatAcceptMsg(System.currentTimeMillis()), future);
        }else {
            throw new AppServiceCallNotAvailableException("Connection not longer available");
        }
    }

    @Override
    public void refuseChatRequest(String localProfilePubKey, String remoteHexPublicKey) {
        CallProfileAppService callProfileAppService = getCall(localProfilePubKey,remoteHexPublicKey);
        if (callProfileAppService == null) return;
        try {
            callProfileAppService.sendMsg(new ChatRefuseMsg(), null);
        }catch (Exception e){
            e.printStackTrace();
            // do nothing..
        }
        callProfileAppService.dispose();
    }

    /**
     *
     * @param remoteProfileInformation
     * @param msg
     * @param msgListener
     * @throws Exception
     */
    @Override
    public void sendMsgToChat(String localProfilePubKey, ProfileInformation remoteProfileInformation, String msg, ProfSerMsgListener<Boolean> msgListener) throws Exception {
        CallProfileAppService callProfileAppService = null;
        try {
            ChatMsg chatMsg = new ChatMsg(msg);
            callProfileAppService = getCall(localProfilePubKey,remoteProfileInformation.getHexPublicKey());
            if (callProfileAppService==null) throw new ChatCallClosedException("Chat connection is not longer available",remoteProfileInformation);
            callProfileAppService.sendMsg(chatMsg, msgListener);
        }catch (AppServiceCallNotAvailableException e){
            e.printStackTrace();
            throw new ChatCallClosedException("Chat call not longer available",remoteProfileInformation);
        }
    }

    @Override
    public boolean isChatActive(String localProfilePubKey, String remotePk) {
        try {
            CallProfileAppService callProfileAppService = getCall(localProfilePubKey,remotePk);
            if (callProfileAppService!=null && callProfileAppService.isStablished()) {
                // check sending a ping
                callProfileAppService.ping();
                return true;
            }
        } catch (CantSendMessageException e) {
            e.printStackTrace();
        } catch (CantConnectException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onChatConnected(Profile localProfile, String remoteProfilePubKey, boolean isLocalCreator) {
        Intent intent = new Intent();
        intent.setAction(ChatIntentsConstants.ACTION_ON_CHAT_CONNECTED);
        intent.putExtra(EXTRA_INTENT_LOCAL_PROFILE,localProfile.getHexPublicKey());
        intent.putExtra(EXTRA_INTENT_REMOTE_PROFILE,remoteProfilePubKey);
        intent.putExtra(EXTRA_INTENT_IS_LOCAL_CREATOR,isLocalCreator);
        getContext().sendBroadcast(intent);
    }

    @Override
    public void onChatDisconnected(String remotePubKey,String reason) {
        Intent intent = new Intent();
        intent.setAction(ChatIntentsConstants.ACTION_ON_CHAT_DISCONNECTED);
        intent.putExtra(EXTRA_INTENT_REMOTE_PROFILE,remotePubKey);
        intent.putExtra(EXTRA_INTENT_DETAIL,reason);
        getContext().sendBroadcast(intent);
    }

    @Override
    public void onMsgReceived(String remotePubKey, BaseMsg msg) {
        Intent intent = new Intent();
        intent.setAction(ChatIntentsConstants.ACTION_ON_CHAT_MSG_RECEIVED);
        intent.putExtra(EXTRA_INTENT_REMOTE_PROFILE,remotePubKey);
        intent.putExtra(EXTRA_INTENT_CHAT_MSG,msg);
        getContext().sendBroadcast(intent);
    }
}
