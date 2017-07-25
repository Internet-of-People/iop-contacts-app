package iop.org.iop_sdk_android.core.service.client_broker;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.fermat.redtooth.global.Module;
import org.fermat.redtooth.profile_server.ModuleRedtooth;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.services.EnabledServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import iop.org.iop_sdk_android.core.global.ModuleObjectWrapper;
import iop.org.iop_sdk_android.core.global.ModuleParameter;
import iop.org.iop_sdk_android.core.service.server_broker.IPlatformService;
import iop.org.iop_sdk_android.core.service.server_broker.IntentServiceAction;
import iop.org.iop_sdk_android.core.service.server_broker.PlatformServiceImp;

import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_IOP_SERVICE_CONNECTED;

/**
 * Created by furszy on 7/19/17.
 *
 * Connect sdk broker pattern client side.
 *
 */

public class ConnectClientService extends Service implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConnectClientService.class);

    private IPlatformService iServerBrokerService = null;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    boolean mPlatformServiceIsBound;
    /** Module interfaces */
    private Map<EnabledServices,Module> openModules = new HashMap<>();

    // Android
    private LocalBroadcastManager broadcastManager;


    public class ConnectBinder extends Binder {
        public ConnectClientService getService() {
            return ConnectClientService.this;
        }
    }

    private final IBinder mBinder = new ConnectClientService.ConnectBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        logger.info(".onBind()");
        return mBinder;
    }


    public Module getModule(EnabledServices enabledServices){
        if (openModules.containsKey(enabledServices)){
            return openModules.get(enabledServices);
        }
        Module module = (Module) Proxy.newProxyInstance(
                  enabledServices.getModuleClass().getClassLoader(),
                  new Class[]{enabledServices.getModuleClass()},
                  this
        );
        openModules.put(enabledServices,module);
        return module;
    }

    @Override
    public final Object invoke(Object o, Method method, Object[] args) throws Throwable {
        if (!mPlatformServiceIsBound){
            throw new IllegalStateException("Platform service is not bound yet.");
        }
        logger.info("invoque method "+method.getName());
        ModuleParameter[] parameters = null;
        Class<?>[] parametersTypes = method.getParameterTypes();
        if (args != null) {
            parameters = new ModuleParameter[args.length];
            for (int i = 0; i < args.length; i++) {
                try {
                    ModuleParameter fermatModuleObjectWrapper = new ModuleParameter((Serializable) args[i], parametersTypes[i]);
                    parameters[i] = fermatModuleObjectWrapper;
                } catch (ClassCastException e) {
                    //e.printStackTrace();
                    logger.error("ERROR: Objet "+args[i].getClass().getName()+(" no implementing Serializable interface"),e);
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            parameters = new ModuleParameter[0];
        }
        ModuleObjectWrapper respObject = iServerBrokerService.callMethod(
                null,
                null, // data id should be packageName+msg_id
                ((Module)o).getId(),
                method.getName(),
                parameters
        );
        if (respObject.getE()!=null){
            throw respObject.getE();
        }
        return respObject.getObject();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        broadcastManager = LocalBroadcastManager.getInstance(this);
        try {
            if (!mPlatformServiceIsBound) {
                Intent serviceIntent = new Intent(this, PlatformServiceImp.class);
                serviceIntent.setAction(IntentServiceAction.ACTION_BIND_AIDL);
                doBindService(serviceIntent);
            }else{
                logger.info("Platform bounded");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mPlatformServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            iServerBrokerService = IPlatformService.Stub.asInterface(service);
            logger.info("Attached.");
            mPlatformServiceIsBound = true;
            logger.info("Registering client");
            //try {
                //serverIdentificationKey = iServerBrokerService.register();
                //running socket receiver
                /*logger.info("Starting socket receiver");
                LocalSocket localSocket = new LocalSocket();
                mReceiverSocketSession = new LocalClientSocketSession(serverIdentificationKey, localSocket, bufferChannelAIDL);
                mReceiverSocketSession.connect();
                mReceiverSocketSession.startReceiving();*/
            /*} catch (RemoteException e) {
                e.printStackTrace();
                logger.error("Cant run socket, register to server fail",e);
            } catch (Exception e) {
                e.printStackTrace();
            }*/
        }


        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            iServerBrokerService = null;
            mPlatformServiceIsBound = false;
            logger.info("ISERVERBROKERSERVICE disconnected");


        }
    };

    void doBindService(Intent intent) {
        try {
            //Log.d(TAG, "Before init intent.componentName");
            //Log.d(TAG, "Before bindService");
            if (bindService(intent, mPlatformServiceConnection, BIND_AUTO_CREATE)) {
                logger.info("Binding to ISERVERBROKERSERVICE returned true");
            } else {
                logger.info("Binding to ISERVERBROKERSERVICE returned false");
            }
        } catch (SecurityException e) {
            logger.info("can't bind to ISERVERBROKERSERVICE, check permission in Manifest");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //mPlatformServiceIsBound = true;
        logger.info("Binding.");
    }

    void doUnbindService() {
        if (mPlatformServiceIsBound) {
            // Detach our existing connection.
            unbindService(mPlatformServiceConnection);
            mPlatformServiceIsBound = false;
            logger.info("Unbinding.");
        }
    }
}
