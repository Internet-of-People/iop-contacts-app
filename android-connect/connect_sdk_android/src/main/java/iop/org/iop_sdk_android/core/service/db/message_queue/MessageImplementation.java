package iop.org.iop_sdk_android.core.service.db.message_queue;

import org.libertaria.world.profile_server.engine.MessageQueueManager;

import java.util.Date;
import java.util.UUID;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 30/8/2017.
 */

class MessageImplementation implements MessageQueueManager.Message {

    private String callId;
    private byte[] token;
    private byte[] msg;
    private UUID messageId;
    private Integer resendingAttempts;
    private Date timestamp;

    public MessageImplementation(String callId, byte[] token, byte[] msg, UUID messageId, Integer resendingAttempts, Date timestamp) {
        this.callId = callId;
        this.token = token;
        this.msg = msg;
        this.messageId = messageId;
        this.resendingAttempts = resendingAttempts;
        this.timestamp = timestamp;
    }

    @Override
    public UUID getMessageId() {
        return messageId;
    }

    @Override
    public String getCallId() {
        return callId;
    }

    @Override
    public byte[] getToken() {
        return token;
    }

    @Override
    public byte[] getMsg() {
        return msg;
    }

    @Override
    public Integer getCurrentResendingAttempts() {
        return resendingAttempts;
    }

    @Override
    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public void increaseResendAttempt() {
        resendingAttempts++;
    }
}
