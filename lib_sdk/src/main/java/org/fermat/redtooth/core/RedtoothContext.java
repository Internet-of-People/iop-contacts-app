package org.fermat.redtooth.core;

import org.fermat.redtooth.profile_server.ProfileServerConfigurations;

/**
 * Created by mati on 09/05/17.
 */

public interface RedtoothContext {

    /**
     * Create an empty profile server configuration
     * @return
     */
    ProfileServerConfigurations createProfSerConfig();

}
