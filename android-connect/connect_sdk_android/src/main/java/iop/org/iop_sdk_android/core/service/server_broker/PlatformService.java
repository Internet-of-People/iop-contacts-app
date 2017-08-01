package iop.org.iop_sdk_android.core.service.server_broker;

import org.libertaria.world.profile_server.ProfileServerConfigurations;
import org.libertaria.world.profiles_manager.ProfilesManager;
import org.libertaria.world.services.interfaces.ProfilesModule;

import iop.org.iop_sdk_android.core.service.db.SqlitePairingRequestDb;

/**
 * Created by furszy on 7/24/17.
 *  Class just for now..
 */

public interface PlatformService {
    //void setProfile(Profile profile);
    ProfilesManager getProfilesDb();
    SqlitePairingRequestDb getPairingRequestsDb();
    ProfileServerConfigurations getConfPref();
    //Profile getProfile();

    ProfilesModule getProfileModule();

}
