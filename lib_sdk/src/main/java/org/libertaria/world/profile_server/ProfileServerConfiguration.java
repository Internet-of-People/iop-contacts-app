package org.libertaria.world.profile_server;

import org.libertaria.world.profile_server.model.ProfServerData;

import java.util.List;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 20/12/2017.
 */

public interface ProfileServerConfiguration {

    /**
     * The configuration for the selected profile server by the user.
     *
     * @return
     */
    ProfServerData getSelectedProfileServer();

    /**
     * Returns the complete list of profile servers
     *
     * @return
     */
    List<ProfServerData> getRegisteredServers();

    /**
     * Registers a new profile server to be used.
     * @param host              mandatory : the IP of the server
     * @param port
     *
     */
    ProfServerData registerNewServer(String host, Integer port);

    /**
     * Updates the server based on the parameters passed.
     *
     * @param profServerData the server configuration.
     */
    void updateServer(ProfServerData profServerData);

    /**
     * Sets this profile as the selected profile
     *
     * @param profServerData
     */
    void selectServer(ProfServerData profServerData);
}
