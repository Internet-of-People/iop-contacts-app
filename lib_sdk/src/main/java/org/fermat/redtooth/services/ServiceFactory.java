package org.fermat.redtooth.services;

import org.fermat.redtooth.profile_server.engine.app_services.AppService;

/**
 * Created by furszy on 7/31/17.
 */

public interface ServiceFactory {

    AppService buildOrGetService(String serviceName);

}
