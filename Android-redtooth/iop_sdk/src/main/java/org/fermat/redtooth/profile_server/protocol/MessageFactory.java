package org.fermat.redtooth.profile_server.protocol;

import com.google.protobuf.ByteString;

import java.util.concurrent.atomic.AtomicInteger;

import org.fermat.redtooth.profile_server.Signer;


/**
 * Created by mati on 20/09/16.
 */
public class MessageFactory {

    private static AtomicInteger messageIdGenerator = new AtomicInteger(0);

    public static final int NO_LOCATION = 0x7FFFFFFF;

    /**
     * Build ping request message
     *
     * @param payload
     * @param version
     * @return
     */
    public static IopProfileServer.Message buildPingRequestMessage(byte[] payload,byte[] version){
        IopProfileServer.PingRequest.Builder pingRequest = IopProfileServer.PingRequest.newBuilder().setPayload(ByteString.copyFrom(payload));
        IopProfileServer.SingleRequest.Builder singleRequestBuilder = IopProfileServer.SingleRequest.newBuilder().setPing(pingRequest).setVersion(ByteString.copyFrom(version));
        return buildMessage(singleRequestBuilder);

    }

    /**
     * Build ping response message
     *
     * @param pingResponse
     * @param version
     * @return
     */
    public static IopProfileServer.Message buildPingResponseMessage(IopProfileServer.PingResponse pingResponse,byte[] version){
        IopProfileServer.SingleResponse.Builder singleResponseBuilder = IopProfileServer.SingleResponse.newBuilder().setPing(pingResponse).setVersion(ByteString.copyFrom(version));
        return buildMessage(singleResponseBuilder);
    }

    /**
     *  Build server list roles request message
     *
     * @param version
     * @return
     */

    public static IopProfileServer.Message buildServerListRolesRequestMessage(byte[] version){
        IopProfileServer.ListRolesRequest.Builder listRolesRequest = IopProfileServer.ListRolesRequest.newBuilder();
        IopProfileServer.SingleRequest.Builder singleRequest = IopProfileServer.SingleRequest.newBuilder().setListRoles(listRolesRequest).setVersion(ByteString.copyFrom(version));
        return buildMessage(singleRequest);
    }

    /**
     *  Build server list roles response message
     *
     * @param listRolesRequest
     * @param version
     * @return
     */
    public static IopProfileServer.Message buildServerListRolesResponseMessage(IopProfileServer.ListRolesResponse listRolesRequest,byte[] version){
        IopProfileServer.SingleResponse.Builder singleRequest = IopProfileServer.SingleResponse.newBuilder().setListRoles(listRolesRequest).setVersion(ByteString.copyFrom(version));
        return buildMessage(singleRequest);
    }

    public static IopProfileServer.Message buildHomeNodeRequestRequest(byte[] identityPk,String identityType,long contractStartTime,byte[] planId){
        IopProfileServer.HostingPlanContract.Builder contract = IopProfileServer.HostingPlanContract
                .newBuilder()
                .setIdentityPublicKey(ByteString.copyFrom(identityPk))
                .setIdentityType(identityType)
                .setStartTime(contractStartTime);
        if (planId!=null) contract.setPlanId(ByteString.copyFrom(planId));
        return buildHomeNodeRequestRequest(contract);
    }

    /**
     *  Build serverRole
     *
     * @param roleType
     * @param port
     * @param isSecure
     * @param isTcp
     * @return
     */
    public static IopProfileServer.ServerRole buildServerRole(IopProfileServer.ServerRoleType roleType, int port, boolean isSecure, boolean isTcp){
        return IopProfileServer.ServerRole.newBuilder().setIsTcp(isTcp).setIsTls(isSecure).setPort(port).setRole(roleType).build();
    }

    public static IopProfileServer.HostingPlan buildHomeNodePlan(String planId,String identityType,int billingPeriodseconds,long fee){
        return IopProfileServer.HostingPlan.newBuilder().setIdentityType(identityType).setBillingPeriodSeconds(billingPeriodseconds).setPlanId(ByteString.copyFromUtf8(planId)).setFee(fee).build();
    }

    public static IopProfileServer.HostingPlanContract buildHomeNodePlanContract(String identityPk, long startTime, String planId){
        return IopProfileServer.HostingPlanContract.newBuilder().setIdentityPublicKey(ByteString.copyFromUtf8(identityPk)).setStartTime(startTime).setPlanId(ByteString.copyFromUtf8(planId)).build();
    }

    public static IopProfileServer.Message buildHomeNodeRequestRequest(IopProfileServer.HostingPlanContract.Builder contract){
        IopProfileServer.RegisterHostingRequest homeNodeRequestRequest = IopProfileServer.RegisterHostingRequest.newBuilder().setContract(contract).build();
        IopProfileServer.ConversationRequest.Builder conversaBuilder = IopProfileServer.ConversationRequest.newBuilder().setRegisterHosting(homeNodeRequestRequest);
        return buildMessage(conversaBuilder);
    }

    public static IopProfileServer.Message buildHomeNodeResponseRequest(IopProfileServer.HostingPlanContract contract,String signature){
        IopProfileServer.RegisterHostingResponse homeNodeRequestRequest = IopProfileServer.RegisterHostingResponse.newBuilder().setContract(contract).build();
        IopProfileServer.ConversationResponse.Builder conversaBuilder = IopProfileServer.ConversationResponse.newBuilder().setRegisterHosting(homeNodeRequestRequest);
        conversaBuilder.setSignature(ByteString.copyFromUtf8(signature));
        return buildMessage(conversaBuilder);
    }

    public static IopProfileServer.Message buildCheckInResponse(String signature){
        IopProfileServer.CheckInResponse checkInResponse = IopProfileServer.CheckInResponse.newBuilder().build();
        IopProfileServer.ConversationResponse.Builder conversaBuilder = IopProfileServer.ConversationResponse.newBuilder().setCheckIn(checkInResponse);
        conversaBuilder.setSignature(ByteString.copyFromUtf8(signature));
        return buildMessage(conversaBuilder);
    }



    public static IopProfileServer.Message buildStartConversationRequest(byte[] clientPk,byte[] challenge,byte[] version){
        IopProfileServer.StartConversationRequest.Builder builder = IopProfileServer.StartConversationRequest
                .newBuilder()
                    .setClientChallenge(ByteString.copyFrom(challenge))
                    .setPublicKey(ByteString.copyFrom(clientPk))
                    .addSupportedVersions(ByteString.copyFrom(version));
        return buildMessage(builder);
    }


    public static IopProfileServer.Message buildCheckInRequest(byte[] nodeChallenge, Signer signer){
        IopProfileServer.CheckInRequest.Builder builder = IopProfileServer.CheckInRequest
                .newBuilder()
                    .setChallenge(ByteString.copyFrom(nodeChallenge));
        IopProfileServer.CheckInRequest checkInRequest = builder.build();
        byte[] signature = signer.sign(checkInRequest.toByteArray());
        return buildMessage(checkInRequest,signature);
    }

    public static IopProfileServer.Message buildUpdateProfileRequest(Signer signer, byte[] version, String name, byte[] img, int latitude, int longitude, String extraData) {
        IopProfileServer.UpdateProfileRequest.Builder updateProfileRequest = IopProfileServer.UpdateProfileRequest.newBuilder();

        if (version!=null && version.length>0){
            updateProfileRequest.setSetVersion(true);
            updateProfileRequest.setVersion(ByteString.copyFrom(version));
        }else
            updateProfileRequest.setSetVersion(false);

        if (name!=null && !name.equals("")){
            updateProfileRequest.setSetName(true);
            updateProfileRequest.setName(name);
        } else
            updateProfileRequest.setSetName(false);

        if (img!=null && img.length>0){
            updateProfileRequest.setImage(ByteString.copyFrom(img));
            updateProfileRequest.setSetImage(true);
        }else
            updateProfileRequest.setSetImage(false);


        /**
         *
         you take DD format (e.g. 70.456789123 -120.58489489)
         10:52
         and you multiply it by 1,000,000 (edited)
         10:52
         70.456789123 -120.58489489 -> 70456789.123 -120584894.89
         10:53
         then you cut off everything on the right of decimal point
         10:53
         70.456789123 -120.58489489 -> 70456789.123 -120584894.89 -> lat=70456789, lon=-120584894
         10:55
         therefore
         10:55
         //                  For latitudes, valid values are in range [-90,000,000;90,000,000], for longitude the range is
         //                  [-179,999,999;180,000,000]. A special constant NO_LOCATION = (int)0xFFFFFFFF is reserved for no location.

         si la locación es negativa la tengo que poner positiva.
         *
         */

        // todo: ver esto..
//        if (latitude>0 && longitude>0){
            updateProfileRequest.setSetLocation(true);
            updateProfileRequest.setLatitude(latitude);
            updateProfileRequest.setLongitude(longitude);
//        }else {
//            updateProfile.setSetLocation(false);
//            updateProfile.setLongitude(0xFFFFFFFF);
//            updateProfile.setLatitude(0xFFFFFFFF);
//        }

        if (extraData!=null && !extraData.equals("")){
            updateProfileRequest.setExtraData(extraData);
            updateProfileRequest.setSetExtraData(true);
        }else
            updateProfileRequest.setSetExtraData(false);


        IopProfileServer.UpdateProfileRequest request = updateProfileRequest.build();
        byte[] signature = signer.sign(request.toByteArray());
        return buildMessage(request,signature);
    }

    /**
     *
     * // Asks a profile server for a list of all identities that match the search criteria. This search never returns
     // profiles of old customer identities who cancelled their hosting agreements, even if the profile server still
     // holds some information about those identities.
     //
     // Each search request only produces a limited number of results. The maximal size of the first set of results
     // is provided by 'maxResponseRecordCount' field. The response to this message contains up to 'maxResponseRecordCount'
     // results. If there are more results available, they are saved to the conversation context, which enables the client
     // to obtain more results with subsequent ProfileSearchPartRequest messages.
     //
     // The profile server will not save more than 'maxTotalRecordCount' search requests.
     // The profile server has to allow the client to get additional results at least 1 minute from receiving
     // ProfileSearchRequest, but it can maintain the result cache for longer than that.
     //
     // Once the client sends another ProfileSearchRequest, or if it disconnects, the old search results are discarded.
     //
     // Roles: clNonCustomer, clCustomer
     //
     // Conversation status: ConversationStarted, Verified, Authenticated
     *
     * @param onlyHostedProfiles  // If set to true, the profile server only returns profiles of its own customers.
     *                            // If set to false, profiles from the server's neighborhood can be included in the result.
     *
     * @param includeThumbnailImages // If set to true, the response will include a thumbnail image of each profile.
     *
     * @param maxResponseRecordCount  // Maximal number of results to be delivered in the response. If 'includeThumbnailImages'
     *                                // is true, this has to be an integer between 1 and 100. If 'includeThumbnailImages' is false,
     *                                // this has to be an integer between 1 and 1000. The value must not be greater than 'maxTotalRecordCount'.
     *
     * @param maxTotalRecordCount     // Maximal number of total results that the profile server will look for and save. If 'includeThumbnailImages'
     *                                // is true, this has to be an integer between 1 and 1,000. If 'includeThumbnailImages' is false,
     *                                // this has to be an integer between 1 and 10,000.
     *
     *
     * @param profileType    // WildcardType or empty string. If not empty, the profile server will only return profiles
     *                       // of identity types that match the wildcard string. If empty, all identity types are allowed.
     *                       // Max 64 bytes long.
     *
     * @param profileName    // WildcardType or empty string. If not empty, the profile server  will only return profiles
     *                       // with names that match the wildcard string. If empty, all profile names are allowed.
     *                       // Max 64 bytes long.
     *
     * @param latitude       // LocationType. Encoded target GPS location latitude or NO_LOCATION. If not NO_LOCATION,
     *                       // it is, in combination with 'longitude' and 'radius' a specification of target area,
     *                       // where the identity has to be located (according to its profile information) in order to be
     *                       // included in the search results. If NO_LOCATION, 'longitude' and 'radius' are ignored
     *                       // and all locations are allowed.
     *
     * @param longitude      // LocationType. If 'latitude' is not NO_LOCATION, this is encoded target GPS location longitude.
     *
     * @param radius         // If 'latitude' is not NO_LOCATION, this is target location radius in metres.
     *
     * @param extraData      // RegexType or empty string. If not empty, specifies the regular expression that identity
     *                       // profile's extra data information must match in order to be included in the results.
     *                       // If empty, no filtering based on extra data information is made.
     *                       // Max 256 bytes long.
     *
     *
     *                       todo: faltan las validaciones de cada campo..
     * @return
     */
    public static IopProfileServer.Message buildProfileSearchRequest(
            boolean onlyHostedProfiles,
            boolean includeThumbnailImages,
            int maxResponseRecordCount,
            int maxTotalRecordCount,
            String profileType,
            String profileName,
            int latitude,
            int longitude,
            int radius,
            String extraData){


        IopProfileServer.ProfileSearchRequest.Builder builder = IopProfileServer.ProfileSearchRequest.newBuilder();

        builder.setIncludeHostedOnly(onlyHostedProfiles);
        builder.setIncludeThumbnailImages(includeThumbnailImages);
        builder.setMaxResponseRecordCount(maxResponseRecordCount);
        builder.setMaxTotalRecordCount(maxTotalRecordCount);
        if (profileType!=null)
            builder.setType(profileType);
        if (profileName!=null)
            builder.setName(profileName);
        builder.setLatitude(latitude);
        builder.setLongitude(longitude);
        if (radius>0)
            builder.setRadius(radius);
        if (extraData!=null)
            builder.setExtraData(extraData);

        return buildMessage(builder);
    }

    /**
     * Request to get another part from the search query.
     * This request only makes sense only if the client previously sent ProfileSearchRequest to
     * the profile server and the search result contained more records than the server provided
     * in the ProfileSearchResponse, and the search results have not expired yet.
     *
     * @param recordIndex   Zero-based record index of the first result to retrieve. It has to be an integer between 0 and 'ProfileSearchResponse.totalRecordCount' - 1.
     * @param  recordCount  Number of results to obtain. 'recordIndex' + 'recordCount' must not be greater than 'ProfileSearchResponse.totalRecordCount'.
     * If 'ProfileSearchResponse.includeThumbnailImages' was set, this has to be an integer between 1 and 100,
     * otherwise it has to be an integer between 1 and 1,000.
     */

    public static IopProfileServer.Message buildSearcProfilePartRequest(int recordIndex, int recordCount) {

        IopProfileServer.ProfileSearchPartRequest.Builder builder = IopProfileServer.ProfileSearchPartRequest.newBuilder().setRecordIndex(recordIndex).setRecordCount(recordCount);
        return buildMessage(builder);
    }

    public static IopProfileServer.Message buildApplicationServiceAddRequest(String serviceName){
        IopProfileServer.ApplicationServiceAddRequest.Builder builder = IopProfileServer.ApplicationServiceAddRequest.newBuilder();
        builder.addServiceNames(serviceName);
        return buildMessage(builder);
    }

    public static IopProfileServer.Message buildGetIdentityInformationRequest(String profilePk,boolean includeApplicationServices,boolean includeThumbnail,boolean includeProfileImage) {
        IopProfileServer.GetIdentityInformationRequest.Builder geBuilder = IopProfileServer.GetIdentityInformationRequest.newBuilder();
        geBuilder.setIdentityNetworkId(ByteString.copyFromUtf8(profilePk));
        geBuilder.setIncludeApplicationServices(includeApplicationServices);
        geBuilder.setIncludeThumbnailImage(includeThumbnail);
        geBuilder.setIncludeProfileImage(includeProfileImage);
        return buildMessage(geBuilder);
    }


    /**
     * Error messages
     */


    public static IopProfileServer.Message buildInvalidMessageHeaderResponse(){
        return buildMessage(IopProfileServer.Status.ERROR_PROTOCOL_VIOLATION);
    }

    public static IopProfileServer.Message buildVersionNotSupportResponse() {
        // todo: Creo que se devuelve el unnsopported cuando la versión no es valida, deberia chequear esto..
        return buildMessage(IopProfileServer.Status.ERROR_UNSUPPORTED);
    }


    /**
     *  Private builders
     */

    private static IopProfileServer.Message buildMessage(IopProfileServer.Status responseStatus){
        return buildMessage(IopProfileServer.Response.newBuilder().setStatus(responseStatus));
    }

    private static IopProfileServer.Message buildMessage(IopProfileServer.SingleRequest.Builder singleRequest){
        IopProfileServer.Request.Builder requestBuilder = IopProfileServer.Request.newBuilder().setSingleRequest(singleRequest);
        return buildMessage(requestBuilder);
    }

    private static IopProfileServer.Message buildMessage(IopProfileServer.ConversationRequest.Builder conversationRequest){
        IopProfileServer.Request.Builder requestBuilder = IopProfileServer.Request.newBuilder().setConversationRequest(conversationRequest);
        return buildMessage(requestBuilder);
    }

    private static IopProfileServer.Message buildMessage(IopProfileServer.Request.Builder request){
        IopProfileServer.Message.Builder messageBuilder = IopProfileServer.Message.newBuilder().setRequest(request);
        return initMessage(messageBuilder);
    }

    private static IopProfileServer.Message buildMessage(IopProfileServer.SingleResponse.Builder singleResponse){
        IopProfileServer.Response.Builder responseBuilder = IopProfileServer.Response.newBuilder().setSingleResponse(singleResponse);
        return buildMessage(responseBuilder);
    }

    private static IopProfileServer.Message buildMessage(IopProfileServer.Response.Builder response){
        IopProfileServer.Message.Builder messageBuilder = IopProfileServer.Message.newBuilder().setResponse(response);
        return initMessage(messageBuilder);
    }

    private static IopProfileServer.Message buildMessage(IopProfileServer.ConversationResponse.Builder conversationResponse) {
        IopProfileServer.Response.Builder requestBuilder = IopProfileServer.Response.newBuilder().setConversationResponse(conversationResponse);
        return buildMessage(requestBuilder);
    }

    private static IopProfileServer.Message buildMessage(IopProfileServer.CheckInRequest checkInRequestBuilder,byte[] signature) {
        return buildMessage(IopProfileServer.ConversationRequest.newBuilder().setSignature(ByteString.copyFrom(signature)).setCheckIn(checkInRequestBuilder));
    }
    private static IopProfileServer.Message buildMessage(IopProfileServer.UpdateProfileRequest updateProfileRequest,byte[] signature) {
        return buildMessage(IopProfileServer.ConversationRequest.newBuilder().setSignature(ByteString.copyFrom(signature)).setUpdateProfile(updateProfileRequest));
    }

    private static IopProfileServer.Message buildMessage(IopProfileServer.StartConversationRequest.Builder startConversationBuilder) {
        return buildMessage(IopProfileServer.ConversationRequest.newBuilder().setStart(startConversationBuilder));
    }

    private static IopProfileServer.Message buildMessage(IopProfileServer.ProfileSearchRequest.Builder builder) {
        return buildMessage(IopProfileServer.ConversationRequest.newBuilder().setProfileSearch(builder));
    }

    private static IopProfileServer.Message buildMessage(IopProfileServer.ProfileSearchPartRequest.Builder builder) {
        return buildMessage(IopProfileServer.ConversationRequest.newBuilder().setProfileSearchPart(builder));
    }

    private static IopProfileServer.Message buildMessage(IopProfileServer.ApplicationServiceAddRequest.Builder builder) {
        return buildMessage(IopProfileServer.ConversationRequest.newBuilder().setApplicationServiceAdd(builder));
    }

    private static IopProfileServer.Message buildMessage(IopProfileServer.GetIdentityInformationRequest.Builder builder){
        return buildMessage(IopProfileServer.SingleRequest.newBuilder().setGetIdentityInformation(builder));
    }

    private static IopProfileServer.Message initMessage(IopProfileServer.Message.Builder messageBuilder) {
        messageBuilder.setId(getMessageId());
        return messageBuilder.build();
    }

    private static int getMessageId(){
        return messageIdGenerator.incrementAndGet();
    }



}
