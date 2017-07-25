package iop.org.iop_sdk_android.core.utils;

import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;

/**
 * Created by furszy on 7/25/17.
 */

public class EmptyListener<I> implements ProfSerMsgListener<I> {

    ProfSerMsgListener<Boolean> profSerMsgListener;

    public EmptyListener(ProfSerMsgListener<Boolean> profSerMsgListener) {
        this.profSerMsgListener = profSerMsgListener;
    }

    @Override
    public void onMsgFail(int messageId, int statusValue, String details) {
        profSerMsgListener.onMsgFail(messageId,statusValue,details);
    }

    @Override
    public String getMessageName() {
        return profSerMsgListener.getMessageName();
    }

    @Override
    public void onMessageReceive(int messageId, I message) {
        profSerMsgListener.onMessageReceive(messageId,true);
    }
}
