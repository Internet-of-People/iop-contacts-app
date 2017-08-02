package org.libertaria.world.core.services.pairing;

/**
 * Created by furszy on 6/5/17.
 */

public enum PairingMsgTypes {

    PAIR_REQUEST("pr"),PAIR_ACCEPT("pa"),PAIR_REFUSE("prr"), PAIR_DISCONNECT("pd");

    private String type;

    PairingMsgTypes(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static PairingMsgTypes getByName(String type){
        for (PairingMsgTypes pairingMsgTypes : PairingMsgTypes.values()) {
            if (pairingMsgTypes.getType().equals(type)){
                return pairingMsgTypes;
            }
        }
        throw new IllegalArgumentException("Invalid type");
    }
}
