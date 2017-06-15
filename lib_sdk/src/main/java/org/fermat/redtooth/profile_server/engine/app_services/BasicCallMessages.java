package org.fermat.redtooth.profile_server.engine.app_services;

/**
 * Created by furszy on 6/15/17.
 */

public enum BasicCallMessages {

    CRYPTO("crypto");

    private String type;

    BasicCallMessages(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static BasicCallMessages getByType(String type){
        for (BasicCallMessages basicCallMessages : BasicCallMessages.values()) {
            if (basicCallMessages.getType().equals(type)){
                return basicCallMessages;
            }
        }
        return null;
    }
}
