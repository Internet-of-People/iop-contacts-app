package iop.org.iop_sdk_android.core.service.device_state;

import android.os.Build;
import android.telephony.CellSignalStrength;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;

import org.libertaria.world.connection.NetworkSignalStrength;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 24/8/2017.
 */

class DeviceStateListener extends PhoneStateListener {

    private NetworkSignalStrength iopNetworkSignalStrength;

    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        super.onSignalStrengthsChanged(signalStrength);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            setSignalStrengthAPIPlus23(signalStrength);
        } else {
            setSignalStrenthAPIMinus23(signalStrength);
        }
    }

    private void setSignalStrenthAPIMinus23(SignalStrength signalStrength) {
        if (signalStrength.isGsm()) {
            int asu = signalStrength.getGsmSignalStrength();
            if (asu == 0 || asu == 99) {
                iopNetworkSignalStrength = NetworkSignalStrength.DISCONNECTED;
            } else if (asu < 5) {
                iopNetworkSignalStrength = NetworkSignalStrength.POOR;
            } else if (asu >= 5 && asu < 8) {
                iopNetworkSignalStrength = NetworkSignalStrength.MEDIUM;
            } else if (asu >= 8 && asu < 12) {
                iopNetworkSignalStrength = NetworkSignalStrength.HIGH;
            } else if (asu >= 12) {
                iopNetworkSignalStrength = NetworkSignalStrength.EXCELLENT;
            }
        } else {
            int cdmaDbm = signalStrength.getCdmaDbm();
            if (cdmaDbm >= -97) {
                iopNetworkSignalStrength = NetworkSignalStrength.EXCELLENT;
            } else if (cdmaDbm >= -103) {
                iopNetworkSignalStrength = NetworkSignalStrength.HIGH;
            } else if (cdmaDbm >= -107) {
                iopNetworkSignalStrength = NetworkSignalStrength.MEDIUM;
            } else if (cdmaDbm >= -109) {
                iopNetworkSignalStrength = NetworkSignalStrength.POOR;
            } else {
                iopNetworkSignalStrength = NetworkSignalStrength.DISCONNECTED;
            }
        }
    }

    private void setSignalStrengthAPIPlus23(SignalStrength signalStrength) {
        switch (signalStrength.getLevel()) {
            case CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN:
                iopNetworkSignalStrength = NetworkSignalStrength.DISCONNECTED;
                break;
            case CellSignalStrength.SIGNAL_STRENGTH_POOR:
                iopNetworkSignalStrength = NetworkSignalStrength.POOR;
                break;
            case CellSignalStrength.SIGNAL_STRENGTH_MODERATE:
                iopNetworkSignalStrength = NetworkSignalStrength.MEDIUM;
                break;
            case CellSignalStrength.SIGNAL_STRENGTH_GOOD:
                iopNetworkSignalStrength = NetworkSignalStrength.HIGH;
                break;
            case CellSignalStrength.SIGNAL_STRENGTH_GREAT:
                iopNetworkSignalStrength = NetworkSignalStrength.EXCELLENT;
                break;
        }
    }

    NetworkSignalStrength getIopNetworkSignalStrength() {
        return iopNetworkSignalStrength;
    }
}
