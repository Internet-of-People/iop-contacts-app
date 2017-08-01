package world.libertaria.sdk.android.client;

import android.app.Application;
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

    private Application application;
    private ConnectClientService clientService;

    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private InitListener listener;


    public static ClientServiceConnectHelper init(Application application, InitListener initListener) {
        ClientServiceConnectHelper anRedtooth = new ClientServiceConnectHelper(application);
        anRedtooth.setListener(initListener);
        anRedtooth.startProfileServerService();
        return anRedtooth;
    }

    private ClientServiceConnectHelper(Application application) {
        this.application = application;
    }

    public ServiceConnection profServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d(TAG,"profile service connected "+className);
            isConnected.set(true);
            clientService = ((ConnectClientService.ConnectBinder)binder).getService();
            listener.onConnected();
        }
        //binder comes from server to communicate with method's of

        public void onServiceDisconnected(ComponentName className) {
            Log.d("ServiceConnection","disconnected");
            isConnected.set(false);
            profServiceConnection = null;
            listener.onDisconnected();
        }
    };
    //
    private void startProfileServerService() {
        Intent intent = new Intent(application,ConnectClientService.class);
        application.bindService(intent,profServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public ConnectClientService getClient(){
        return clientService;
    }

    public void setListener(InitListener listener) {
        this.listener = listener;
    }
}
