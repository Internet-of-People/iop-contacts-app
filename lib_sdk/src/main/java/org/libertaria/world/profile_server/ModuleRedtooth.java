package org.libertaria.world.profile_server;

import org.libertaria.world.profile_server.engine.futures.SearchMessageFuture;
import org.libertaria.world.profile_server.protocol.IopProfileServer;
import org.libertaria.world.profiles_manager.PairingRequest;
import org.libertaria.world.services.chat.ChatCallAlreadyOpenException;
import org.libertaria.world.services.chat.RequestChatException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    int updateProfile(String name, byte[] img, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<Boolean> msgListener) throws Exception;
    int updateProfile(String pubKey ,String name, byte[] img, int latitude, int longitude, String extraData, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<Boolean> msgListener) throws Exception;

    /**
     * Request pair profile, This will notify to the other user that you want to connect with him.
     *
     * @param remotePubKey
     * @param listener
     */

    void requestPairingProfile(byte[] remotePubKey, String remoteName, String psHost, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<ProfileInformation> listener) throws Exception;

    /**
     * Accept a pairing request.
     *
     * @param pairingRequest
     * @param profSerMsgListener
     */
    void acceptPairingProfile(PairingRequest pairingRequest, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<Boolean> profSerMsgListener) throws Exception;

    void cancelPairingRequest(PairingRequest pairingRequest);

    void requestChat(ProfileInformation remoteProfileInformation, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<Boolean> readyListener, TimeUnit timeUnit, long time) throws RequestChatException, ChatCallAlreadyOpenException;

    void refuseChatRequest(String remoteHexPublicKey) throws Exception;

    void acceptChatRequest(String remoteHexPublicKey, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<Boolean> future) throws Exception;

    void sendMsgToChat(ProfileInformation remoteProfileInformation, String msg, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<Boolean> msgListener) throws Exception;

    boolean isChatActive(String remotePk);

    boolean isIdentityCreated();

    /* Search queries **/

    void getProfileInformation(String profPubKey, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException;

    /**
     *
     * @param profPubKey
     * @param getInfo  -> If you want to update your searched profile from the home node.
     * @param profileFuture
     * @throws CantConnectException
     * @throws CantSendMessageException
     */
    void getProfileInformation(String profPubKey, boolean getInfo, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException;

    void searchProfileByName(String name, org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener<List<IopProfileServer.ProfileQueryInformation>> listener);

    /**  */
    SearchMessageFuture<List<IopProfileServer.ProfileQueryInformation>> searchProfiles(org.libertaria.world.profile_server.engine.SearchProfilesQuery searchProfilesQuery);

    org.libertaria.world.profile_server.engine.futures.SubsequentSearchMsgListenerFuture<List<IopProfileServer.ProfileQueryInformation>> searchSubsequentsProfiles(org.libertaria.world.profile_server.engine.SearchProfilesQuery searchProfilesQuery);

    File getUserImageFile();

    org.libertaria.world.profile_server.model.Profile getProfile();

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
