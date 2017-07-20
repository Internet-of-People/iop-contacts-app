package org.fermat.redtooth.profile_server;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.fermat.redtooth.core.services.AppServiceListener;
import org.fermat.redtooth.profile_server.engine.SearchProfilesQuery;
import org.fermat.redtooth.profile_server.engine.futures.SearchMessageFuture;
import org.fermat.redtooth.profile_server.engine.futures.SubsequentSearchMsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.app_services.PairingListener;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;
import org.fermat.redtooth.profile_server.engine.listeners.ProfileListener;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;
import org.fermat.redtooth.profiles_manager.PairingRequest;
import org.fermat.redtooth.services.chat.ChatCallAlreadyOpenException;
import org.fermat.redtooth.services.chat.ChatMsg;
import org.fermat.redtooth.services.chat.RequestChatException;

/**
 * Created by mati on 22/11/16.
 */

public interface ModuleRedtooth {

    File backupProfile(File backupDir, String password) throws IOException;

    void scheduleBackupProfileFile(File backupDir,String password);

    void restoreFrom(File file, String password);

    boolean isProfileRegistered();

    void addService(String serviceName, Object... args);

    // moved methods..
    void connect(String pubKey) throws Exception;
    String registerProfile(String name,String type, byte[] img, int latitude, int longitude, String extraData) throws Exception;
    String registerProfile(String name,byte[] img) throws Exception;

    int updateProfile(String name,byte[] img,ProfSerMsgListener<Boolean> msgListener) throws Exception;
    int updateProfile(String pubKey ,String name, byte[] img, int latitude, int longitude, String extraData, ProfSerMsgListener<Boolean> msgListener) throws Exception;

    /**
     * Request pair profile, This will notify to the other user that you want to connect with him.
     *
     * @param remotePubKey
     * @param listener
     */

    void requestPairingProfile(byte[] remotePubKey, String remoteName, String psHost, ProfSerMsgListener<ProfileInformation> listener) throws Exception;

    /**
     * Accept a pairing request.
     *
     * @param pairingRequest
     * @param profSerMsgListener
     */
    void acceptPairingProfile(PairingRequest pairingRequest, ProfSerMsgListener<Boolean> profSerMsgListener) throws Exception;

    void cancelPairingRequest(PairingRequest pairingRequest);

    void requestChat(ProfileInformation remoteProfileInformation, ProfSerMsgListener<Boolean> readyListener, TimeUnit timeUnit, long time) throws RequestChatException, ChatCallAlreadyOpenException;

    void refuseChatRequest(String remoteHexPublicKey);

    void acceptChatRequest(String remoteHexPublicKey, ProfSerMsgListener<Boolean> future) throws Exception;

    void sendMsgToChat(ProfileInformation remoteProfileInformation, String msg, ProfSerMsgListener<Boolean> msgListener) throws Exception;

    boolean isIdentityCreated();

    /* Search queries **/

    void getProfileInformation(String profPubKey, ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException;

    /**
     *
     * @param profPubKey
     * @param getInfo  -> If you want to update your searched profile from the home node.
     * @param profileFuture
     * @throws CantConnectException
     * @throws CantSendMessageException
     */
    void getProfileInformation(String profPubKey, boolean getInfo, ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException;

    void searchProfileByName(String name, ProfSerMsgListener<List<IopProfileServer.ProfileQueryInformation>> listener);

    /**  */
    SearchMessageFuture<List<IopProfileServer.ProfileQueryInformation>> searchProfiles(SearchProfilesQuery searchProfilesQuery);

    SubsequentSearchMsgListenerFuture<List<IopProfileServer.ProfileQueryInformation>> searchSubsequentsProfiles(SearchProfilesQuery searchProfilesQuery);

    File getUserImageFile();

    Profile getProfile();

    ProfileInformation getMyProfile();

    List<ProfileInformation> getKnownProfiles();

    ProfileInformation getKnownProfile(String pubKey);

    PairingRequest getProfilePairingRequest(String hexPublicKey);

    List<PairingRequest> getPairingRequests();

    List<PairingRequest> getPairingOpenRequests();

    String getPsHost();

    void deteleContacts();

    void deletePairingRequests();

    Collection<PairingRequest> listAllPairingRequests();

    Collection<ProfileInformation> listAllProfileInformation();

    boolean isProfileConnectedOrConnecting();
}
