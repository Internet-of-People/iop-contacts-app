package iop.org.iop_sdk_android.core.service.client_broker;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by furszy on 7/19/17.
 *
 * Connect sdk broker pattern client side.
 *
 */

public class ConnectClientService extends Service{

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
