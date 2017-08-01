package org.libertaria.world.profile_server.engine.futures;

import org.libertaria.world.profile_server.engine.SearchProfilesQuery;
import org.libertaria.world.profile_server.engine.listeners.ProfSerPartSearchListener;
import org.libertaria.world.profile_server.protocol.IopProfileServer;

import java.util.List;

/**
 * Created by mati on 31/03/17.
 */

public class SubsequentSearchMsgListenerFuture<O extends List<IopProfileServer.ProfileQueryInformation>> extends BaseMsgFuture<O> implements ProfSerPartSearchListener<O> {

    private SearchProfilesQuery searchProfilesQuery;

    public SubsequentSearchMsgListenerFuture(SearchProfilesQuery searchProfilesQuery) {
        this.searchProfilesQuery = searchProfilesQuery;
    }

    @Override
    public void onMessageReceive(int messageId, O message, int recordIndex, int recordCount) {
        this.messageId = messageId;
        this.searchProfilesQuery.setLastRecordIndex(recordIndex);
        this.searchProfilesQuery.setLastRecordCount(recordCount);
        this.searchProfilesQuery.addListToChache(recordIndex,message);
        object = message;
        reentrantLock.notifyAll();
    }

    @Override
    public void onMsgFail(int messageId, int statusValue, String details) {
        this.status = statusValue;
        this.statusDetail = details;
        reentrantLock.notifyAll();
    }

    @Override
    public void onMessageReceive(int messageId, Object message) {

    }
}
