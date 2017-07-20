package org.fermat.redtooth.services;


import org.fermat.redtooth.profile_server.engine.app_services.AppService;

/**
 * Created by furszy on 6/4/17.
 */

public enum EnabledServices {

    PROFILE_PAIRING("prof_pair"),
    CHAT("chat")
    ;

    private String name;

    EnabledServices(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static EnabledServices getServiceByName(String name){
        switch (name){
            case "prof_pair":
                return PROFILE_PAIRING;
            case "chat":
                return CHAT;
            default:
                throw new IllegalArgumentException("service with name: "+name+" not found");
        }
    }
}
