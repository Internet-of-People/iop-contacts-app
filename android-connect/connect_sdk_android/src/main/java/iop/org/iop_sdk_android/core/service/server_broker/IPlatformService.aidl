// IPlatformService.aidl

package iop.org.iop_sdk_android.core.service.server_broker;

import iop.org.iop_sdk_android.core.global.ModuleObjectWrapper;
import iop.org.iop_sdk_android.core.global.ModuleParameter;

interface IPlatformService {

    ModuleObjectWrapper callMethod(
                in String clientKey,
                in String dataId,
                in String serviceName,
                in String method,
                in ModuleParameter[] parameters
                );

}

