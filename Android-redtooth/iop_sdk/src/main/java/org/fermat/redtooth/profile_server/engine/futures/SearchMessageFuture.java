package org.fermat.redtooth.profile_server.engine.futures;

import java.util.List;

import org.fermat.redtooth.profile_server.engine.SearchProfilesQuery;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerPartSearchListener;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;

/**
 * Created by mati on 31/03/17.
 */

public class SearchMessageFuture <O extends List<IopProfileServer.IdentityNetworkProfileInformation>> extends BaseMsgFuture<O> implements ProfSerMsgListener<O> {

    private SearchProfilesQuery searchProfilesQuery;

    public SearchMessageFuture(SearchProfilesQuery searchProfilesQuery) {
        this.searchProfilesQuery = searchProfilesQuery;
    }

    @Override
    public void onMessageReceive(int messageId, O message) {
        this.messageId = messageId;
        this.searchProfilesQuery.addListToChache(0,message);
        queue.offer(message);
    }

    @Override
    public void onMsgFail(int messageId, int statusValue, String details) {
        this.status = statusValue;
        this.statusDetail = details;
        queue.offer(null);
    }

}
