package org.fermat.redtooth.services;

import org.fermat.redtooth.global.Module;
import org.fermat.redtooth.profile_server.engine.app_services.AppService;
import org.fermat.redtooth.services.chat.ChatMsgListener;

/**
 * Created by furszy on 7/3/17.
 */

public class EnabledServicesFactory {

    public static final AppService buildService(String serviceName, Module module, Object... args){
        AppService appService = null;
        switch (EnabledServices.getServiceByName(serviceName)){
            case CHAT:
                appService = new org.fermat.redtooth.services.chat.ChatAppService();
                ((org.fermat.redtooth.services.chat.ChatAppService)appService).addListener((ChatMsgListener) module);
                break;
            default:
                throw new IllegalArgumentException("Service with name: "+serviceName+" not enabled.");
        }
        return appService;
    }

}
