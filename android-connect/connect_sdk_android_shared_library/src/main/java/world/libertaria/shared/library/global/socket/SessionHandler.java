package world.libertaria.shared.library.global.socket;

import world.libertaria.shared.library.global.ModuleObject;

/**
 * Created by furszy on 7/28/17.
 */

public interface SessionHandler {

    void onReceive(LocalSocketSession localSocketSession,ModuleObject.ModuleResponse response);

    void sessionClosed(LocalSocketSession localSocketSession,String clientPk);
}
