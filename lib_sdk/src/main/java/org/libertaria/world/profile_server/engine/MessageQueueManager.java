package org.libertaria.world.profile_server.engine;

import org.libertaria.world.profile_server.engine.app_services.BaseMsg;
import org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 29/8/2017.
 * <p>
 * This interface manages the queue of message. All the responses from this interface
 * comes through events. If you are interested in listen the responses you should add a listener
 * for the events exposed on this interface: {@link MessageQueueManager#EVENT_MESSAGE_FAILED},
 * {@link MessageQueueManager#EVENT_MESSAGE_SUCCESSFUL} and {@link MessageQueueManager#EVENT_MESSAGE_ENQUEUED}.
 */

public interface MessageQueueManager {

    String EVENT_MESSAGE_ENQUEUED = "message_enqueued";
    String EVENT_MESSAGE_FAILED = "message_failed";
    String EVENT_MESSAGE_SUCCESSFUL = "message_success";

    /**
     * Put a message into the queue which will try to resend it periodically. This method
     * immediately raises a {@link MessageQueueManager#EVENT_MESSAGE_ENQUEUED} event
     * which includes a {@link Message}.
     *
     */
    void enqueueMessage(String serviceName,
    String localProfile,
    String remoteProfile,
    BaseMsg message,
    boolean tryUpdateRemoteServices);

    /**
     * Retrieves the queue of messages. Note that it's responsibility of the implementation
     * to return the messages in the correct order to be resent.
     *
     * @return {@link List} implementation with the list of messages waiting to be sent, the messages are sorted by antiquity.
     */
    List<Message> getMessageQueue();

    /**
     * Removes from the message queue the first message.
     *
     * @param messageToRemove {@link Message} the message to be removed from the queue.
     * @return {@link Boolean} whether the deletion was successful or not.
     */
    Boolean removeFromQueue(Message messageToRemove);


    void notifyMessageSent(Message messageSent);
    /**
     * Retrieves the default version of the queue manager's listener.
     *
     * @param message the message associated with this listener.
     * @return
     */
    ProfSerMsgListener<Boolean> buildDefaultQueueListener(final Message message);

    /**
     * Calling this method means that, somehow, we failed when trying to resend that message.
     * The actions to be taken with the message on the queue depends on the implementation.
     *
     * @param message {@link Message} the message of which its resend attempt failed.
     */
    void failedToResend(Message message);

    /**
     * The number of attempts to resend an individual message, if this number is reached the message
     * will be deleted from the queue and a failed to send event will be raised.
     *
     * @return {@link Integer} times we have tried to resend this message.
     */
    Integer getResendAttemptLimit();

    /**
     * Updates the maximum number of attempts to resend any message on the queue.
     *
     * @param integer resend attempt limit.
     */
    void setResendAttemptLimit(Integer integer);

    interface Message extends Serializable {

        UUID getMessageId();

        String getServiceName();

        String getLocalProfilePubKey();

        String getRemoteProfileKey();

        BaseMsg getMessage();

        boolean tryUpdateRemoteServices();

        Integer getCurrentResendingAttempts();

        Date getTimestamp();

        void increaseResendAttempt();
    }
}
