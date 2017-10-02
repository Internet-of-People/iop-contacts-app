package org.libertaria.world.services.chat;

import org.libertaria.world.profile_server.ProfileInformation;
import org.libertaria.world.profile_server.engine.app_services.AppService;
import org.libertaria.world.profile_server.engine.app_services.CallProfileAppService;
import org.libertaria.world.profile_server.engine.app_services.MessageWrapper;
import org.libertaria.world.profile_server.model.Profile;
import org.libertaria.world.services.EnabledServices;
import org.libertaria.world.services.chat.msg.ChatMsgTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by mati on 16/05/17.
 */

public class ChatAppService extends AppService {

    private static final Logger logger = LoggerFactory.getLogger(ChatAppService.class);

    private ChatMsgListener listener;

    public ChatAppService() {
        super(EnabledServices.CHAT.getName());
    }

    public void addListener(ChatMsgListener msgListener){
        this.listener = msgListener;
    }

    @Override
    public boolean onPreCall(CallProfileAppService callProfileAppService) {
        return listener.onPreCall(callProfileAppService.getLocalProfile().getHexPublicKey(),callProfileAppService.getRemoteProfile());
    }

    @Override
    public void onWrapCall(final CallProfileAppService callProfileAppService) {
        callProfileAppService.setCallIdleTime(TimeUnit.MINUTES.toMillis(1));
        callProfileAppService.setMsgListener(new org.libertaria.world.profile_server.engine.app_services.CallProfileAppService.CallMessagesListener() {
            @Override
            public void onMessage(MessageWrapper msg) {
                if (msg.getMsg().getType().equals(ChatMsgTypes.CHAT_REFUSED.name())){
                    // clean the connection here
                    callProfileAppService.dispose();
                }

                // todo: Cambiar esto, en vez de enviar el chat message tengo que enviar el chatMsgWrapper con el local y remote profile..
                listener.onMsgReceived(callProfileAppService.getRemotePubKey(),msg.getMsg());


            }
        });
    }

    @Override
    public void onCallConnected(Profile localProfile, ProfileInformation remoteProfile, boolean isLocalCreator) {
        listener.onChatConnected(localProfile,remoteProfile.getHexPublicKey(),isLocalCreator);
    }

    @Override
    public void onCallDisconnected(Profile localProfile, ProfileInformation remoteProfile, String reason) {
        listener.onChatDisconnected(remoteProfile.getHexPublicKey(),reason);
    }
}
