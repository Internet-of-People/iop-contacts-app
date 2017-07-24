package iop.org.iop_sdk_android.core.service.client_broker;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.fermat.redtooth.global.Module;
import org.fermat.redtooth.services.EnabledServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by furszy on 7/19/17.
 *
 * Connect sdk broker pattern client side.
 *
 */

public class ConnectClientService extends Service implements InvocationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConnectClientService.class);

    private Map<EnabledServices,Module> openModules = new HashMap<>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        logger.info("invoque method "+method.getName());

        return null;
    }

    private void sendMessage(){

    }



}
