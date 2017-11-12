package iop.org.iop_sdk_android.core.service.device_state;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 10/11/2017.
 */

public class ContactLocationListener implements LocationListener {

    private static final ContactLocationListener ourInstance = new ContactLocationListener();


    public static ContactLocationListener getInstance() {
        return ourInstance;
    }

    private ContactLocationListener() {
    }

    @Override
    public void onLocationChanged(Location location) {
        //Do something.
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
