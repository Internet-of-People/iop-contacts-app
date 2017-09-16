package iop.org.iop_sdk_android.core.service.server_broker;

import org.libertaria.world.core.IoPConnect;
import org.libertaria.world.global.Module;
import org.libertaria.world.global.SystemContext;
import org.libertaria.world.services.EnabledServices;
import org.libertaria.world.services.ServiceFactory;

import java.util.HashMap;

import iop.org.iop_sdk_android.core.modules.ModuleFactory;
import iop.org.iop_sdk_android.core.modules.pairing.PairingModuleImp;
import iop.org.iop_sdk_android.core.modules.profile.ProfilesModuleImp;

/**
 * Created by furszy on 7/19/17.
 *
 * AbstractModule's mananger
 *
 */

public class Core {

    private HashMap<EnabledServices,Module> modules = new HashMap<>();

    private SystemContext context;
    private IoPConnect ioPConnect;
    private PlatformService ioPConnectService;
    private ServiceFactory serviceFactory;

    public Core(SystemContext context, PlatformService ioPConnectService, IoPConnect ioPConnect, ServiceFactory serviceFactory) {
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
            // internal modules
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
            default:
                // External modules
                module = ModuleFactory.createModule(moduleId,context,ioPConnect);
                break;
                //throw new IllegalArgumentException("EnabledService not found.");
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
