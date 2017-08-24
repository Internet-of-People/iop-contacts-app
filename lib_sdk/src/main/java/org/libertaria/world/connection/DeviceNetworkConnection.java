package org.libertaria.world.connection;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 23/8/2017.
 *
 * This interface represents the network connection of this device.
 *
 */

public interface DeviceNetworkConnection {

    /**
     * Checks the current network connectivity state.
     *
     * @return the current connection state, {@code true} if we have internet connection through
     * the current channel, otherwise {@code false}.
     */
    Boolean isConnected();

    /**
     *
     * @return the current connection channel used by this device represented by the enum {@link ConnectionType}
     */
    ConnectionType getConnectionType();

    /**
     *
     * @return the signal strength for the current device's network, represented in an abstract way
     * by the enum {@link NetworkSignalStrength}
     */
    NetworkSignalStrength getSignalStrength();
}
