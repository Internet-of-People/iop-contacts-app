package org.libertaria.world.profiles_manager;

import org.libertaria.world.profile_server.ProfileInformation;
import org.libertaria.world.profile_server.imp.ProfileInformationImp;

import java.util.List;
import java.util.Set;

/**
 * Created by mati on 16/05/17.
 *
 * Class in charge of save profiles data as a cache.
 *
 */

public interface ProfilesManager {


    long saveProfile(String localProfilePubKeyOwnerOfContact, org.libertaria.world.profile_server.ProfileInformation profile);

    long saveProfileIfNotExist(String localProfilePubKeyOwnerOfContact, org.libertaria.world.profile_server.ProfileInformation profile);

    void saveOrUpdateProfile(String localProfilePubKeyOwnerOfContact, org.libertaria.world.profile_server.ProfileInformation profile);

    boolean updateProfile(String localProfilePubKeyOwnerOfContact, org.libertaria.world.profile_server.ProfileInformation profile);

    org.libertaria.world.profile_server.ProfileInformation getProfile(long id);

    boolean existProfile(String localProfileOwnerOfContacts, String pubKey);

    List<org.libertaria.world.profile_server.ProfileInformation> listOwnProfiles(String localProfileOwnerOfContacts);

    org.libertaria.world.profile_server.ProfileInformation getProfile(String localProfileOwnerOfContacts, String pubKey);

    List<org.libertaria.world.profile_server.ProfileInformation> listConnectedProfiles(String localProfileOwnerOfContacts);

    List<org.libertaria.world.profile_server.ProfileInformation> listAll(String localProfilePubKeyOwnerOfContact);

    boolean updatePaired(String localProfilePubKey, String remotePubKey, org.libertaria.world.profile_server.imp.ProfileInformationImp.PairStatus value);

    boolean updateRemoteServices(String localProfilePubKey, String remotePubKey,Set<String> services);

    void saveAllProfiles(String localProfilePubKey, List<org.libertaria.world.profile_server.ProfileInformation> profileInformationList);

    int deleteProfileByPubKey(String localProfilePubKey, String remoteHexPubKey);

    void truncate();
}
