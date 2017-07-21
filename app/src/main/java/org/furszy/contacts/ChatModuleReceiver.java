package org.furszy.contacts;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.app_services.BaseMsg;
import org.fermat.redtooth.services.chat.msg.ChatMsg;
import org.fermat.redtooth.services.chat.msg.ChatMsgTypes;
import org.furszy.contacts.ui.chat.WaitingChatActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatIntentsConstants;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatIntentsConstants.ACTION_ON_CHAT_CONNECTED;
import static iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatIntentsConstants.ACTION_ON_CHAT_DISCONNECTED;
import static iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatIntentsConstants.ACTION_ON_CHAT_MSG_RECEIVED;
import static iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatIntentsConstants.EXTRA_INTENT_CHAT_MSG;
import static iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatIntentsConstants.EXTRA_INTENT_IS_LOCAL_CREATOR;
import static iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatIntentsConstants.EXTRA_INTENT_LOCAL_PROFILE;
import static iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatIntentsConstants.EXTRA_INTENT_REMOTE_PROFILE;
import static org.furszy.contacts.App.INTENT_CHAT_ACCEPTED_BROADCAST;
import static org.furszy.contacts.App.INTENT_CHAT_REFUSED_BROADCAST;
import static org.furszy.contacts.App.INTENT_CHAT_TEXT_BROADCAST;
import static org.furszy.contacts.App.INTENT_CHAT_TEXT_RECEIVED;

/**
 * Created by furszy on 7/20/17.
 */

public class ChatModuleReceiver extends BroadcastReceiver{

    private Logger log = LoggerFactory.getLogger(ChatModuleReceiver.class);

    public ChatModuleReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(ACTION_ON_CHAT_CONNECTED)){
            String localPk = intent.getStringExtra(EXTRA_INTENT_LOCAL_PROFILE);
            String remotePk = intent.getStringExtra(EXTRA_INTENT_REMOTE_PROFILE);
            boolean isLocalCreator = intent.getBooleanExtra(EXTRA_INTENT_IS_LOCAL_CREATOR,false);
            onChatConnected(localPk,remotePk,isLocalCreator);
        }else if (action.equals(ACTION_ON_CHAT_DISCONNECTED)){
            String remotePk = intent.getStringExtra(EXTRA_INTENT_REMOTE_PROFILE);
            onChatDisconnected(remotePk);
        }else if (action.equals(ACTION_ON_CHAT_MSG_RECEIVED)){
            String remotePk = intent.getStringExtra(EXTRA_INTENT_REMOTE_PROFILE);
            BaseMsg baseMsg = (BaseMsg) intent.getSerializableExtra(EXTRA_INTENT_CHAT_MSG);
            onMsgReceived(remotePk,baseMsg);
        }
    }

    public void onChatConnected(String localProfilePubKey, String remoteProfilePubKey, boolean isLocalCreator) {
        log.info("on chat connected: " + remoteProfilePubKey);
        App app = App.getInstance();
        ProfileInformation remoteProflie = app.getAnRedtooth().getRedtooth().getKnownProfile(remoteProfilePubKey);
        if (remoteProflie != null) {
            // todo: negro acá abrí la vista de incoming para aceptar el request..
            Intent intent = new Intent(app, WaitingChatActivity.class);
            intent.putExtra(WaitingChatActivity.REMOTE_PROFILE_PUB_KEY, remoteProfilePubKey);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            if (isLocalCreator) {
                intent.putExtra(WaitingChatActivity.IS_CALLING, false);
                app.startActivity(intent);
            } else {
                PendingIntent pendingIntent = PendingIntent.getActivity(app, 0, intent, 0);
                // todo: null pointer found.
                String name = remoteProflie.getName();
                Notification not = new Notification.Builder(app)
                        .setContentTitle("Hey, chat notification received")
                        .setContentText(name + " want to chat with you!")
                        .setSmallIcon(R.drawable.ic_chat_disable)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build();
                app.getNotificationManager().notify(43, not);
            }
        } else {
            log.error("Chat notification arrive without know the profile, remote pub key " + remoteProfilePubKey);
        }
    }

    public void onChatDisconnected(String remotePubKey) {
        log.info("on chat disconnected: " + remotePubKey);
    }

    public void onMsgReceived(String remotePubKey, BaseMsg msg) {
        log.info("on chat msg received: " + remotePubKey);
        App app = App.getInstance();
        Intent intent = new Intent();
        intent.putExtra(WaitingChatActivity.REMOTE_PROFILE_PUB_KEY, remotePubKey);
        switch (ChatMsgTypes.valueOf(msg.getType())) {
            case CHAT_ACCEPTED:
                intent.setAction(INTENT_CHAT_ACCEPTED_BROADCAST);
                break;
            case CHAT_REFUSED:
                intent.setAction(INTENT_CHAT_REFUSED_BROADCAST);
                break;
            case TEXT:
                intent.putExtra(INTENT_CHAT_TEXT_RECEIVED, ((ChatMsg) msg).getText());
                intent.setAction(INTENT_CHAT_TEXT_BROADCAST);
                break;
        }
        app.getBroadcastManager().sendBroadcast(intent);
    }

}
