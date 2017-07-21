package iop.org.iop_sdk_android.core.service.modules.interfaces;

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
    void requestChat(final Profile localProfile, final ProfileInformation remoteProfileInformation, final ProfSerMsgListener<Boolean> readyListener, TimeUnit timeUnit, long time) throws RequestChatException, ChatCallAlreadyOpenException;

    void acceptChatRequest(Profile localProfile, String remoteHexPublicKey, ProfSerMsgListener<Boolean> future) throws Exception;

    void refuseChatRequest(Profile localProfile, String remoteHexPublicKey);

    void sendMsgToChat(Profile localProfile, ProfileInformation remoteProfileInformation, String msg, ProfSerMsgListener<Boolean> msgListener) throws Exception;

    boolean isChatActive(Profile localProfile, String remotePk);
}
