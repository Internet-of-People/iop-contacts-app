package org.libertaria.world.services;


import org.libertaria.world.global.Module;
import org.libertaria.world.services.chat.ChatModule;
import org.libertaria.world.services.interfaces.PairingModule;
import org.libertaria.world.services.interfaces.ProfilesModule;

/**
 * Created by furszy on 6/4/17.
 *
 * Improvements:
 * Add versioning to modules.
 */

public enum EnabledServices {

    PROFILE_DATA("prof_data", ProfilesModule.class),
    PROFILE_PAIRING("prof_pair", PairingModule.class),
    CHAT("chat", ChatModule.class)
    ;

    private String name;
    private Class<Module> moduleClass;

    EnabledServices(String name,Class moduleClass) {
        this.name = name;
        this.moduleClass = moduleClass;
    }

    public String getName() {
        return name;
    }

    public Class<Module> getModuleClass() {
        return moduleClass;
    }

    public static EnabledServices getServiceByName(String name){
        switch (name){
            case "prof_data":
                return PROFILE_DATA;
            case "prof_pair":
                return PROFILE_PAIRING;
            case "chat":
                return CHAT;
            default:
                throw new IllegalArgumentException("service with name: "+name+" not found");
        }
    }
}
