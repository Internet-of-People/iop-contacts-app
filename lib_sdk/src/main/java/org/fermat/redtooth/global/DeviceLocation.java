package org.fermat.redtooth.global;

/**
 * Created by furszy on 6/9/17.
 */

public interface DeviceLocation {

    boolean isDeviceLocationEnabled();

    GpsLocation getDeviceLocation();

}
