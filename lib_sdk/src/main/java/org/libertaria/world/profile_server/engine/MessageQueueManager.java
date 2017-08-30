package org.libertaria.world.profile_server.engine;

import org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener;
import org.libertaria.world.profile_server.protocol.IopProfileServer;

import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 29/8/2017.
 */

public interface MessageQueueManager {

    /**
     * Put a message into the queue which will try to resend it periodically.
     *
     * @param callId {@link String} containing the ID of the call associated with this message.
     * @param token  {@link Byte[]} containing the token of this message.
     * @param msg    {@link Byte[]} containing the content of this message.
     * @return {@link UUID} that represents this message on the queue.
     */
    Message enqueueMessage(String callId, byte[] token, byte[] msg);

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

    /**
     * Retrieves the default version of the queue manager's listener.
     *
     * @param message the message associated with this listener.
     * @return
     */
    ProfSerMsgListener<IopProfileServer.ApplicationServiceSendMessageResponse> buildDefaultQueueListener(final Message message);

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

    interface Message {

        UUID getMessageId();

        String getCallId();

        byte[] getToken();

        byte[] getMsg();

        Integer getCurrentResendingAttempts();

        Date getTimestamp();

        void increaseResendAttempt();
    }
}
