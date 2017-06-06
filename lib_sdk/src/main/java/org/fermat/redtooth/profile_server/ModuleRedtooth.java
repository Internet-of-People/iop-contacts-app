package org.fermat.redtooth.profile_server;

import java.io.File;
import java.util.List;

import org.fermat.redtooth.profile_server.engine.SearchProfilesQuery;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.futures.SearchMessageFuture;
import org.fermat.redtooth.profile_server.engine.futures.SubsequentSearchMsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.listeners.PairingListener;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;
import org.fermat.redtooth.profiles_manager.PairingRequest;

/**
 * Created by mati on 22/11/16.
 */

public interface ModuleRedtooth {

    boolean isProfileRegistered();

    void connect(String pubKey) throws Exception;

    String registerProfile(String name,String type, byte[] img, int latitude, int longitude, String extraData) throws Exception;

    int updateProfile(String name, ProfSerMsgListener msgListener) throws Exception;

    int updateProfile(String name,byte[] img,ProfSerMsgListener msgListener) throws Exception;

    int updateProfile(String pubKey,String name, byte[] img, String extraData, ProfSerMsgListener msgListener) throws Exception;

    int updateProfile(byte[] version, String pubKey ,String name, byte[] img, int latitude, int longitude, String extraData, ProfSerMsgListener msgListener) throws Exception;

    int updateProfileExtraData(String pubKey,Signer signer, String extraData) throws Exception;

    /**
     * Request pair profile, This will notify to the other user that you want to connect with him.
     *
     * @param pubKey
     * @param profileServerId
     * @param listener
     */
    void requestPairingProfile(byte[] pubKey, byte[] profileServerId, ProfSerMsgListener<Integer> listener);

    /**
     * Accept a pairing request.
     *
     * @param profileServerId
     * @param publicKey
     */
    void acceptPairingProfile(byte[] profileServerId, byte[] publicKey);

    boolean isIdentityCreated();

    void setPairListener(PairingListener pairListener);

    /* Search queries **/

    void getProfileInformation(String profPubKey, ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException;
    void getProfileInformation(String profPubKey,boolean withImage ,ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException;

    void searchProfileByName(String name, ProfSerMsgListener<List<IopProfileServer.ProfileQueryInformation>> listener);

    /**  */
    SearchMessageFuture<List<IopProfileServer.ProfileQueryInformation>> searchProfiles(SearchProfilesQuery searchProfilesQuery);

    SubsequentSearchMsgListenerFuture<List<IopProfileServer.ProfileQueryInformation>> searchSubsequentsProfiles(SearchProfilesQuery searchProfilesQuery);

    File getUserImageFile();

    Profile getProfile();

    List<ProfileInformation> getKnownProfiles();

    ProfileInformation getKnownProfile(byte[] pubKey);

    PairingRequest getProfilePairingRequest(String hexPublicKey);
}
