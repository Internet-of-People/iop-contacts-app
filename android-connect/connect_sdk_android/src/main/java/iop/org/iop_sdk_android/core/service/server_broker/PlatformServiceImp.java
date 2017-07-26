package iop.org.iop_sdk_android.core.service.server_broker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import org.fermat.redtooth.core.IoPConnect;
import org.fermat.redtooth.core.IoPConnectContext;
import org.fermat.redtooth.global.DeviceLocation;
import org.fermat.redtooth.global.GpsLocation;
import org.fermat.redtooth.global.Module;
import org.fermat.redtooth.global.PlatformSerializer;
import org.fermat.redtooth.profile_server.ProfileServerConfigurations;
import org.fermat.redtooth.profile_server.model.KeyEd25519;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.services.EnabledServices;
import org.fermat.redtooth.services.interfaces.ProfilesModule;
import org.fermat.redtooth.wallet.utils.BlockchainState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import iop.org.iop_sdk_android.core.crypto.CryptoWrapperAndroid;
import iop.org.iop_sdk_android.core.db.SqlitePairingRequestDb;
import iop.org.iop_sdk_android.core.db.SqliteProfilesDb;
import iop.org.iop_sdk_android.core.global.ModuleObjectWrapper;
import iop.org.iop_sdk_android.core.global.ModuleParameter;
import iop.org.iop_sdk_android.core.service.IoPConnectService;
import iop.org.iop_sdk_android.core.service.ProfileServerConfigurationsImp;
import iop.org.iop_sdk_android.core.service.SslContextFactory;
import iop.org.iop_sdk_android.core.service.modules.Core;
import iop.org.iop_sdk_android.core.service.modules.imp.profile.ProfilesModuleImp;

import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_ON_PROFILE_CONNECTED;
import static iop.org.iop_sdk_android.core.IntentBroadcastConstants.ACTION_ON_PROFILE_DISCONNECTED;

/**
 * Created by furszy on 7/19/17.
 *
 * Connect sdk broker pattern server side.
 *
 */

public class PlatformServiceImp extends Service implements PlatformService,DeviceLocation {

    private Logger logger = LoggerFactory.getLogger(PlatformServiceImp.class);

    private static final String ACTION_SCHEDULE_SERVICE = "schedule_service";
    public static final String ACTION_BOOT_SERVICE = "boot_service";

    private LocalBroadcastManager localBroadcastManager;

    private ExecutorService executor;
    /** Context */
    private IoPConnectContext application;
    /** Main library */
    private IoPConnect ioPConnect;
    /** Configurations impl */
    private ProfileServerConfigurations configurationsPreferences;
    private Profile profile;
    /** Databases */
    private SqlitePairingRequestDb pairingRequestDb;
    private SqliteProfilesDb profilesDb;

    private Core core;

    private AtomicBoolean isInitialized = new AtomicBoolean(false);

    private final Set<BlockchainState.Impediment> impediments = EnumSet.noneOf(BlockchainState.Impediment.class);

    private PlatformSerializer platformSerializer = new PlatformSerializer(){
        @Override
        public KeyEd25519 toPlatformKey(byte[] privKey, byte[] pubKey) {
            return iop.org.iop_sdk_android.core.crypto.KeyEd25519.wrap(privKey,pubKey);
        }
    };

    private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                final NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                final boolean hasConnectivity = networkInfo.isConnected();
                logger.info("network is {}, state {}/{}", hasConnectivity ? "up" : "down", networkInfo.getState(), networkInfo.getDetailedState());
                if (hasConnectivity)
                    impediments.remove(BlockchainState.Impediment.NETWORK);
                else {
                    impediments.add(BlockchainState.Impediment.NETWORK);
                }
                check();
            } else if (Intent.ACTION_DEVICE_STORAGE_LOW.equals(action)) {
                logger.info("device storage low");
                impediments.add(BlockchainState.Impediment.STORAGE);
                // todo: control this on the future
                //check();
            } else if (Intent.ACTION_DEVICE_STORAGE_OK.equals(action)) {
                logger.info("device storage ok");
                impediments.remove(BlockchainState.Impediment.STORAGE);
                // todo: control this on the future
                //check();
            }
        }
    };

    public ProfileServerConfigurations getConfPref() {
        return configurationsPreferences;
    }

    @Override
    public Profile getProfile() {
        return profile;
    }

    @Override
    public ProfilesModule getProfileModule() {
        return core.getModule(EnabledServices.PROFILE_DATA.getName(),ProfilesModule.class);
    }

    public SqliteProfilesDb getProfilesDb() {
        return profilesDb;
    }

    public SqlitePairingRequestDb getPairingRequestsDb() {
        return pairingRequestDb;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        logger.info("onCreate");
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_LOW);
        intentFilter.addAction(Intent.ACTION_DEVICE_STORAGE_OK);
        registerReceiver(connectivityReceiver, intentFilter); // implicitly init PeerGroup
        initService();
    }

    private void initService(){
        try {
            if (isInitialized.compareAndSet(false, true)) {
                localBroadcastManager = LocalBroadcastManager.getInstance(this);
                application = (IoPConnectContext) getApplication();
                executor = Executors.newFixedThreadPool(3);
                configurationsPreferences = new ProfileServerConfigurationsImp(this, getSharedPreferences(ProfileServerConfigurationsImp.PREFS_NAME, 0));
                KeyEd25519 keyEd25519 = (KeyEd25519) configurationsPreferences.getUserKeys();
                if (keyEd25519 != null)
                    profile = configurationsPreferences.getProfile();
                pairingRequestDb = new SqlitePairingRequestDb(this);
                profilesDb = new SqliteProfilesDb(this);
                ioPConnect = new IoPConnect(application,new CryptoWrapperAndroid(),new SslContextFactory(this),profilesDb,pairingRequestDb,this);
                // init core
                core = new Core(this,this,ioPConnect);
                ioPConnect.setEngineListener((ProfilesModuleImp)core.getModule(EnabledServices.PROFILE_DATA.getName()));

                tryScheduleService();

            }
        }catch (Exception e){
            e.printStackTrace();
            isInitialized.set(false);
        }
    }

    /**
     * Try to solve some impediment if there is any.
     */
    private void check(){
        try {
            if (configurationsPreferences.getBackgroundServiceEnable()) {
                if (impediments.contains(BlockchainState.Impediment.NETWORK)) {
                    // network unnavailable, check if i have to clean something here
                    Intent intent = new Intent(ACTION_ON_PROFILE_DISCONNECTED);
                    localBroadcastManager.sendBroadcast(intent);
                    return;
                }
                if (profile != null) {
                    if (!ioPConnect.isProfileConnectedOrConnecting(profile.getHexPublicKey())) {
                        connect(profile.getHexPublicKey());
                    } else {
                        Intent intent = new Intent(ACTION_ON_PROFILE_CONNECTED);
                        localBroadcastManager.sendBroadcast(intent);
                        logger.info("check, profile connected or connecting. no actions");
                    }
                } else {
                    logger.warn("### Trying to check with a null profile.");
                }
            }else {
                logger.warn("### background service disabled");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception on check",e);
        }
    }

    public void connect(final String pubKey) throws Exception {
        ProfilesModule module = core.getModule(EnabledServices.PROFILE_DATA.getName(), ProfilesModule.class);
        module.connect(pubKey);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logger.info("onStartCommand");
        if (intent!=null) {
            String action = intent.getAction();
            if (action.equals(ACTION_SCHEDULE_SERVICE)) {
                check();
            } else if (action.equals(ACTION_BOOT_SERVICE)) {
                // only check for now..
                check();
            }
        }else {
            //
            check();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        logger.info("onDestroy");
        core.clean();
        executor.shutdown();
        ioPConnect.stop();
        super.onDestroy();
    }

    /**
     * Schedule service for later
     */
    private void tryScheduleService() {
        boolean isSchedule = System.currentTimeMillis()<configurationsPreferences.getScheduleServiceTime();

        if (!isSchedule){
            logger.info("scheduling service");
            AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
            long scheduleTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5); // 10 minutes from now

            Intent intent = new Intent(this, IoPConnectService.class);
            intent.setAction(ACTION_SCHEDULE_SERVICE);
            alarm.set(
                    // This alarm will wake up the device when System.currentTimeMillis()
                    // equals the second argument value
                    alarm.RTC_WAKEUP,
                    scheduleTime,
                    // PendingIntent.getService creates an Intent that will start a service
                    // when it is called. The first argument is the Context that will be used
                    // when delivering this intent. Using this has worked for me. The second
                    // argument is a request code. You can use this code to cancel the
                    // pending intent if you need to. Third is the intent you want to
                    // trigger. In this case I want to create an intent that will start my
                    // service. Lastly you can optionally pass flags.
                    PendingIntent.getService(this, 0,intent , 0)
            );
            // save
            configurationsPreferences.saveScheduleServiceTime(scheduleTime);
        }
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    private final IPlatformService.Stub mBinder = new IPlatformService.Stub() {
        @Override
        public ModuleObjectWrapper callMethod(String clientKey, String dataId, String serviceName, String method, ModuleParameter[] parameters) throws RemoteException {
            logger.info("call service: "+serviceName+", method "+method);
            // todo: security checks here..
            Serializable object = null;
            try {
                object = moduleDataRequest(serviceName,method,parameters);
                ModuleObjectWrapper moduleObjectWrapper = new ModuleObjectWrapper(dataId,object);
                return moduleObjectWrapper;
            } catch (Exception e) {
                e.printStackTrace();
                return new ModuleObjectWrapper(dataId,e);
            }
        }
    };

    private Serializable moduleDataRequest(final String serviceName, final String method, final ModuleParameter[] parameters) throws Exception {
        EnabledServices service = EnabledServices.getServiceByName(serviceName);
        Class clazz = service.getModuleClass();
        Module module = core.getModule(serviceName);
        Method m = null;
        Object returnedObject = null;
        Object[] params = null;
        Class<?>[] paramsTypes = null;
        if (parameters != null) {
            params = new Object[parameters.length];
            paramsTypes = new Class[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                params[i] = parameters[i].getObject();
                paramsTypes[i] = parameters[i].getParameterType();
            }
        }
        try{
            if (paramsTypes == null) {
                m = clazz.getDeclaredMethod(method, null);
                returnedObject = m.invoke(module, null);
            } else {
                try {
                    m = clazz.getDeclaredMethod(method, paramsTypes);
                } catch (NoSuchMethodException e) {
                    //Log.e(TAG,"Metodo buscando: "+method);
                    for (Method methodInterface : clazz.getDeclaredMethods()) {
                        if (methodInterface.getName().equals(method)) {
                            m = methodInterface;
                        }

                    }
                }
                if (module != null) {
                    if (m != null) {
                        returnedObject = m.invoke(module, params);
                    } else {
                        for (Method method1 : module.getClass().getSuperclass().getDeclaredMethods()) {
                            if (method1.getName().equals(method)) {
                                try {
                                    returnedObject = method1.invoke(module, params);
                                } catch (Exception e) {
                                    returnedObject = e;
                                }
                                break;
                            }
                        }

                    }
                } else {
                    logger.info("NOT FOUND module");
                }
            }
            if (m!=null){
                if (!m.getReturnType().equals(Void.TYPE) && returnedObject == null){
                    // nothing..
                }
            }
        }catch (NoSuchMethodException e) {
            logger.info("NoSuchMethodException:" + method + " on class" + clazz.getName());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            return e.getTargetException();
//                    return e;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            return e;
        }
        if (!(returnedObject instanceof Serializable)){
            logger.warn("Error, Method: "+method+" doesn't return a Serializable object");
            throw new Exception("Method doesn't return a Serializable object");
        }
        return (Serializable) returnedObject;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        logger.info("onBind:" + intent.getAction());
        IBinder iBinder = null;
        try {
            switch (intent.getAction()) {
                case IntentServiceAction.ACTION_BIND_AIDL:
                    iBinder = mBinder;
                    break;
                default:
                    logger.info("onBind defautl");
                    break;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return iBinder;
    }

    @Override
    public boolean isDeviceLocationEnabled() {
        return false;
    }

    @Override
    public GpsLocation getDeviceLocation() {
        return null;
    }
}
