// IPlatformService.aidl

package world.libertaria.shared.library.global.service;

import world.libertaria.shared.library.global.ModuleObjectWrapper;
import world.libertaria.shared.library.global.ModuleParameter;

interface IPlatformService {

    String register(in String applicationName);

    ModuleObjectWrapper callMethod(
                in String clientKey,
                in String dataId,
                in String serviceName,
                in String method,
                in ModuleParameter[] parameters
                );

}

