package org.fermat.redtooth.price_apis;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.fermat.redtooth.forum.discourge.com.wareninja.opensource.discourse.utils.RequestParameter;
import org.fermat.redtooth.forum.wrapper.CantGetProposalsFromServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fermat.redtooth.utils.StreamsUtils.convertInputStreamToString;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by mati on 22/02/17.
 */

public class MarketCapApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(MarketCapApiClient.class);

    private static final String URL = "https://api.coinmarketcap.com/v1/";


    public BigDecimal getIoPPrice() throws CantGetProposalsFromServerException {
        String url = this.URL+"ticker/internet-of-people/";

        BigDecimal iopUsdPrice = null;

        List<RequestParameter> requestParams = new ArrayList<>();
        HttpResponse httpResponse = null;
        String result = null;
        try {
            int i = 0;
            for (RequestParameter requestParam: requestParams) {
                url += (i==0 && !url.contains("?"))?"?":"&";
                url += requestParam.format();
                i++;
            }

            LOG.info("getIoPPrice URL: "+url);

            BasicHttpParams basicHttpParams = new BasicHttpParams();
            HttpConnectionParams.setSoTimeout(basicHttpParams, (int) TimeUnit.MINUTES.toMillis(1));
            HttpClient client = new DefaultHttpClient(basicHttpParams);
            HttpGet httpGet = new HttpGet(url);
            //httpPost.setHeader("Content-type", "application/vnd.api+json");
            httpGet.addHeader("Accept", "text/html,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
            httpGet.setHeader("Content-type", "application/json");

            // make GET request to the given URL
            httpResponse = client.execute(httpGet);
            InputStream inputStream = null;
            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if (inputStream != null)
                result = convertInputStreamToString(inputStream);


            LOG.info("###########################");
            LOG.info(result);
            LOG.info("###########################");

            if (httpResponse.getStatusLine().getStatusCode()==200){

                JsonParser jsonParser = new JsonParser();
                JsonArray jsonArray = jsonParser.parse(result).getAsJsonArray();
                JsonObject jsonObject = jsonArray.get(0).getAsJsonObject();

                iopUsdPrice = new BigDecimal(jsonObject.get("price_usd").getAsString());

            }else {
                throw new CantGetProposalsFromServerException("Something fail, server code: "+((httpResponse!=null)?httpResponse.getStatusLine().getStatusCode():"null"));
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (HttpHostConnectException e){
            throw new CantGetProposalsFromServerException("url: "+url,e);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IllegalStateException e){
            throw new CantGetProposalsFromServerException("Something fail, server return: "+result+", status code: "+httpResponse.getStatusLine().getStatusCode());
        }
        return iopUsdPrice;
    }

}
