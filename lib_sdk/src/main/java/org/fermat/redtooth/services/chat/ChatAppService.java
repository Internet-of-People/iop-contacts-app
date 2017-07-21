package org.fermat.redtooth.services.chat;

import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.app_services.AppService;
import org.fermat.redtooth.profile_server.engine.app_services.CallProfileAppService;
import org.fermat.redtooth.profile_server.engine.app_services.MsgWrapper;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.services.EnabledServices;
import org.fermat.redtooth.services.chat.msg.*;
import org.fermat.redtooth.services.chat.msg.ChatMsgTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by mati on 16/05/17.
 */

public class ChatAppService extends AppService{

    private static final Logger logger = LoggerFactory.getLogger(ChatAppService.class);

    private LinkedList<ChatMsgListener> listeners;

    public ChatAppService() {
        super(EnabledServices.CHAT.getName());
        listeners = new LinkedList<>();
    }

    public void addListener(ChatMsgListener msgListener){
        listeners.add(msgListener);
    }

    @Override
    public void onPreCall() {
        super.onPreCall();
    }

    @Override
    public void onWrapCall(final CallProfileAppService callProfileAppService) {
        callProfileAppService.setMsgListener(new CallProfileAppService.CallMessagesListener() {
            @Override
            public void onMessage(MsgWrapper msg) {
                if (msg.getMsg().getType().equals(ChatMsgTypes.CHAT_REFUSED.name())){
                    // clean the connection here
                    callProfileAppService.dispose();
                }
                for (ChatMsgListener listener : listeners) {
                    // todo: Cambiar esto, en vez de enviar el chat message tengo que enviar el chatMsgWrapper con el local y remote profile..
                    listener.onMsgReceived(callProfileAppService.getRemotePubKey(),msg.getMsg());
                }

            }
        });
    }

    @Override
    public void onCallConnected(Profile localProfile, ProfileInformation remoteProfile,boolean isLocalCreator) {
        for (ChatMsgListener listener : listeners) {
            listener.onChatConnected(localProfile,remoteProfile.getHexPublicKey(),isLocalCreator);
        }
    }
}
