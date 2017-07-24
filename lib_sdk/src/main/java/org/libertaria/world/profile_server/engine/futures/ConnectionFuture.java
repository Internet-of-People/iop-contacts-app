package org.libertaria.world.profile_server.engine.futures;

/**
 * Created by furszy on 6/26/17.
 */

public class ConnectionFuture extends MsgListenerFuture<Boolean> {

    private org.libertaria.world.profile_server.model.ProfServerData profServerData;

    public void setProfServerData(org.libertaria.world.profile_server.model.ProfServerData profServerData) {
        this.profServerData = profServerData;
    }

    public org.libertaria.world.profile_server.model.ProfServerData getProfServerData() {
        return profServerData;
    }
}
