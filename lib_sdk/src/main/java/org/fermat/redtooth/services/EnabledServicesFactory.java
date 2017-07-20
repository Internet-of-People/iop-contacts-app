package org.fermat.redtooth.services;

import org.fermat.redtooth.core.services.AppServiceListener;
import org.fermat.redtooth.profile_server.engine.app_services.AppService;
import org.fermat.redtooth.services.chat.ChatMsgListener;

/**
 * Created by furszy on 7/3/17.
 */

public class EnabledServicesFactory {

    public static final AppService buildService(String serviceName,Object... args){
        AppService appService = null;
        switch (EnabledServices.getServiceByName(serviceName)){
            case CHAT:
                appService = new org.fermat.redtooth.services.chat.ChatAppService();
                ((org.fermat.redtooth.services.chat.ChatAppService)appService).addListener((ChatMsgListener) args[0]);
                break;
            default:
                throw new IllegalArgumentException("Service with name: "+serviceName+" not enabled.");
        }
        return appService;
    }

}
