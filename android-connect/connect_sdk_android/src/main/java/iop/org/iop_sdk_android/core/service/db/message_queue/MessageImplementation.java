package iop.org.iop_sdk_android.core.service.db.message_queue;

import org.libertaria.world.profile_server.engine.MessageQueueManager;

import java.util.Arrays;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageImplementation that = (MessageImplementation) o;

        if (callId != null ? !callId.equals(that.callId) : that.callId != null) return false;
        if (!Arrays.equals(token, that.token)) return false;
        return Arrays.equals(msg, that.msg);

    }

    @Override
    public int hashCode() {
        int result = callId != null ? callId.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(token);
        result = 31 * result + Arrays.hashCode(msg);
        return result;
    }

    @Override
    public String toString() {
        return "MessageImplementation{" +
                "callId='" + callId + '\'' +
                ", token=" + Arrays.toString(token) +
                ", msg=" + Arrays.toString(msg) +
                ", messageId=" + messageId +
                ", resendingAttempts=" + resendingAttempts +
                ", timestamp=" + timestamp +
                '}';
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
