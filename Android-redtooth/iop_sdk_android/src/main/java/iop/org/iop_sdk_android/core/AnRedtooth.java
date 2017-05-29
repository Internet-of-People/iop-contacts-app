package iop.org.iop_sdk_android.core;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import org.fermat.redtooth.profile_server.ModuleRedtooth;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import iop.org.iop_sdk_android.core.profile_server.RedtoothService;

/**
 * Created by mati on 08/05/17.
 */

public class AnRedtooth {

    public static final String TAG = "AnRedtooth";

    private Application application;
    private RedtoothService redtoothService;

    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private InitListener listener;


    public static AnRedtooth init(Application application, InitListener initListener) {
        AnRedtooth anRedtooth = new AnRedtooth(application);
        anRedtooth.setListener(initListener);
        anRedtooth.startProfileServerService();
        return anRedtooth;
    }

    private AnRedtooth(Application application) {
        this.application = application;
    }

    public ServiceConnection profServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d(TAG,"profile service connected");
            isConnected.set(true);
            redtoothService = ((RedtoothService.ProfServerBinder)binder).getService();
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
        Intent intent = new Intent(application,RedtoothService.class);
        application.bindService(intent,profServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public ModuleRedtooth getRedtooth(){
        return redtoothService;
    }

    public void waitConnected() {
        while (!isConnected.get()){
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void setListener(InitListener listener) {
        this.listener = listener;
    }
}
