package org.furszy.contacts.requests;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.furszy.contacts.R;
import org.libertaria.world.core.services.pairing.PairRequestMessage;
import org.libertaria.world.profile_server.engine.MessageQueueManager;
import org.libertaria.world.profiles_manager.PairingRequest;
import org.libertaria.world.services.interfaces.PairingModule;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 15/11/2017.
 */
public class MessageQueueReceiver extends BroadcastReceiver {

    private PairingModule pairingModule;
    private NotificationManager notificationManager;

    public MessageQueueReceiver(PairingModule pairingModule, NotificationManager notificationManager) {
        this.pairingModule = pairingModule;
        this.notificationManager = notificationManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            PairRequestMessage pairRequestMessage;
            switch (intent.getAction()) {
                case MessageQueueManager.EVENT_MESSAGE_SUCCESSFUL: {
                    MessageQueueManager.Message message = (MessageQueueManager.Message) intent.getExtras().get(MessageQueueManager.EVENT_MESSAGE_SUCCESSFUL);
                    if (message == null) {
                        return;
                    }
                    if ((message.getMessage() instanceof PairRequestMessage)) {
                        pairRequestMessage = (PairRequestMessage) message.getMessage();
                        PairingRequest pairingRequest = pairingModule.getPairingRequest(pairRequestMessage.getPairingRequestId());
                        Notification not = new Notification.Builder(context)
                                .setContentTitle("Pairing request accepted!")
                                .setContentText("Your pairing request with " + pairingRequest.getRemoteName() + " has been accepted!")
                                .setSmallIcon(R.drawable.ic_add_contact)
                                .setAutoCancel(true)
                                .build();
                        notificationManager.notify((int) pairingRequest.getId(), not);
                    }
                }
                break;
                case MessageQueueManager.EVENT_MESSAGE_FAILED: {
                    MessageQueueManager.Message message = (MessageQueueManager.Message) intent.getExtras().get(MessageQueueManager.EVENT_MESSAGE_FAILED);
                    if (message == null) {
                        return;
                    }
                    if ((message.getMessage() instanceof PairRequestMessage)) {
                        pairRequestMessage = (PairRequestMessage) message.getMessage();
                        PairingRequest pairingRequest = pairingModule.getPairingRequest(pairRequestMessage.getPairingRequestId());
                        pairingModule.cancelPairingRequest(pairingRequest, false);
                        Notification not = new Notification.Builder(context)
                                .setContentTitle("Pairing request couldn't be sent.")
                                .setContentText("Your pairing request with " + pairingRequest.getRemoteName() + " couldn't be sent.")
                                .setSmallIcon(R.drawable.ic_close)
                                .setAutoCancel(true)
                                .build();
                        notificationManager.notify((int) pairingRequest.getId(), not);
                    }
                }
                break;
            }
        } catch (Exception e) {
            //Any exception here would be ignored.
            e.printStackTrace();
        }
    }
}