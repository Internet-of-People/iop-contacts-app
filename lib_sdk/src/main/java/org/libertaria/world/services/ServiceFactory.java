package org.libertaria.world.services;

/**
 * Created by furszy on 7/31/17.
 */

public interface ServiceFactory {

    org.libertaria.world.profile_server.engine.app_services.AppService buildOrGetService(String serviceName);

}
