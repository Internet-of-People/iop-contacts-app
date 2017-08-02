package org.libertaria.world.services;

import org.libertaria.world.profile_server.engine.app_services.AppService;

/**
 * Created by furszy on 7/31/17.
 */

public interface ServiceFactory {

    AppService buildOrGetService(String serviceName);

}
