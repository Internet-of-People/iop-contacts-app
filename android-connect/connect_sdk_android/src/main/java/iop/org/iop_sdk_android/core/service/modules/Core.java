package iop.org.iop_sdk_android.core.service.modules;

import android.content.Context;

import org.fermat.redtooth.core.IoPConnect;
import org.fermat.redtooth.global.Module;
import org.fermat.redtooth.services.EnabledServices;
import org.fermat.redtooth.services.ServiceFactory;

import java.util.HashMap;

import iop.org.iop_sdk_android.core.service.IoPConnectService;
import iop.org.iop_sdk_android.core.service.modules.imp.chat.ChatModuleImp;
import iop.org.iop_sdk_android.core.service.modules.imp.pairing.PairingModuleImp;
import iop.org.iop_sdk_android.core.service.modules.imp.profile.ProfilesModuleImp;
import iop.org.iop_sdk_android.core.service.server_broker.PlatformService;

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
    private PlatformService ioPConnectService;
    private ServiceFactory serviceFactory;

    public Core(Context context, PlatformService ioPConnectService, IoPConnect ioPConnect, ServiceFactory serviceFactory) {
        this.ioPConnectService = ioPConnectService;
        this.context = context;
        this.ioPConnect = ioPConnect;
        this.serviceFactory = serviceFactory;
    }

    public Module getModule(String id){
        EnabledServices moduleId = EnabledServices.getServiceByName(id);
        if (modules.containsKey(moduleId)){
            return modules.get(moduleId);
        }
        Module module = null;
        switch (moduleId){
            case PROFILE_DATA:
                module = new ProfilesModuleImp(
                        context,
                        ioPConnect,
                        serviceFactory,
                        ioPConnectService.getConfPref()
                );
                break;
            case PROFILE_PAIRING:
                module = new PairingModuleImp(context,ioPConnectService,ioPConnect);
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
