package iop.org.iop_sdk_android.core.service.server_broker;

import org.fermat.redtooth.profile_server.ProfileServerConfigurations;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profiles_manager.ProfilesManager;
import org.fermat.redtooth.services.interfaces.ProfilesModule;

import iop.org.iop_sdk_android.core.db.SqlitePairingRequestDb;
import iop.org.iop_sdk_android.core.service.IoPConnectService;

/**
 * Created by furszy on 7/24/17.
 *  Class just for now..
 */

public interface PlatformService {
    void setProfile(Profile profile);
    ProfilesManager getProfilesDb();
    SqlitePairingRequestDb getPairingRequestsDb();
    ProfileServerConfigurations getConfPref();
    Profile getProfile();

    ProfilesModule getProfileModule();

}
