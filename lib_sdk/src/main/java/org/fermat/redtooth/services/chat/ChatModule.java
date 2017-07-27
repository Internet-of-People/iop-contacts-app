package org.fermat.redtooth.services.chat;

import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.services.chat.ChatCallAlreadyOpenException;
import org.fermat.redtooth.services.chat.RequestChatException;

import java.util.concurrent.TimeUnit;

import org.fermat.redtooth.global.Module;

/**
 * Created by furszy on 7/20/17.
 */

public interface ChatModule extends Module {

    /**
     * Request chat
     * todo: add timeout..
     *
     * @param remoteProfileInformation
     * @param readyListener
     */
    void requestChat(final String localProfilePubKey, final ProfileInformation remoteProfileInformation, final ProfSerMsgListener<Boolean> readyListener) throws RequestChatException, ChatCallAlreadyOpenException;

    void acceptChatRequest(String localProfilePubKey, String remoteHexPublicKey, ProfSerMsgListener<Boolean> future) throws Exception;

    void refuseChatRequest(String localProfilePubKey, String remoteHexPublicKey);

    void sendMsgToChat(String localProfilePubKey, ProfileInformation remoteProfileInformation, String msg, ProfSerMsgListener<Boolean> msgListener) throws Exception;

    boolean isChatActive(String localProfilePubKey, String remotePk);
}
