package iop.org.iop_sdk_android.core.service.db.message_queue;

import org.libertaria.world.profile_server.engine.MessageQueueManager;
import org.libertaria.world.profile_server.engine.app_services.BaseMsg;

import java.util.Date;
import java.util.UUID;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 30/8/2017.
 */

class MessageImplementation implements MessageQueueManager.Message {

    private String serviceName;
    private String localProfilePubKey;
    private String remoteProfile;
    private boolean tryUpdateRemoteServices;
    private UUID messageId;
    private BaseMsg baseMsg;
    private Integer resendingAttempts;
    private Date timestamp;

    public MessageImplementation(String serviceName, String localProfilePubKey, String remoteProfile, boolean tryUpdateRemoteServices, BaseMsg baseMsg) {
        this.serviceName = serviceName;
        this.localProfilePubKey = localProfilePubKey;
        this.remoteProfile = remoteProfile;
        this.tryUpdateRemoteServices = tryUpdateRemoteServices;
        this.messageId = UUID.randomUUID();
        this.timestamp = new Date();
        this.resendingAttempts = 0;
        this.baseMsg = baseMsg;
    }

    public MessageImplementation(String serviceName, String localProfilePubKey, String remoteProfile, boolean tryUpdateRemoteServices, UUID messageId, byte[] msg, String msgType, Integer resendingAttempts, Date timestamp) {
        this.serviceName = serviceName;
        this.localProfilePubKey = localProfilePubKey;
        this.remoteProfile = remoteProfile;
        this.tryUpdateRemoteServices = tryUpdateRemoteServices;
        this.messageId = messageId;
        try {
            setMessage(msg, msgType);
        } catch (Exception e) {
        }
        this.resendingAttempts = resendingAttempts;
        this.timestamp = timestamp;
    }

    public MessageImplementation(String serviceName, String localProfilePubKey, String remoteProfile, boolean tryUpdateRemoteServices, UUID messageId, BaseMsg<?> baseMsg, Integer resendingAttempts, Date timestamp) {
        this.serviceName = serviceName;
        this.localProfilePubKey = localProfilePubKey;
        this.remoteProfile = remoteProfile;
        this.tryUpdateRemoteServices = tryUpdateRemoteServices;
        this.messageId = messageId;
        this.baseMsg = baseMsg;
        this.resendingAttempts = resendingAttempts;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageImplementation that = (MessageImplementation) o;

        if (tryUpdateRemoteServices != that.tryUpdateRemoteServices) return false;
        if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null)
            return false;
        if (localProfilePubKey != null ? !localProfilePubKey.equals(that.localProfilePubKey) : that.localProfilePubKey != null)
            return false;
        if (remoteProfile != null ? !remoteProfile.equals(that.remoteProfile) : that.remoteProfile != null)
            return false;
        if (messageId != null ? !messageId.equals(that.messageId) : that.messageId != null)
            return false;
        if (resendingAttempts != null ? !resendingAttempts.equals(that.resendingAttempts) : that.resendingAttempts != null)
            return false;
        return timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null;

    }

    @Override
    public int hashCode() {
        int result = serviceName != null ? serviceName.hashCode() : 0;
        result = 31 * result + (localProfilePubKey != null ? localProfilePubKey.hashCode() : 0);
        result = 31 * result + (remoteProfile != null ? remoteProfile.hashCode() : 0);
        result = 31 * result + (tryUpdateRemoteServices ? 1 : 0);
        result = 31 * result + (messageId != null ? messageId.hashCode() : 0);
        result = 31 * result + (resendingAttempts != null ? resendingAttempts.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MessageImplementation{" +
                "serviceName='" + serviceName + '\'' +
                ", localProfilePubKey='" + localProfilePubKey + '\'' +
                ", remoteProfile=" + remoteProfile +
                ", tryUpdateRemoteServices=" + tryUpdateRemoteServices +
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
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String getLocalProfilePubKey() {
        return localProfilePubKey;
    }

    @Override
    public String getRemoteProfileKey() {
        return remoteProfile;
    }

    public void setBaseMsg(BaseMsg baseMsg) {
        this.baseMsg = baseMsg;
    }

    @Override
    public BaseMsg getMessage() {
        return baseMsg;
    }

    //I really tried to generify this method but it's impossible :(
    public void setMessage(byte[] bytes, final String messageType) throws Exception {
        BaseMsg baseMsg = new BaseMsg() {
            @Override
            public String getType() {
                return messageType;
            }
        };
        this.baseMsg = (BaseMsg) baseMsg.decode(bytes);
    }

    @Override
    public boolean tryUpdateRemoteServices() {
        return tryUpdateRemoteServices;
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
