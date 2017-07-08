package org.fermat.redtooth.profiles_manager;

import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.imp.ProfileInformationImp;
import org.fermat.redtooth.profile_server.model.Profile;

import java.util.List;
import java.util.Set;

/**
 * Created by mati on 16/05/17.
 *
 * Class in charge of save profiles data as a cache.
 *
 */

public interface ProfilesManager {


    long saveProfile(String localProfilePubKeyOwnerOfContact, ProfileInformation profile);

    void saveOrUpdateProfile(String localProfilePubKeyOwnerOfContact, ProfileInformation profile);

    boolean updateProfile(String localProfilePubKeyOwnerOfContact, ProfileInformation profile);

    ProfileInformation getProfile(long id);

    List<ProfileInformation> listOwnProfiles(String localProfileOwnerOfContacts);

    ProfileInformation getProfile(String localProfileOwnerOfContacts, String pubKey);

    List<ProfileInformation> listConnectedProfiles(String localProfileOwnerOfContacts);

    List<ProfileInformation> listAll(String localProfilePubKeyOwnerOfContact);

    boolean updatePaired(String localProfilePubKey, String remotePubKey, ProfileInformationImp.PairStatus value);

    boolean updateRemoteServices(String localProfilePubKey, String remotePubKey,Set<String> services);

    void saveAllProfiles(String localProfilePubKey, List<ProfileInformation> profileInformationList);
}
