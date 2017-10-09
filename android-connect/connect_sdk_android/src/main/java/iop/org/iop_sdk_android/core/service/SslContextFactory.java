package iop.org.iop_sdk_android.core.service;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;


/**
 * Created by mati on 08/11/16.
 */

public class SslContextFactory implements org.libertaria.world.profile_server.SslContextFactory {

    private static final String TAG = "SslContextFactory";

    private final Context context;

    public SslContextFactory(Context context) {
        this.context = context;
    }

    public SSLContext buildContext() throws Exception{
        try {
            //InputStream inputStream = ((Context)context).getResources().openRawResource(R.raw.profile_server);
            String certificate = "-----BEGIN CERTIFICATE-----\n" +
                    "MIIFXzCCA0egAwIBAgIJAIZyz3FCVLq5MA0GCSqGSIb3DQEBCwUAMEUxCzAJBgNV\n" +
                    "BAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBX\n" +
                    "aWRnaXRzIFB0eSBMdGQwIBcNMTcwNjI4MTY0MTIzWhgPMzAxNjEwMjkxNjQxMjNa\n" +
                    "MEUxCzAJBgNVBAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJ\n" +
                    "bnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
                    "ggIKAoICAQC12NTczbyXeI6LdWFKiPyiPFO6jsbO4uQ60qqUd1EQuGncomeW1ss+\n" +
                    "0rCWPt7nZvbbMfaNzhNwO+WZHg2OMzKCsbzBIMen8ixSvDFp2jugqRB9nzJotih2\n" +
                    "pocoJ4+5RE4l/YFjDGVoxNk5dKrY/WorofU/qGsfFSgVNPuxoTRnkYkjWKnQzwYJ\n" +
                    "YdFcCHoSELJb3FZO4gdkKNDCSyFjeF1S731hgC2YbTTloiLlazEMNEzBw43db2Wt\n" +
                    "ogHUVyIuaiBZxVXdSiD9xhRGxYKBNRSK/k+NRX+0v7X40xaId7SGdxLPKOts3D5a\n" +
                    "L1J/h2/5MG1jq6fgnm34hgy7BNDq8r3P7Ld0x/xK6E/r69tmoaVdNuLMjGKghB5C\n" +
                    "D6G5ckPe88xxMX+S45L785sqB3JbUZrKr5ZR3mRkVBy4bkk1N2JIEoOCtNf3wyQP\n" +
                    "D6pwyDA0iWyeP/QjLljxN6R/yOBULSGceDPUOZwFVEf8Ngnr20LOzy2QKcF8nhm3\n" +
                    "1OVIA2MPiEpgHnz9FtoRRElFx1g0sHmll+Q3yesAU/PnzaSjQ6cwIWaMnb/2dYPe\n" +
                    "zwis4Pdstz2l0rnpgVCynu34doj/28fYgdHuo/5u/ZkYj6uja6nzSO0qyoiIq6Ii\n" +
                    "t4yo8EbKn9Lmb9tVnP/ssrYyvFeIu0w9UTtz2gRf4/mjI6x2B558mQIDAQABo1Aw\n" +
                    "TjAdBgNVHQ4EFgQUdujfrZVn4W6DzN/CF1cdTHsuB40wHwYDVR0jBBgwFoAUdujf\n" +
                    "rZVn4W6DzN/CF1cdTHsuB40wDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQsFAAOC\n" +
                    "AgEAEZ7mLtWpP7CkTnZxHWvRhEYHK8YPbi+/ugkIqSjPRCWECUynq7/XeMHwDddB\n" +
                    "aPNzTLKlBbaWZPyEV2f45UIpHleW8Xf6hbGeeAqeYh9Upmu6qai6Nj82JMWXcJWo\n" +
                    "4zSOKDncjiaROB2620x20FzGTwfdRhxpNC3wXSzXMqj7pSbYAp7u+MnSzIVgsYWD\n" +
                    "4X5WnkIDaab7bkiOygb/C37+rteMQbRtu4Lv/g1/g8yiKvW5PClZfRn5pqFQ5OUw\n" +
                    "xMNMr9b3QIkQA5qPw94QD2Fdomz2JpEwoGQPJVfrHHT1ngFgccoIg7WdOVey6m/d\n" +
                    "8uff61AG5V8Lf6dSaGr5PepX/NKCXu0qGcmVVV20aUfg0obihBre+k/GAj74g29e\n" +
                    "mLyxU0Xb0VRUc9uoTjWClmZeDgdDn6jTGi4QX9jUTuVKWGDb0t2PK2uqTecqEIoA\n" +
                    "ZRKC9Z9CX2y+3UclRbH8I35D+6K40CrOvhPtjrVGQ58IhcKVU93WHnNYUjWQhphV\n" +
                    "Z+UnqGFk6+fLqKcvESaabORWA1bveDHOu2y4/9iruI+MS3fESnXzybdJ2NHtdMU7\n" +
                    "j9OJ7i4l+u8Dmmb0d53haJvzGn0AsYBL42uNX4ILafSb/BRrWBd3EF5QpUm2M69Y\n" +
                    "nccmEQOO/tAsvUuvUOEzqd2qlVjc1dwxvaVuIHMkRnSnzMk=\n" +
                    "-----END CERTIFICATE-----\n";

            InputStream inputStream = new ByteArrayInputStream(certificate.getBytes(StandardCharsets.UTF_8));

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate caCert = (X509Certificate) cf.generateCertificate(inputStream);
            Log.d(TAG, "ca=" + caCert.getSubjectDN());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null); // You don't need the KeyStore instance to come from a file.
            ks.setCertificateEntry("caCert", caCert);
            tmf.init(ks);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            return sslContext;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        throw new Exception("See the exceptions above..");
    }


}
