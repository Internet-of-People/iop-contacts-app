package org.fermat.redtooth.profile_server.client;

import java.io.IOException;

import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.IoHandler;
import org.fermat.redtooth.profile_server.Signer;
import org.fermat.redtooth.profile_server.protocol.CanStoreMap;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;


/**
 * Created by mati on 08/11/16.
 */

//
// Home Node Request and Check-in - Identity registration and first login full sequence
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
// A) Provided that the node's clNonCustomer port is different from clCustomer port, the sequence is as follows:
//   1) Identity connects to the node on its primary port and gets information about its roles to ports mapping.
//   2) Identity connects to the clNonCustomer port and sends a home node request.
//   3) Identity connects to the clCustomer port and performs a check-in.
//
// B) If clNonCustomer port is equal to clCustomer port, the sequence is as follows:
//   1) Identity connects to the node on its primary port and gets information about its roles to ports mapping.
//   2) Identity connects to the clNonCustomer+clCustomer port and sends a home node request.
// 3) Identity performs a check-in over the connection from 2).

public interface ProfileServer {


    ProfSerRequest ping(IopProfileServer.ServerRoleType portType) throws Exception;

    ProfSerRequest listRolesRequest() throws Exception;

    ProfSerRequest registerHostRequest(Signer signer,byte[] identityPk, String identityType) throws Exception;

    ProfSerRequest startConversationNonCl(byte[] clientPk, byte[] challenge) throws Exception;
    ProfSerRequest startConversationNonCl(String clientPk, String challenge) throws Exception;

    ProfSerRequest startConversationCl(byte[] clientPk, byte[] challenge) throws Exception;
    ProfSerRequest startConversationCl(String clientPk, String challenge) throws Exception;

    ProfSerRequest checkIn(byte[] signedChallenge, Signer signer) throws Exception;

    ProfSerRequest updateProfileRequest(Signer signer,byte[] profilePublicKey,String profType, byte[] version, String name, byte[] img,byte[] imgHash, int latitude, int longitude, String extraData) throws Exception;

   /* ProfSerRequest updateExtraData(Signer signer,byte[] profilePublicKey,String profType, String extraData) throws Exception;*/

    ProfSerRequest storeCanDataRequest(CanStoreMap canStoreMap);

    /**
     * Search request
     */

    ProfSerRequest searchProfilesRequest(boolean onlyHostedProfiles,
                              boolean includeThumbnailImages,
                              int maxResponseRecordCount,
                              int maxTotalRecordCount,
                              String profileType,
                              String profileName,
                              String extraData) throws CantConnectException, CantSendMessageException;

    ProfSerRequest searchProfilesRequest(boolean onlyHostedProfiles,
                              boolean includeThumbnailImages,
                              int maxResponseRecordCount,
                              int maxTotalRecordCount,
                              String profileType,
                              String profileName,
                              int latitude,
                              int longitude,
                              int radius,
                              String extraData) throws CantConnectException, CantSendMessageException;

    ProfSerRequest searchProfilePartRequest(int recordIndex, int recordCount) throws CantConnectException, CantSendMessageException;

    ProfSerRequest addApplcationService(String applicationService) throws CantConnectException, CantSendMessageException;

    ProfSerRequest getProfileInformationRequest(byte[] profileNetworkId, boolean applicationServices, boolean thumbnail, boolean profileImage) throws CantConnectException, CantSendMessageException;

    ProfSerRequest callIdentityApplicationServiceRequest(byte[] profileNetworkId, String appService);

    ProfSerRequest appServiceSendMessageRequest(byte[] token,byte[] msg);

    // responses

    ProfSerRequest incomingCallNotificationResponse(int msgId);

    ProfSerRequest appServiceReceiveMessageNotificationResponse(String token,int msgId);

    void addHandler(PsSocketHandler hanlder);

    void closePort(IopProfileServer.ServerRoleType portType) throws IOException;

    void closeCallChannel(String callToken) throws IOException;

    void shutdown() throws IOException;

}
