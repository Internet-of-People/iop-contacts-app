package org.fermat.redtooth.forum;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.apache.http.HttpResponse;
import org.fermat.redtooth.forum.discourge.com.wareninja.opensource.discourse.DiscourseApiClient;
import org.fermat.redtooth.forum.discourge.com.wareninja.opensource.discourse.DiscouseApiConstants;
import org.fermat.redtooth.forum.discourge.com.wareninja.opensource.discourse.utils.ResponseModel;
import org.fermat.redtooth.forum.wrapper.AdminNotificationException;
import org.fermat.redtooth.forum.wrapper.CantGetProposalsFromServerException;
import org.fermat.redtooth.forum.wrapper.ResponseMessageConstants;
import org.fermat.redtooth.forum.wrapper.ServerWrapper;
import org.fermat.redtooth.global.exceptions.ConnectionRefusedException;
import org.fermat.redtooth.global.exceptions.NotValidParametersException;
import org.fermat.redtooth.governance.propose.Proposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fermat.redtooth.forum.wrapper.ResponseMessageConstants.ADMIN_NOTIFICATION;
import static org.fermat.redtooth.forum.wrapper.ResponseMessageConstants.ADMIN_NOTIFICATION_MESSAGE;
import static org.fermat.redtooth.forum.wrapper.ResponseMessageConstants.ADMIN_NOTIFICATION_TYPE;
import static org.fermat.redtooth.forum.wrapper.ResponseMessageConstants.REGISTER_ERROR_STR;
import static org.fermat.redtooth.utils.StreamsUtils.convertInputStreamToString;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by mati on 28/11/16.
 */

public class ForumClientDiscourseImp implements ForumClient {

    private static final Logger LOG = LoggerFactory.getLogger(ForumClientDiscourseImp.class);

    private final ForumConfigurations conf;

    private DiscourseApiClient client;

    private ForumProfile forumProfile;

    private ServerWrapper serverWrapper;

    private String forunLink = DiscouseApiConstants.FORUM_TEST_URL;

    private String apiKey;

    public ForumClientDiscourseImp(ForumConfigurations forumConfigurations, ServerWrapper serverWrapper) {
        this.conf = forumConfigurations;
        this.serverWrapper = serverWrapper;
        apiKey = conf.getApiKey();
        forumProfile = forumConfigurations.getForumUser();
        init();
    }

    private void init(){
        client = new DiscourseApiClient(
                forunLink,//args[0] // api_url  : e.g. http://your_domain.com
                apiKey, //, args[1] // api_key : you get from discourse admin
                (forumProfile!=null)?forumProfile.getUsername():null//, args[2] // api_username : you make calls on behalf of
        );
    }


    @Override
    public ForumProfile getForumProfile() {
        return forumProfile;
    }

    @Override
    public boolean isRegistered() {
        return conf.isRegistered();
    }

    /**
     *  Check if the user exist
     *
     * @param username
     * @param password
     * @return
     */
    @Override
    public boolean connect(String username, String password) throws InvalidUserParametersException, ConnectionRefusedException, AdminNotificationException {
        //init();
        LOG.info("connect");
        if (apiKey == null) {
            try {
                // request api key to Mati server, if the user exist the api key will be created.

                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put("username", username);
                parameters.put("password", password);

                apiKey = serverWrapper.connect(parameters);
                if (apiKey != null) {
                    conf.setApiKey(apiKey);
                    this.client.setApiKey(apiKey);
                    if (forumProfile != null) {
                        if (!forumProfile.getUsername().equals(username) && !forumProfile.getPassword().equals(password)) {
                            forumProfile = new ForumProfile(username, password, null);
                        }
                    } else {
                        forumProfile = new ForumProfile(username, password, null);
                    }
                    saveForumData(true, username, password, null);
                    return true;
                } else
                    return false;

            } catch (ConnectionRefusedException e) {
                throw e;
            } catch (AdminNotificationException e){
                throw e;
            }catch (Exception e){
                e.printStackTrace();
            }
        }else
            LOG.error("Api key != null");
        return false;
    }

    /**
     * todo: falta probar el tema del topic id y el forum id..
     *
     * @param title
     * @param category
     * @param raw
     * @return
     * @throws CantCreateTopicException
     */
    @Override
    public int createTopic(String title, String category, String raw) throws CantCreateTopicException, AdminNotificationException {
        LOG.info("CreateTopic");
        try {
            Map<String, String> parameters = new HashMap<>();
            parameters.put("username", forumProfile.getUsername());
            //parameters.put("username", new_username);
            parameters.put("category", category);
            parameters.put("title", title);
            parameters.put("raw", raw);
            ResponseModel responseModel = client.createTopic(parameters);
            if (responseModel.meta.code > 201) {
                LOG.error("topic fail. data: " + responseModel.data);
                if (!captureFail(responseModel)) {
                    int index = responseModel.data.toString().lastIndexOf("|");
                    String errorDetail = responseModel.data.toString().substring(index + 1);
                    ;
                    if (errorDetail.contains("<!DOCTYPE html>")) {
                        throw new CantCreateTopicException("Cant create topic, something bad happen with the forum");
                    } else {
                        LOG.error("forum fail with proposal: title=" + title + ", category=" + category + ", raw=" + raw + "\n" + responseModel.data.toString());
                        LOG.error("####");
                        throw new CantCreateTopicException(errorDetail);
                    }
                }
            } else {
                LOG.info("topic created.");
                LOG.info("### data: " + responseModel.data.toString());
//                JSONObject jsonObject = new JSONObject(responseModel.data.toString());
                JsonObject jsonObject = (JsonObject) new JsonParser().parse(responseModel.data.toString());

                checkAdminNotification(jsonObject);
                // post id
                int postId = Integer.valueOf(jsonObject.get("id").getAsString());
                // forum id
                int topic_id = Integer.valueOf(jsonObject.get("topic_id").getAsString());
                LOG.info("## Forum id obtained: " + topic_id);
                return topic_id;
            }
        } catch (CantCreateTopicException e) {
            throw e;
        } catch (ResponseFailException e) {
            throw new CantCreateTopicException(e.getMessage());
        } catch(AdminNotificationException e){
            throw e;
        }catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }

    /**
     *
     * curl -X PUT -d id=32 -d post[raw="This is mey weafafearqwrwas]  afafafwaea" http://fermat.community/posts/32.json?api_key=70f4d61d58a2ebcf024e3514e2e64c5e106985ec9d9fd99b9b19bcc8742648e6&api_username=jhon123

     *
     * @param title
     * @param postId
     * @param category
     * @param raw
     * @return
     * @throws CantUpdatePostException
     */
    @Override
    public boolean updatePost(String title, int postId, String category, String raw) throws CantUpdatePostException {
        LOG.info("updatePost");
        Map<String, String> parameters = new HashMap<>();
//        parameters.put("username", forumProfile.getUsername());
        parameters.put("id",String.valueOf(postId));
        parameters.put("category", category);
        parameters.put("title", title);
        parameters.put("raw", raw);
        ResponseModel responseModel = client.updatePost(parameters);
        if (responseModel.meta.code > 201) {
            LOG.error("topic fail. data: " + responseModel.data);
            int index = responseModel.data.toString().lastIndexOf("|");
            String errorDetail = responseModel.data.toString().substring(index+1);
            throw new CantUpdatePostException(errorDetail);
        } else {
            LOG.info("topic updated.");
            LOG.info("### data: "+responseModel.data.toString());
            LOG.info("## Forum id obtained: "+postId);
            return true;
        }
    }

    /**
     * Get topic
     * ../t/:id.json
     *
     * @param forumId
     * @return
     */
    public Proposal getProposal(int forumId) throws AdminNotificationException {
        LOG.info("getForumProposal");
        Proposal proposal = null;
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("id", String.valueOf(forumId));
        ResponseModel responseModel = client.getTopic(parameters);

        try {
//            JSONObject jsonObject = new JSONObject(responseModel.data.toString());
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(responseModel.data.toString());

            checkAdminNotification(jsonObject);

            jsonObject = (JsonObject) jsonObject.get("post_stream");
            JsonArray jsonArray = jsonObject.get("posts").getAsJsonArray();
//            JSONArray jsonArray = new JSONArray(jsonObject.get("posts"));

            // post
            jsonObject = (JsonObject) jsonArray.get(0);
            // body
            String formatedBody = jsonObject.get("cooked").getAsString();
            proposal = Proposal.buildFromBody(formatedBody);
            proposal.setForumId(forumId);

            proposal.setBody(decodeBody(proposal.getBody()));


        } catch (JsonParseException e) {
            e.printStackTrace();
        }

        return proposal;
    }

    /**
     * Get topic
     * ../t/:id.json
     *
     * @param forumId
     * @return
     */
    @Override
    public Proposal getProposalFromWrapper(int forumId) throws AdminNotificationException, CantGetProposalsFromServerException {
        LOG.info("getForumProposal for id: "+forumId);
        Proposal proposal = null;
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("id", String.valueOf(forumId));
        parameters.put("username", forumProfile.getUsername());
        parameters.put("password", forumProfile.getPassword());
        String topicPost = serverWrapper.getTopic(parameters);

        LOG.info("topic post: "+topicPost);

        try {
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(topicPost);

            checkAdminNotification(jsonObject);

            String formatedBody = jsonObject.get(ResponseMessageConstants.TOPIC_POST).getAsString();
            proposal = Proposal.buildFromBody(formatedBody);
            proposal.setForumId(forumId);

            proposal.setBody(decodeBody(proposal.getBody()));

        } catch (JsonParseException e) {
            throw new CantGetProposalsFromServerException(e);
        } catch (Exception e){
            throw new CantGetProposalsFromServerException(e);
        }

        return proposal;
    }

    private String decodeBody(String body){
        String s = body.replace("<br>","\n");
        s = s.replace("&amp;","&");
        return s;
    }

    @Override
    public void getAndCheckValid(Proposal proposal) throws NotValidParametersException, AdminNotificationException {
        Proposal forumProposal = getProposal(proposal.getForumId());
        proposal.equals(forumProposal);
    }

    @Override
    public void clean() {
        forumProfile = null;
        apiKey = null;
        conf.remove();
        client.clean();
    }

    @Override
    public boolean replayTopic(int forumId, String text) throws CantReplayPostException {
        try {
            Map<String, String> parameters = new HashMap<>();
            parameters.put("topic_id", String.valueOf(forumId));
            parameters.put("raw", text);
            ResponseModel responseModel = client.replyToTopic(parameters);
            if (responseModel.meta.code > 201) {
                LOG.error("topic fail. data: " + responseModel.data);
                if (!captureFail(responseModel)) {
                    int index = responseModel.data.toString().lastIndexOf("|");
                    String errorDetail = responseModel.data.toString().substring(index + 1);
                    ;
                    if (errorDetail.contains("<!DOCTYPE html>")) {
                        throw new CantReplayPostException("Cant replay topic, something bad happen with the forum");
                    } else {
                        LOG.error("forum fail with replay: id=" + forumId + ", text=" + text + "\n" + responseModel.data.toString());
                        LOG.error("####");
                        throw new CantReplayPostException(errorDetail);
                    }
                }
            } else {
                LOG.info("replay created.");
                if (responseModel.data!=null) {
                    LOG.info("### data: " + responseModel.data.toString());
                    JsonObject jsonObject = (JsonObject) new JsonParser().parse(responseModel.data.toString());

                    checkAdminNotification(jsonObject);
                    // post id
                    int postId = Integer.valueOf(jsonObject.get("id").getAsString());

                }
                return true;
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new CantReplayPostException(e.getMessage());
        }
        return false;
    }


    public ForumProfile getUser(String username){
        LOG.debug("getUser");
        ForumProfile forumProfile = null;
        Map<String, String> parameters = new HashMap<String, String>();
        parameters = new HashMap<String, String>();
        parameters.put("username", username);
        ResponseModel responseModel = client.getUser(parameters);
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse( ""+responseModel.data );
        JsonArray jsonArray = null;
        JsonObject userObject = null;
        if (jsonElement!=null) {
            userObject = jsonElement.isJsonObject()?jsonElement.getAsJsonObject():null;
            jsonArray = jsonElement.isJsonArray()?jsonElement.getAsJsonArray():null;
        }

        if (userObject!=null && userObject.has("user")) {
            userObject = userObject.getAsJsonObject("user");
        }else
            return null;

        forumProfile = new ForumProfile(
                userObject.get("id").getAsLong(),
                userObject.get("name").getAsString(),
                userObject.get("username").getAsString()
        );
        return forumProfile;

    }

    /**
     *
     *
     * @param forumProfile
     * @return
     */
    private String requestApiKey(ForumProfile forumProfile){
        // -> generate api_key
        String apiKey = null;
        Map<String, String> parameters = new HashMap<String, String>();
        parameters = new HashMap<String, String>();
        parameters.put("userid", ""+forumProfile.getForumId());
        parameters.put("username", forumProfile.getUsername());
        ResponseModel responseModel = client.generateApiKey(parameters);

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject1 = null;
        jsonObject1 = jsonParser.parse( ""+responseModel.data ).getAsJsonObject();

        if (jsonObject1!=null && jsonObject1.has("api_key")) {
            jsonObject1 = jsonObject1.getAsJsonObject("api_key");

            if (jsonObject1.has("key")) {
                apiKey = jsonObject1.get("key").getAsString();
            }
        }
        return apiKey;
    }

    @Override
    public boolean registerUser(String username, String password, String email) throws InvalidUserParametersException, AdminNotificationException {
        //if (forumProfile!=null) throw new IllegalStateException("Forum profile already exist");
        LOG.debug("registerUser");
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("name", username);
        parameters.put("email", email);
        parameters.put("username", username);
        parameters.put("password", password);

        //url = url + "?api_key=" + DiscouseApiConstants.API_KEY + "&api_username=system";

        try {

            HttpResponse httpResponse = serverWrapper.registerUser(parameters);

            InputStream inputStream = null;
            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();
            String result = null;
            // convert inputstream to string
            if (inputStream != null)
                result = convertInputStreamToString(inputStream);

            LOG.info("###########################");
            LOG.info(result);
            LOG.info("###########################");

            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(result);

            checkAdminNotification(jsonObject);

            if (httpResponse.getStatusLine().getStatusCode()==200){
                saveForumData(false,username,password,email);
                forumProfile = new ForumProfile(username,password,email);
                return true;
            }else {
                if (jsonObject.has(REGISTER_ERROR_STR)){
                    throw new InvalidUserParametersException(jsonObject.get(REGISTER_ERROR_STR).toString());
                }
                return false;
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AdminNotificationException e) {
            throw e;
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }


    private boolean checkIfResponseFail(ResponseModel responseModel){
        return responseModel.meta.code>201 || responseModel.data==null;
    }

    /**
     *
     * @param responseModel
     * @return true if this method capture the error
     */
    private boolean captureFail(ResponseModel responseModel) throws ResponseFailException {
        LOG.error("captureFail");
        switch (responseModel.meta.code){
            case 405:
                throw new ResponseFailException("Connection is block by your router or ISP, please try later.");
            default:

                break;
        }
        return false;

    }

    private void saveForumData(boolean isRegistered,String username,String password,String email){
        conf.setIsRegistered(isRegistered);
        conf.setForumUser(username,password,email);
    }


    public String getJsonFromParams(Map<String,String> requestParams) {

        JsonObject jsonObject = new JsonObject();

        for (Map.Entry<String, String> stringStringEntry : requestParams.entrySet()) {
            if ( !stringStringEntry.getKey().equalsIgnoreCase("api_key") && !stringStringEntry.getKey().equalsIgnoreCase("api_username")) {
                jsonObject.addProperty(
                        stringStringEntry.getKey()
                        , ""+stringStringEntry.getValue()//, requestParam.getValueStr()
                );
            }
        }
        return jsonObject.toString();
    }


    public void setForunLink(String forunLink) {
        this.forunLink = forunLink;
    }

    private void checkAdminNotification(JsonObject jsonObject) throws AdminNotificationException {
        JsonElement json = jsonObject.get(ResponseMessageConstants.ADMIN_NOTIFICATION);
        if (json!=null) {
            if (!json.isJsonNull()) {
                JsonObject adminNotJson = jsonObject.getAsJsonObject(ADMIN_NOTIFICATION);
                int admNotType = adminNotJson.get(ADMIN_NOTIFICATION_TYPE).getAsInt();
                String message = adminNotJson.get(ADMIN_NOTIFICATION_MESSAGE).getAsString();
                throw new AdminNotificationException(admNotType, message);
            }
        }
    }
}
