package org.fermat.redtooth.core.services;


/**
 * Created by furszy on 6/4/17.
 */

public enum DefaultServices {

    PROFILE_PAIRING("prof_pair");

    private String name;

    DefaultServices(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static DefaultServices getServiceByName(String name){
        switch (name){
            case "prof_pair":
                return PROFILE_PAIRING;
            default:
                throw new IllegalArgumentException("service with name: "+name+" not found");
        }
    }
}
