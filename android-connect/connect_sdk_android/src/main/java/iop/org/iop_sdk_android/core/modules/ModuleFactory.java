package iop.org.iop_sdk_android.core.modules;

import org.libertaria.world.core.IoPConnect;
import org.libertaria.world.global.Module;
import org.libertaria.world.global.SystemContext;
import org.libertaria.world.services.EnabledServices;

import iop.org.iop_sdk_android.core.modules.chat.ChatModuleImp;

/**
 * Created by furszy on 8/1/17.
 */

public class ModuleFactory {

    public static Module createModule(EnabledServices moduleId, SystemContext context, IoPConnect ioPConnect) {
        Module module = null;
        switch (moduleId){
            case CHAT:
                module = new ChatModuleImp(context,ioPConnect);
                break;
            default:
                throw new IllegalArgumentException("EnabledService not found.");
        }
        return module;
    }

}
