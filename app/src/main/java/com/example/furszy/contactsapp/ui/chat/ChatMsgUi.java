package com.example.furszy.contactsapp.ui.chat;

/**
 * Created by furszy on 7/6/17.
 */

public class ChatMsgUi {

    private boolean isMine;
    private String text;
    private long timestamp;

    public ChatMsgUi(boolean isMine, String text,long timestamp) {
        this.isMine = isMine;
        this.text = text;
        this.timestamp = timestamp;
    }

    public boolean isMine() {
        return isMine;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
