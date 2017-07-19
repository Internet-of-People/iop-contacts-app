package iop.org.iop_sdk_android.core.service.modules;

import android.content.Context;

import org.fermat.redtooth.core.IoPConnect;

import java.util.HashMap;

import iop.org.iop_sdk_android.core.service.IoPConnectService;
import iop.org.iop_sdk_android.core.service.modules.imp.ProfilesModuleImp;

/**
 * Created by furszy on 7/19/17.
 *
 * AbstractModule's mananger
 *
 */

public class Core {

    private HashMap<ModuleId,? extends AbstractModule> modules = new HashMap<>();

    private Context context;
    private IoPConnect ioPConnect;
    private IoPConnectService ioPConnectService;

    public Core(IoPConnectService ioPConnectService,IoPConnect ioPConnect) {
        this.ioPConnectService = ioPConnectService;
        this.context = ioPConnectService;
        this.ioPConnect = ioPConnect;
    }

    public <T extends Module> T getModule(String id, Class<T> tClass){
        if (modules.containsKey(id)){
            return (T) modules.get(id);
        }
        Module module = null;
        switch (ModuleId.getModuleIdById(id)){
            case PROFILES:
                module = new ProfilesModuleImp(
                        context,
                        ioPConnect,
                        ioPConnectService
                );
                break;
        }
        return (T) module;
    }

    public void clean() {
        for (AbstractModule abstractModule : modules.values()) {
            abstractModule.onDestroy();
        }
        ioPConnect = null;
        ioPConnectService = null;
    }
}
