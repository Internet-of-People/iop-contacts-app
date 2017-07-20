package iop.org.iop_sdk_android.core.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import iop.org.iop_sdk_android.core.service.IoPConnectService;
import iop.org.iop_sdk_android.core.service.ProfileServerConfigurationsImp;


public class ReceiverBoot extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ProfileServerConfigurationsImp configurationsPreferences = new ProfileServerConfigurationsImp(context,context.getSharedPreferences(ProfileServerConfigurationsImp.PREFS_NAME,0));
        if (configurationsPreferences.getBackgroundServiceEnable()) {
            Intent serviceIntent = new Intent(context,IoPConnectService.class);
            serviceIntent.setAction(IoPConnectService.ACTION_BOOT_SERVICE);
            context.startService(serviceIntent);
        }
    }
}