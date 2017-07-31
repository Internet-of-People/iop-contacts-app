package org.fermat.redtooth.profiles_manager;

import org.fermat.redtooth.profile_server.model.Profile;

import java.util.List;

/**
 * Created by furszy on 7/27/17.
 */

public interface LocalProfilesDao {

    long save(Profile profile);

    void updateProfile(Profile profile);

    Profile getProfile(String profilePublicKey);

    List<Profile> list();
}
