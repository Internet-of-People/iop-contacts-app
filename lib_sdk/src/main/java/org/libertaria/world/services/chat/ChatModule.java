package org.libertaria.world.services.chat;

/**
 * Created by furszy on 7/20/17.
 */

public interface ChatModule extends org.libertaria.world.global.Module {

    /**
     * Request chat
     * todo: add timeout..
     *
     * @param remoteProfileInformation
     * @param readyListener
     */
    void requestChat(final String localProfilePubKey, final org.libertaria.world.profile_server.ProfileInformation remoteProfileInformation, final org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<Boolean> readyListener) throws RequestChatException, ChatCallAlreadyOpenException;

    void acceptChatRequest(String localProfilePubKey, String remoteHexPublicKey, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<Boolean> future) throws Exception;

    void refuseChatRequest(String localProfilePubKey, String remoteHexPublicKey);

    void sendMsgToChat(String localProfilePubKey, org.libertaria.world.profile_server.ProfileInformation remoteProfileInformation, String msg, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<Boolean> msgListener) throws Exception;

    boolean isChatActive(String localProfilePubKey, String remotePk);
}
