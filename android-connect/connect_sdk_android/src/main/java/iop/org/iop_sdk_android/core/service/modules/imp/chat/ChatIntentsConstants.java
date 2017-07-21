package iop.org.iop_sdk_android.core.service.modules.imp.chat;

/**
 * Created by furszy on 7/20/17.
 */

public class ChatIntentsConstants {

    public static final String ACTION_ON_CHAT_CONNECTED = "org.furszy.broadcast.on_chat_connected";
    public static final String ACTION_ON_CHAT_DISCONNECTED = "org.furszy.broadcast.on_chat_disconnected";
    public static final String ACTION_ON_CHAT_MSG_RECEIVED = "org.furszy.broadcast.on_chat_msg_received";

    public static final String EXTRA_INTENT_LOCAL_PROFILE = "loc_profile";
    public static final String EXTRA_INTENT_REMOTE_PROFILE = "rem_profile";
    public static final String EXTRA_INTENT_IS_LOCAL_CREATOR = "is_loc_creator";
    public static final String EXTRA_INTENT_CHAT_MSG = "chat_msg";
    public static final String EXTRA_INTENT_DETAIL = "detail";

}
