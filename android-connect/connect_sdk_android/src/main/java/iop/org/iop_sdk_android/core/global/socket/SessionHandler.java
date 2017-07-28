package iop.org.iop_sdk_android.core.global.socket;

import iop.org.iop_sdk_android.core.global.ModuleObject;

/**
 * Created by furszy on 7/28/17.
 */

public interface SessionHandler {

    void onReceive(ModuleObject.ModuleResponse response);

    void sessionClosed(String clientPk);
}
