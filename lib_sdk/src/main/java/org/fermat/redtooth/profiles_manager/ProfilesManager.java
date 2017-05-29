package org.fermat.redtooth.profiles_manager;

import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.model.Profile;

import java.util.List;

/**
 * Created by mati on 16/05/17.
 *
 * Class in charge of save profiles data as a cache.
 *
 */

public interface ProfilesManager {


    long saveProfile(ProfileInformation profile);

    boolean updateProfile(ProfileInformation profile);

    ProfileInformation getProfile(long id);

    ProfileInformation getProfile(byte[] pubKey);

    List<ProfileInformation> listOwnProfiles(byte[] pubKey);

    List<ProfileInformation> listConnectedProfiles(byte[] pubKey);

    List<ProfileInformation> listAll();
}
