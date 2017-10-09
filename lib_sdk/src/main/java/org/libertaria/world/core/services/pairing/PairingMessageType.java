package org.libertaria.world.core.services.pairing;

/**
 * Created by furszy on 6/5/17.
 */

public enum PairingMessageType {

    PAIR_REQUEST("pr"),
    PAIR_ACCEPT("pa"),
    PAIR_REFUSE("prr"),
    PAIR_DISCONNECT("pd");

    private String type;

    PairingMessageType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static PairingMessageType getByName(String type) {
        for (PairingMessageType pairingMessageType : PairingMessageType.values()) {
            if (pairingMessageType.getType().equals(type)) {
                return pairingMessageType;
            }
        }
        throw new IllegalArgumentException("Invalid type");
    }
}
