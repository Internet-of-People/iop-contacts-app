package org.fermat.redtooth.profile_server.engine.futures;

import org.fermat.redtooth.profile_server.model.ProfServerData;

/**
 * Created by furszy on 6/26/17.
 */

public class ConnectionFuture extends MsgListenerFuture<Boolean> {

    private ProfServerData profServerData;

    public void setProfServerData(ProfServerData profServerData) {
        this.profServerData = profServerData;
    }

    public ProfServerData getProfServerData() {
        return profServerData;
    }
}
