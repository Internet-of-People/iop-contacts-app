package iop.org.iop_sdk_android.core.service.db.message_queue;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.libertaria.world.profile_server.engine.MessageQueueManager;
import org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener;
import org.libertaria.world.profile_server.protocol.IopProfileServer;
import org.spongycastle.util.encoders.Base64;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by furszy on 5/25/17.
 */

public class MessageQueueDb extends SQLiteOpenHelper implements MessageQueueManager {


    public static final int DATABASE_VERSION = 12;

    public static final String DATABASE_NAME = "message_queue";

    public static final String MESSAGES_TABLE_NAME = "messages";
    public static final String MESSAGES_COLUMN_ID = "message_id";
    public static final String MESSAGES_COLUMN_CALL_ID = "call_id";
    public static final String MESSAGES_COLUMN_TOKEN = "token";
    public static final String MESSAGES_COLUMN_MSG = "msg";
    public static final String MESSAGES_COLUMN_RESEND_ATTEMPTS = "resend_attempts";
    public static final String MESSAGES_COLUMN_TIMESTAMP = "timestamp";

    public static final int MESSAGES_POS_COLUMN_ID = 0;
    public static final int MESSAGES_POS_COLUMN_CALL_ID = 1;
    public static final int MESSAGES_POS_COLUMN_TOKEN = 2;
    public static final int MESSAGES_POS_COLUMN_MSG = 3;
    public static final int MESSAGES_POS_COLUMN_RESEND_ATTEMPTS = 4;
    public static final int MESSAGES_POS_COLUMN_TIMESTAMP = 5;

    private List<Message> messageQueue;
    private final MessageQueueComparator DEFAULT_COMPARATOR = new MessageQueueComparator();
    private static final Integer RESEND_ATTEMPT_LIMIT = 5;

    public MessageQueueDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        messageQueue = new ArrayList<>();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + MESSAGES_TABLE_NAME +
                        "(" +
                        MESSAGES_COLUMN_ID + " TEXT PRIMARY KEY, " +
                        MESSAGES_COLUMN_CALL_ID + " TEXT, " +
                        MESSAGES_COLUMN_TOKEN + " TEXT, " +
                        MESSAGES_COLUMN_MSG + " TEXT, " +
                        MESSAGES_COLUMN_RESEND_ATTEMPTS + " INTEGER," +
                        MESSAGES_COLUMN_TIMESTAMP + " LONG)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MESSAGES_TABLE_NAME);
        onCreate(db);
    }

    @Override
    public Message enqueueMessage(String callId, byte[] token, byte[] msg) {
        return insertMessage(callId, token, msg);
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
    public ProfSerMsgListener<IopProfileServer.ApplicationServiceSendMessageResponse> buildDefaultQueueListener(Message message) {
        return new MessageListener(message);
    }

    @Override
    public void failedToResend(Message message) {
        increaseResendAttempt(message);
        if (message.getCurrentResendingAttempts() >= getResendAttemptLimit()) {
            removeFromQueue(message);
        }
    }

    @Override
    public Integer getResendAttemptLimit() {
        return RESEND_ATTEMPT_LIMIT;
    }


    private Message insertMessage(String callId, byte[] token, byte[] msg) {
        SQLiteDatabase db = this.getWritableDatabase();
        MessageImplementation message = new MessageImplementation(callId, token, msg, UUID.randomUUID(), 0, new Date());
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
                    messageQueue.add(buildFrom(res));
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

    private void increaseResendAttempt(Message message) {
        message.increaseResendAttempt();
        SQLiteDatabase db = this.getReadableDatabase();
        db.update(MESSAGES_TABLE_NAME, buildContent(message), MESSAGES_COLUMN_ID + "= ?", new String[]{message.getMessageId().toString()});
    }

    private ContentValues buildContent(Message message) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MESSAGES_COLUMN_CALL_ID, message.getCallId());
        contentValues.put(MESSAGES_COLUMN_TOKEN, Base64.encode(message.getToken()));
        contentValues.put(MESSAGES_COLUMN_MSG, Base64.encode(message.getMsg()));
        contentValues.put(MESSAGES_COLUMN_RESEND_ATTEMPTS, message.getCurrentResendingAttempts());
        contentValues.put(MESSAGES_COLUMN_ID, message.getMessageId().toString());
        contentValues.put(MESSAGES_COLUMN_TIMESTAMP, message.getTimestamp().getTime());
        return contentValues;
    }

    private Message buildFrom(Cursor cursor) {
        UUID messageId = UUID.fromString(cursor.getString(MESSAGES_POS_COLUMN_ID));
        String callId = cursor.getString(MESSAGES_POS_COLUMN_CALL_ID);
        byte[] token = Base64.decode(cursor.getString(MESSAGES_POS_COLUMN_TOKEN));
        byte[] msg = Base64.decode(cursor.getString(MESSAGES_POS_COLUMN_MSG));
        Integer resendAttempts = cursor.getInt(MESSAGES_POS_COLUMN_RESEND_ATTEMPTS);
        Date timestamp = new Date(cursor.getLong(MESSAGES_POS_COLUMN_TIMESTAMP));
        return new MessageImplementation(callId, token, msg, messageId, resendAttempts, timestamp);
    }


    private class MessageQueueComparator implements Comparator<Message> {
        @Override
        public int compare(Message o1, Message o2) {
            return o1.getTimestamp().compareTo(o2.getTimestamp());
        }
    }

    private class MessageListener implements ProfSerMsgListener<IopProfileServer.ApplicationServiceSendMessageResponse> {

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
        public void onMessageReceive(int messageId, IopProfileServer.ApplicationServiceSendMessageResponse message) {
            removeFromQueue(messageInQueue);
        }
    }
}
