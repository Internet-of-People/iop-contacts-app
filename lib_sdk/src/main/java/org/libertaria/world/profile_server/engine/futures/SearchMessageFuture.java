package org.libertaria.world.profile_server.engine.futures;

import org.libertaria.world.profile_server.protocol.IopProfileServer;

import java.util.List;

/**
 * Created by mati on 31/03/17.
 */

public class SearchMessageFuture <O extends List<IopProfileServer.ProfileQueryInformation>> extends BaseMsgFuture<O> implements org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<O> {

    private org.libertaria.world.profile_server.engine.SearchProfilesQuery searchProfilesQuery;

    public SearchMessageFuture(org.libertaria.world.profile_server.engine.SearchProfilesQuery searchProfilesQuery) {
        this.searchProfilesQuery = searchProfilesQuery;
    }

    @Override
    public void onMessageReceive(int messageId, O message) {
        synchronized (reentrantLock) {
            this.messageId = messageId;
            this.searchProfilesQuery.addListToChache(0, message);
            object = message;
            reentrantLock.notifyAll();
        }
    }

    @Override
    public void onMsgFail(int messageId, int statusValue, String details) {
        synchronized(reentrantLock) {
            this.status = statusValue;
            this.statusDetail = details;
            reentrantLock.notifyAll();
        }
    }

}
