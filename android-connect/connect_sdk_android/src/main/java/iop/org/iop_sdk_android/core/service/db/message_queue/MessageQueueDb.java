package iop.org.iop_sdk_android.core.service.db.message_queue;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.libertaria.world.global.IntentMessage;
import org.libertaria.world.global.SystemContext;
import org.libertaria.world.profile_server.engine.MessageQueueManager;
import org.libertaria.world.profile_server.engine.app_services.BaseMsg;
import org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener;
import org.spongycastle.util.encoders.Base64;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import iop.org.iop_sdk_android.core.wrappers.IntentWrapperAndroid;

/**
 * Created by furszy on 5/25/17.
 */

public class MessageQueueDb extends SQLiteOpenHelper implements MessageQueueManager {


    public static final int DATABASE_VERSION = 12;

    public static final String DATABASE_NAME = "message_queue";

    public static final String MESSAGES_TABLE_NAME = "messages";

    public static final String MESSAGES_COLUMN_ID = "message_id";
    public static final String MESSAGES_COLUMN_SERVICE_NAME = "service_name";
    public static final String MESSAGES_COLUMN_LOCAL_PROFILE = "local_profile";
    public static final String MESSAGES_COLUMN_REMOTE_PROFILE = "remote_profile";
    public static final String MESSAGES_COLUMN_MSG = "msg";
    public static final String MESSAGES_COLUMN_MSG_TYPE = "msg_type";
    public static final String MESSAGES_COLUMN_TRY_SEND_REMOTE = "try_send_remote";
    public static final String MESSAGES_COLUMN_RESEND_ATTEMPTS = "resend_attempts";
    public static final String MESSAGES_COLUMN_TIMESTAMP = "timestamp";

    public static final int MESSAGES_POS_COLUMN_ID = 0;
    public static final int MESSAGES_POS_COLUMN_SERVICE_NAME = 1;
    public static final int MESSAGES_POS_COLUMN_LOCAL_PROFILE = 2;
    public static final int MESSAGES_POS_COLUMN_REMOTE_PROFILE = 3;
    public static final int MESSAGES_POS_COLUMN_MSG = 4;
    public static final int MESSAGES_POS_COLUMN_MSG_TYPE = 5;
    public static final int MESSAGES_POS_COLUMN_TRY_SEND_REMOTE = 6;
    public static final int MESSAGES_POS_COLUMN_RESEND_ATTEMPTS = 7;
    public static final int MESSAGES_POS_COLUMN_TIMESTAMP = 8;

    private List<Message> messageQueue;

    private static final MessageQueueComparator DEFAULT_COMPARATOR = new MessageQueueComparator();
    private static Integer resendAttemptLimit = 3;

    private final SystemContext systemContext;

    public MessageQueueDb(SystemContext systemContext) {
        super((Context) systemContext, DATABASE_NAME, null, DATABASE_VERSION);
        messageQueue = new ArrayList<>();
        this.systemContext = systemContext;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + MESSAGES_TABLE_NAME +
                        "(" +
                        MESSAGES_COLUMN_ID + " TEXT PRIMARY KEY, " +
                        MESSAGES_COLUMN_SERVICE_NAME + " TEXT, " +
                        MESSAGES_COLUMN_LOCAL_PROFILE + " TEXT, " +
                        MESSAGES_COLUMN_REMOTE_PROFILE + " TEXT, " +
                        MESSAGES_COLUMN_MSG + " TEXT, " +
                        MESSAGES_COLUMN_MSG_TYPE + " TEXT, " +
                        MESSAGES_COLUMN_TRY_SEND_REMOTE + " TEXT, " +
                        MESSAGES_COLUMN_RESEND_ATTEMPTS + " INTEGER, " +
                        MESSAGES_COLUMN_TIMESTAMP + " LONG)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MESSAGES_TABLE_NAME);
        onCreate(db);
    }

    @Override
    public void enqueueMessage(String serviceName, String localProfile, String remoteProfile, BaseMsg messageToSend, boolean tryUpdateRemoteServices) {
        try {
            Message message = insertMessage(serviceName, localProfile, remoteProfile, messageToSend, tryUpdateRemoteServices);
            IntentMessage eventMessage = new IntentWrapperAndroid(EVENT_MESSAGE_ENQUEUED);
            eventMessage.put(EVENT_MESSAGE_ENQUEUED, message);
            systemContext.broadcastPlatformEvent(eventMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Message> getMessageQueue() {
        return getMessagesInQueue();
    }

    @Override
    public Boolean removeFromQueue(Message messageToRemove) {
        return deleteMessage(messageToRemove);
    }

    @Override
    public void notifyMessageSent(Message messageSent) {
        IntentMessage eventMessage = new IntentWrapperAndroid(EVENT_MESSAGE_SUCCESSFUL);
        eventMessage.put(messageSent.getMessageId().toString(), messageSent);
        systemContext.broadcastPlatformEvent(eventMessage);
    }

    @Override
    public ProfSerMsgListener<Boolean> buildDefaultQueueListener(Message message) {
        return new MessageListener(message);
    }

    @Override
    public void failedToResend(Message message) {
        try {
            increaseResendAttempt(message);
        } catch (Exception e) {
            notifyFailMessage(message);
        }
        if (message.getCurrentResendingAttempts() >= getResendAttemptLimit()) {
            notifyFailMessage(message);
        }
    }

    @Override
    public Integer getResendAttemptLimit() {
        return resendAttemptLimit;
    }

    @Override
    public void setResendAttemptLimit(Integer integer) {
        resendAttemptLimit = integer;
    }

    private void notifyFailMessage(Message message) {
        IntentMessage eventMessage = new IntentWrapperAndroid(EVENT_MESSAGE_FAILED);
        eventMessage.put(EVENT_MESSAGE_FAILED, message);
        systemContext.broadcastPlatformEvent(eventMessage);
        removeFromQueue(message);
    }

    private Message insertMessage(String serviceName, String localProfile, String remoteProfile, BaseMsg messageToSend, boolean tryUpdateRemoteServices) {
        Message message = new MessageImplementation(serviceName, localProfile, remoteProfile, tryUpdateRemoteServices, messageToSend);
        if (messageQueue.contains(message)) {
            message = messageQueue.get(messageQueue.indexOf(message)); //If it already exists we take the entry from the queue
            failedToResend(message); //Then we notify that it's a resend failure
            return message;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.insert(MESSAGES_TABLE_NAME, null, buildContent(message));
        messageQueue.add(message);
        return message;
    }

    private List<Message> getMessagesInQueue() {
        if (messageQueue.isEmpty()) {
            //hp = new HashMap();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + MESSAGES_TABLE_NAME + " ORDER BY " + MESSAGES_COLUMN_TIMESTAMP, null);
            if (res.moveToFirst()) {
                do {
                    try {
                        messageQueue.add(buildFrom(res));
                    } catch (Exception e) {
                        e.printStackTrace(); //Let's ignore only this record...
                    }
                } while (res.moveToNext());
            }
        }
        //We always resort the queue before returning it.
        Collections.sort(messageQueue, DEFAULT_COMPARATOR);
        return messageQueue;
    }

    private Boolean deleteMessage(Message message) {
        SQLiteDatabase db = this.getReadableDatabase();
        Integer rowsRemoved = db.delete(MESSAGES_TABLE_NAME, MESSAGES_COLUMN_ID + " = ?", new String[]{message.getMessageId().toString()});
        messageQueue.remove(message);
        return rowsRemoved > 0;
    }

    private void increaseResendAttempt(Message message) throws Exception {
        message.increaseResendAttempt();
        SQLiteDatabase db = this.getReadableDatabase();
        db.update(MESSAGES_TABLE_NAME, buildContent(message), MESSAGES_COLUMN_ID + "= ?", new String[]{message.getMessageId().toString()});
    }

    private ContentValues buildContent(Message message) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MESSAGES_COLUMN_LOCAL_PROFILE, message.getLocalProfilePubKey());
        contentValues.put(MESSAGES_COLUMN_REMOTE_PROFILE, message.getRemoteProfileKey());
        if (message.getMessage() != null) {
            try {
                contentValues.put(MESSAGES_COLUMN_MSG, Base64.toBase64String(message.getMessage().encode()));
                contentValues.put(MESSAGES_COLUMN_MSG_TYPE, message.getMessage().getType());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        contentValues.put(MESSAGES_COLUMN_SERVICE_NAME, message.getServiceName());
        contentValues.put(MESSAGES_COLUMN_TRY_SEND_REMOTE, String.valueOf(message.tryUpdateRemoteServices()));
        contentValues.put(MESSAGES_COLUMN_RESEND_ATTEMPTS, message.getCurrentResendingAttempts());
        contentValues.put(MESSAGES_COLUMN_ID, message.getMessageId().toString());
        contentValues.put(MESSAGES_COLUMN_TIMESTAMP, message.getTimestamp().getTime());
        return contentValues;
    }

    private Message buildFrom(Cursor cursor) {
        UUID messageId = UUID.fromString(cursor.getString(MESSAGES_POS_COLUMN_ID));
        String localProfile = cursor.getString(MESSAGES_POS_COLUMN_LOCAL_PROFILE);
        String remoteProfile = cursor.getString(MESSAGES_POS_COLUMN_REMOTE_PROFILE);
        String serviceName = cursor.getString(MESSAGES_POS_COLUMN_SERVICE_NAME);
        byte[] message = Base64.decode(cursor.getString(MESSAGES_POS_COLUMN_MSG));
        String messageType = cursor.getString(MESSAGES_POS_COLUMN_MSG_TYPE);
        boolean trySendRemote = Boolean.valueOf(cursor.getString(MESSAGES_POS_COLUMN_TRY_SEND_REMOTE));
        Integer resendAttempts = cursor.getInt(MESSAGES_POS_COLUMN_RESEND_ATTEMPTS);
        Date timestamp = new Date(cursor.getLong(MESSAGES_POS_COLUMN_TIMESTAMP));
        return new MessageImplementation(serviceName, localProfile, remoteProfile, trySendRemote, messageId, message, messageType, resendAttempts, timestamp);
    }


    private static class MessageQueueComparator implements Comparator<Message> {
        @Override
        public int compare(Message o1, Message o2) {
            return o1.getTimestamp().compareTo(o2.getTimestamp());
        }
    }

    private class MessageListener implements ProfSerMsgListener<Boolean> {

        private final Message messageInQueue;

        private MessageListener(Message message) {
            this.messageInQueue = message;
        }

        @Override
        public void onMsgFail(int messageId, int statusValue, String details) {
            failedToResend(messageInQueue);
        }

        @Override
        public String getMessageName() {
            return messageInQueue.getMessageId().toString();
        }

        @Override
        public void onMessageReceive(int messageId, Boolean message) {
            notifyMessageSent(messageInQueue);
        }
    }
}
