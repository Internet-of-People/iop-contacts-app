package org.fermat.redtooth.services.chat;

/**
 * Created by furszy on 7/4/17.
 */

public class ChatMsgWrapper {

    private String fromPubKey;
    private String toPubKey;
    private ChatMsg chatMsg;

    public ChatMsgWrapper(String fromPubKey, String toPubKey, ChatMsg chatMsg) {
        this.fromPubKey = fromPubKey;
        this.toPubKey = toPubKey;
        this.chatMsg = chatMsg;
    }

    public String getFromPubKey() {
        return fromPubKey;
    }

    public String getToPubKey() {
        return toPubKey;
    }

    public ChatMsg getChatMsg() {
        return chatMsg;
    }
}
