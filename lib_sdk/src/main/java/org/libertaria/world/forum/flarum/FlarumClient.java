package org.libertaria.world.forum.flarum;//package org.libertaria.world.forum.flarum;
//
//import org.apache.http.HttpResponse;
//import org.apache.http.StatusLine;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.apache.http.params.BasicHttpParams;
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//
//
//
///**
// * Created by mati on 22/11/16.
// */
//
//public class FlarumClient {
//
//
//    public static final String FORUM_URL = "http://fermat.community/api/";
//
//    private static final Logger LOG = LoggerFactory.getLogger(FlarumClient.class);
//
//    /** Forum session token */
//    private String token;
//    private long userId;
//
//    private boolean isConnected = false;
//
//    private ForumConfigurations conf;
//
//
//    public FlarumClient(ForumConfigurations conf) {
//        this.conf = conf;
//    }
//
//    public boolean registerUser(String username,String password,String email) throws FlarumClientInvalidDataException {
//
//        String url = FORUM_URL+"users";
//
//        String data =
//                "{\"data\": " +
//
//                        "{\"attributes\": " +
//                        "{" +
//                        "\"username\": \""+username+"\"," +
//                        "\"password\": \""+password+"\"," +
//                        "\"email\": \""+email+"\"" +
//                        "}" +
//                        "}" +
//
//                        "}";
//
//        try {
//            String result = formatResponse(postRequest(url,token,data));
//
//            LOG.info("Register user response: "+result);
//
//            return true;
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (FlarumClientException e){
//            e.printStackTrace();
//        }
//        return false;
//    }
//
//
//    public boolean connect(String username, String password) throws FlarumClientInvalidDataException {
//
//        String url = FORUM_URL+"token";
//
//
//        String data =
//                "{" +
//                        "\"identification\": \""+username+"\"," +
//                        "\"password\": \""+password+"\"" +
//                        "}";
//
//        try {
//
//            String result = formatResponse(postRequest(url,null,data));
//
//            LOG.info("connect user response: "+result);
//
//            JSONObject jsonObject = new JSONObject(result);
//            token = jsonObject.getString("token");
//            userId = jsonObject.getLong("userId");
//
//            isConnected = true;
//
//            return isConnected;
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (FlarumClientException e){
//            e.printStackTrace();
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        return false;
//    }
//
//
//    public boolean createDiscussion(String title,String content) throws FlarumClientInvalidDataException {
//
//        if (!isConnected) throw new IllegalStateException("Client not connected");
//
//        String url = FORUM_URL+"discussions";
//
//        String data =
//                "{\"data\": " +
//
//                        "{\"attributes\": " +
//                        "{" +
//                        "\"title\": \""+title+"\"," +
//                        "\"content\": \""+content+"\"," +
//                        "\"tags\": \"community-foundation\"" +
//                        "}" +
//                        "}" +
//
//                        "}";
//
//        try {
//
//            String responseContent = formatResponse(postRequest(url,token,data));
//
//            LOG.info("Forum create discusion response: "+responseContent);
//
//
//            return true;
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (FlarumClientException e) {
//            e.printStackTrace();
//        }
//
//        return false;
//    }
//
//
//
//
//
//    private HttpResponse postRequest(String url, String token, String data) throws IOException, FlarumClientException, FlarumClientInvalidDataException {
//        HttpClient client = new DefaultHttpClient(new BasicHttpParams());
//        HttpPost httpPost = new HttpPost(url);
//        httpPost.setHeader("Content-type", "application/vnd.api+json");
//        httpPost.setHeader("Authorization","Token "+token);
//
//        //passes the results to a string builder/entity
//        StringEntity se = new StringEntity(data);
//        //sets the postRequest request as the resulting string
//        httpPost.setEntity(se);
//
//        // make GET request to the given URL
//        HttpResponse httpResponse = client.execute(httpPost);
//
//        if (httpResponse.getStatusLine().getStatusCode()!=200) {
//            StatusLine statusLine = httpResponse.getStatusLine();
//            LOG.error("Response " + httpResponse);
//
//            // invalid data
//            if (statusLine.getStatusCode() == 202) {
//                String formatedResponse = formatResponse(httpResponse);
//                throw new FlarumClientInvalidDataException("HttpRequest fail, content: " + formatResponse(httpResponse) +
//                        " detail: " + statusLine.getReasonPhrase() + ",protocol: " + statusLine.getProtocolVersion() + ",statuc code: " + statusLine.getStatusCode());
//
//            } else{
//                throw new FlarumClientException("HttpRequest fail, content: " + formatResponse(httpResponse) +
//                        " detail: " + statusLine.getReasonPhrase() + ",protocol: " + statusLine.getProtocolVersion() + ",statuc code: " + statusLine.getStatusCode());
//            }
//        }
//
//        return httpResponse;
//    }
//
//
//    private String formatResponse(HttpResponse httpResponse){
//        String result = null;
//        try {
//            InputStream inputStream = null;
//            // receive response as inputStream
//            inputStream = httpResponse.getEntity().getContent();
//            // convert inputstream to string
//            if (inputStream != null)
//                result = convertInputStreamToString(inputStream);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//        return result;
//    }
//
//
//
//    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
//        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
//        String line = "";
//        String result = "";
//        while((line = bufferedReader.readLine()) != null)
//            result += line;
//
//        inputStream.close();
//        return result;
//
//    }
//
//
//    public boolean isConnected() {
//        return isConnected;
//    }
//
//    public boolean isRegistered() {
//        return conf.isRegistered();
//    }
//
//    public String getToken() {
//        return token;
//    }
//}
