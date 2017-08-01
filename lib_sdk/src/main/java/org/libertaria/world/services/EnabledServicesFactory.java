package org.libertaria.world.services;

import org.libertaria.world.global.Module;
import org.libertaria.world.services.chat.ChatAppService;

/**
 * Created by furszy on 7/3/17.
 */

public class EnabledServicesFactory {

    public static final org.libertaria.world.profile_server.engine.app_services.AppService buildService(String serviceName, Module module, Object... args){
        org.libertaria.world.profile_server.engine.app_services.AppService appService = null;
        switch (EnabledServices.getServiceByName(serviceName)){
            case CHAT:
                appService = new ChatAppService();
                ((ChatAppService)appService).addListener((org.libertaria.world.services.chat.ChatMsgListener) module);
                break;
            default:
                throw new IllegalArgumentException("Service with name: "+serviceName+" not enabled.");
        }
        return appService;
    }

}
