package iop.org.iop_sdk_android.core.service.server_broker;

import org.libertaria.world.profile_server.model.ProfServerData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 20/12/2017.
 */

public class InitialProfileServers {

    private static final List<ProfServerData> INITIAL_SERVERS = new ArrayList<>();

    static {
        ProfServerData victorRaspberry = new ProfServerData("IP");
    }

}
