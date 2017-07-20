package iop.org.iop_sdk_android.core.service.modules;

import android.content.Context;

import org.fermat.redtooth.core.IoPConnect;
import org.spongycastle.math.raw.Mod;

import java.util.HashMap;

import iop.org.iop_sdk_android.core.service.IoPConnectService;
import iop.org.iop_sdk_android.core.service.modules.imp.ChatModuleImp;
import iop.org.iop_sdk_android.core.service.modules.imp.PairingModuleImp;
import iop.org.iop_sdk_android.core.service.modules.imp.ProfilesModuleImp;

/**
 * Created by furszy on 7/19/17.
 *
 * AbstractModule's mananger
 *
 */

public class Core {

    private HashMap<ModuleId,Module> modules = new HashMap<>();

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
        ModuleId moduleId = ModuleId.getModuleIdById(id);
        switch (moduleId){
            case PROFILES:
                module = new ProfilesModuleImp(
                        context,
                        ioPConnect,
                        ioPConnectService
                );
                break;
            case PAIRING:
                module = new PairingModuleImp(ioPConnectService,ioPConnect);
                break;
            case CHAT:
                module = new ChatModuleImp(ioPConnect);
                break;
            default:
                throw new IllegalArgumentException("ModuleId not found.");
        }
        modules.put(moduleId,module);
        return (T) module;
    }

    public void clean() {
        for (Module module : modules.values()) {
            module.onDestroy();
        }
        ioPConnect = null;
        ioPConnectService = null;
    }
}
