package org.libertaria.world.services.chat;

import org.libertaria.world.profile_server.ProfileInformation;
import org.libertaria.world.profile_server.engine.app_services.CallProfileAppService;
import org.libertaria.world.profile_server.model.Profile;
import org.libertaria.world.services.EnabledServices;
import org.libertaria.world.services.chat.msg.ChatMsgTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * Created by mati on 16/05/17.
 */

public class ChatAppService extends org.libertaria.world.profile_server.engine.app_services.AppService {

    private static final Logger logger = LoggerFactory.getLogger(ChatAppService.class);

    private LinkedList<ChatMsgListener> listeners;

    public ChatAppService() {
        super(EnabledServices.CHAT.getName());
        listeners = new LinkedList<>();
    }

    public void addListener(ChatMsgListener msgListener){
        listeners.add(msgListener);
    }

    public void removeListener(ChatMsgListener msgListener){
        listeners.remove(msgListener);
    }

    @Override
    public void onPreCall() {
        super.onPreCall();
    }

    @Override
    public void onWrapCall(final CallProfileAppService callProfileAppService) {
        callProfileAppService.setCallIdleTime(TimeUnit.MINUTES.toMillis(1));
        callProfileAppService.setMsgListener(new org.libertaria.world.profile_server.engine.app_services.CallProfileAppService.CallMessagesListener() {
            @Override
            public void onMessage(org.libertaria.world.profile_server.engine.app_services.MsgWrapper msg) {
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
    public void onCallConnected(Profile localProfile, ProfileInformation remoteProfile, boolean isLocalCreator) {
        for (ChatMsgListener listener : listeners) {
            listener.onChatConnected(localProfile,remoteProfile.getHexPublicKey(),isLocalCreator);
        }
    }

    @Override
    public void onCallDisconnected(Profile localProfile, ProfileInformation remoteProfile, String reason) {
        for (ChatMsgListener listener : listeners) {
            listener.onChatDisconnected(remoteProfile.getHexPublicKey(),reason);
        }
    }
}
