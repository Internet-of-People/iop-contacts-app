package iop.org.iop_sdk_android.core.service.modules;

import android.content.Context;

import org.fermat.redtooth.core.IoPConnect;
import org.fermat.redtooth.global.Module;
import org.fermat.redtooth.services.EnabledServices;

import java.util.HashMap;

import iop.org.iop_sdk_android.core.service.IoPConnectService;
import iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatModuleImp;
import iop.org.iop_sdk_android.core.service.modules.imp.PairingModuleImp;
import iop.org.iop_sdk_android.core.service.modules.imp.ProfilesModuleImp;

/**
 * Created by furszy on 7/19/17.
 *
 * AbstractModule's mananger
 *
 */

public class Core {

    private HashMap<EnabledServices,Module> modules = new HashMap<>();

    private Context context;
    private IoPConnect ioPConnect;
    private IoPConnectService ioPConnectService;

    public Core(IoPConnectService ioPConnectService,IoPConnect ioPConnect) {
        this.ioPConnectService = ioPConnectService;
        this.context = ioPConnectService;
        this.ioPConnect = ioPConnect;
    }

    public Module getModule(String id){
        if (modules.containsKey(id)){
            return modules.get(id);
        }
        Module module = null;
        EnabledServices moduleId = EnabledServices.getServiceByName(id);
        switch (moduleId){
            case PROFILE_DATA:
                module = new ProfilesModuleImp(
                        context,
                        ioPConnect,
                        ioPConnectService
                );
                break;
            case PROFILE_PAIRING:
                module = new PairingModuleImp(ioPConnectService,ioPConnect);
                break;
            case CHAT:
                module = new ChatModuleImp(context,ioPConnect);
                break;
            default:
                throw new IllegalArgumentException("EnabledService not found.");
        }
        modules.put(moduleId,module);
        return module;
    }

    public <T extends Module> T getModule(String id, Class<T> tClass){
        return (T) getModule(id);
    }

    public void clean() {
        for (Module module : modules.values()) {
            module.onDestroy();
        }
        ioPConnect = null;
        ioPConnectService = null;
    }
}
