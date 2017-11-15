package world.libertaria.sdk.android.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by mati on 08/05/17.
 */

public class ClientServiceConnectHelper {

    public static final String TAG = "ClientConnHelper";

    private Context application;
    private ConnectClientService clientService;

    public static AtomicBoolean isConnected = new AtomicBoolean(false);
    private InitListener listener;
    private Intent intent;


    public static ClientServiceConnectHelper init(Context context, InitListener initListener) {
        ClientServiceConnectHelper anRedtooth = new ClientServiceConnectHelper(context);
        anRedtooth.setListener(initListener);
        anRedtooth.startProfileServerService();
        return anRedtooth;
    }

    private ClientServiceConnectHelper(Context context) {
        this.application = context;
    }

    public ServiceConnection profServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d(TAG, "profile service connected " + className);
            isConnected.set(true);
            clientService = ((ConnectClientService.ConnectBinder) binder).getService();
            listener.onConnected();
        }
        //binder comes from server to communicate with method's of

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "profile service disconnected " + className);
            isConnected.set(false);
            profServiceConnection = null;
            listener.onDisconnected();
        }
    };

    //
    public void startProfileServerService() {
        intent = new Intent(application, ConnectClientService.class);
        application.bindService(intent, profServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public ConnectClientService getClient() {
        return clientService;
    }

    public void setListener(InitListener listener) {
        this.listener = listener;
    }
}
