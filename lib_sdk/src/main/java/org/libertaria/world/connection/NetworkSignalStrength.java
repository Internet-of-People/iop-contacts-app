package org.libertaria.world.connection;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 23/8/2017.
 */

public enum NetworkSignalStrength {
    //ENUM DECLARATION
    EXCELLENT,
    HIGH,
    MEDIUM,
    POOR,
    DISCONNECTED;
    //VARIABLE DECLARATION

    //CONSTRUCTORS

    //PUBLIC METHODS
    @Override
    public String toString() {
        return this.name();
    }

    //PRIVATE METHODS
}
