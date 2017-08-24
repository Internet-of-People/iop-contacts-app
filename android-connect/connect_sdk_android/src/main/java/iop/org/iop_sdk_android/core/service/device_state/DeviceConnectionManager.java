package iop.org.iop_sdk_android.core.service.device_state;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import org.libertaria.world.connection.ConnectionType;
import org.libertaria.world.connection.DeviceNetworkConnection;
import org.libertaria.world.connection.NetworkSignalStrength;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 24/8/2017.
 */

public class DeviceConnectionManager implements DeviceNetworkConnection {

    private Boolean connected;
    private ConnectionType connectionType;
    private NetworkSignalStrength networkSignalStrength;
    private final Context context;

    public DeviceConnectionManager(Context context) {
        this.context = context;
        monitorSignalStrength();
    }

    @Override
    public Boolean isConnected() {
        defineConnectionStatus();
        return connected;
    }

    @Override
    public ConnectionType getConnectionType() {
        defineConnectionStatus();
        return connectionType;
    }

    @Override
    public NetworkSignalStrength getSignalStrength() {
        return networkSignalStrength;
    }

    private void defineConnectionStatus() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null) {
            connected = false;
            connectionType = ConnectionType.NONE;
            networkSignalStrength = NetworkSignalStrength.DISCONNECTED;
        } else {
            connected = activeNetwork.isConnectedOrConnecting();
            setConnectionType(activeNetwork);
        }
    }

    private void setConnectionType(NetworkInfo activeNetwork) {
        switch (activeNetwork.getType()) {
            case ConnectivityManager.TYPE_ETHERNET:
                connectionType = ConnectionType.ETHERNET;
                break;
            case ConnectivityManager.TYPE_MOBILE:
                switch (activeNetwork.getSubtype()) {
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                    case TelephonyManager.NETWORK_TYPE_IDEN:
                        connectionType = ConnectionType.GSM;
                        break;
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    case TelephonyManager.NETWORK_TYPE_EHRPD:
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                        connectionType = ConnectionType.THREEG;
                        break;
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        connectionType = ConnectionType.FOURG;
                        break;
                    default:
                        connectionType = ConnectionType.THREEG;
                        break;
                }
                break;
            case ConnectivityManager.TYPE_WIFI:
                connectionType = ConnectionType.WIFI;
                break;
        }
    }

    private void monitorSignalStrength() {
        DeviceStateListener deviceStateListener = new DeviceStateListener();
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(deviceStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }
}