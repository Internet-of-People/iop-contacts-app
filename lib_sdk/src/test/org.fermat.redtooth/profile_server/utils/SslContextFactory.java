package org.libertaria.world.profile_server.utils;

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
 * Created by mati on 09/05/17.
 */

public class SslContextFactory implements org.libertaria.world.profile_server.SslContextFactory {


    @Override
    public SSLContext buildContext() throws Exception {
        try {

            String certificate = "" +
                    "-----BEGIN CERTIFICATE-----\n" +
                    "MIIFXzCCA0egAwIBAgIJAJeA/yrH344xMA0GCSqGSIb3DQEBCwUAMEUxCzAJBgNV\n" +
                    "BAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBX\n" +
                    "aWRnaXRzIFB0eSBMdGQwIBcNMTcwNTE2MTQxODEzWhgPMzAxNjA5MTYxNDE4MTNa\n" +
                    "MEUxCzAJBgNVBAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJ\n" +
                    "bnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
                    "ggIKAoICAQC8aA/p9uD14yril6rdvJ3qfsXMSdYQk6HS5V7HvbUOwJfVSy8N77aV\n" +
                    "VAL2gyd8Xh/w4wJSlJ72P4wDh1kkH21oaRVbihoa+yztmJEBUA2lC0X4EezaFJeJ\n" +
                    "YODLi/j0jxBASsIU+5X12KdeXbF5GIT0WaxwNiC+Qm3KGocWp7nDMw57I8hrCsM2\n" +
                    "RkYkEwYOrGSwYT/p78FyofiDsvWpsoY9mBwsUuoJ2H+b6Z6usJNfxxqEyTALQ4Fq\n" +
                    "aO4o/4pjmiX6oaMNd73JJAj4luwmlhxhcbi/HTYW+a/xEgslq7ZAxJbTrqbn8nE5\n" +
                    "fNLmXpJIPLKsnomrGmv5slB1yYHQdYFDWdRX0n5XMqVxNeOmaU9rjQ4uJSxikypY\n" +
                    "utAhh5QT/rx+JYsFFOXAZrEm5SokygCb4rLRbZGEE7NnnZJUtQBsLNDmMdYPZdel\n" +
                    "bnp3YB50hHZHtimH64KRYCbyfM5GVXPF8nr0HpQIVz8AA/wzetWRY/9W0UWBHNOd\n" +
                    "m0fpzbypatpAWjfQE6lQyJDYFmt/68j0fHR3D8Xou+4wSVY8ZL5/JrnpTFdj/x+A\n" +
                    "ZrNRx0Dg5P+lef5eu3H74eB63qXptDTx6OxG/mFB+0aQqQrj5k079W25fpsx/Pmx\n" +
                    "q29aZtL0s3Qd21wfP4K4DBaRaEO+EDqj5+IDQO94Pdszw9hVZp+DeQIDAQABo1Aw\n" +
                    "TjAdBgNVHQ4EFgQUVuW+GXETmtBeETieZy/bPWjoOrEwHwYDVR0jBBgwFoAUVuW+\n" +
                    "GXETmtBeETieZy/bPWjoOrEwDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQsFAAOC\n" +
                    "AgEAtT6FBPx/IM8h2NsGj0GQ6xqxoNFuQHzdx0XaxagM4IUpHjSymDSSeydZ+Zfu\n" +
                    "EL+oVTxqSeNBxmdQ3gc1gKx2/CIjWxCMkMZl08hx4O3dFIsSg749XNElnMg5CGL2\n" +
                    "FgJtgXbgYg5TI2WF9hrmBGYxqsv6PVncRT9/pCVZfEkZZqrSQ08Kp5WIezuHIV5r\n" +
                    "U1nJs/JhoSFneaZhoW7vSUzPokVnhjj5y6GQOgpBj9qKFK92o5yLCl/sQ48FLeqU\n" +
                    "7oMGDVbECyG5Z2QtNv6trohT6cip+SKSpKt5nFrFB5PaoU7suYE8S83rQbGtxHAH\n" +
                    "Jz7xANuQPNmAF4B7ObbBqS3Kq7o9Yt4XYsFp/DTnWq0HP6Kc1YNHLyRZzWlImBFO\n" +
                    "YIkfIYJh+mGVDUOEXtsrSul40YqKftjvUyg81z42gHPWRA1AdIAsy0KmDm44RUvG\n" +
                    "fiZmFyPGE5LnkZ117K5Mc48tlDWPz1QmvyGRg0SBDY3XHOvzFBMYn8utDHYa8tqU\n" +
                    "gS/KPzAC2/IhGzRFup/mgCdb6940k+z6oTJVUcOG2CI71RHhk2NhUFIDdfXSJPWk\n" +
                    "5/zoHUsVrhuUzm+ydvS1lmp/q8jySWFUYFOmnKA709ecKgUVzWO/7VKBn/ZS8Sb2\n" +
                    "M2u0S06ro1iTvSNIVP+flHzndXcEr+oz3d7Zxjmn2jd4LtI=\n" +
                    "-----END CERTIFICATE-----";

//            String certificate = "-----BEGIN CERTIFICATE-----\n" +
//                    "MIIFtzCCA5+gAwIBAgIJAJsIU2JVlQIbMA0GCSqGSIb3DQEBBQUAMEUxCzAJBgNV\n" +
//                    "BAYTAkFVMRMwEQYDVQQIEwpTb21lLVN0YXRlMSEwHwYDVQQKExhJbnRlcm5ldCBX\n" +
//                    "aWRnaXRzIFB0eSBMdGQwIBcNMTcwNTIyMTgwMDU2WhgPMzAxNjA5MjIxODAwNTZa\n" +
//                    "MEUxCzAJBgNVBAYTAkFVMRMwEQYDVQQIEwpTb21lLVN0YXRlMSEwHwYDVQQKExhJ\n" +
//                    "bnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
//                    "ggIKAoICAQDBy7bjpcimFdYsUPs9PsRc98bqq7Qi90zAgBY5itVVK6jtvQzvph/D\n" +
//                    "StAVebYYPMecoy0SXfCz/Gg0f9M/Nf4ZIFQXAE3GoZPws01Otl3GAucEN2DR83O+\n" +
//                    "CWSqb4FKHQ85wrnwceBlFIOh5hWC7LfIWgpsthKv5qSEIDFQMYPQ838Td09fwjPw\n" +
//                    "pDzQu2npncT5WlGds7nnNqFaZvlQZhzev1GZTEMwIDAejzHPL7HlpHvLvNKePEwF\n" +
//                    "57E1qFzJUb0ttv8N5qQji9EMdEgdfbnDC9KWEzNTavQvQ6bkTChnXhvb+VOWg2Hr\n" +
//                    "U/rMkeYSAvCsFOu2Gvwt0NdJNKupsjM2d41C4Zrk9/tipG7fQ5Ccx7xEG9lwdhQ5\n" +
//                    "sAeCT5p7ckLOKbVxeBjaW+2Wtfc4adHSqJFR9Nxw7SMhfz8H2ZF1PcIUrUjCmTsN\n" +
//                    "MVxKiQZMOZsqedlbjtLDQOxCam6yyf6p+bIrVteo3ibmtEovKhJxUAGzrD81TEm0\n" +
//                    "RoR44/aQqfC1zD5AmQ5lLqk2SUzgF6PaMrZmczu4UCWMYNIV5vZ7lotSBMwq/3cw\n" +
//                    "L6S9Nwb8aWaooxY/qoBB1lpekUc2OQZs+mtI9/KsvnhCIx+yYVkjCRAIAHqjqSq8\n" +
//                    "FsPm6xtYvC3NqwMHMZUpcuQ3g0UvMKm3BSd5pmowv3bLoAvGU6yWVwIDAQABo4Gn\n" +
//                    "MIGkMB0GA1UdDgQWBBRzaDRUcynN81d9h+1Rl+kpFUWoFjB1BgNVHSMEbjBsgBRz\n" +
//                    "aDRUcynN81d9h+1Rl+kpFUWoFqFJpEcwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgT\n" +
//                    "ClNvbWUtU3RhdGUxITAfBgNVBAoTGEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZIIJ\n" +
//                    "AJsIU2JVlQIbMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADggIBACmrKF2b\n" +
//                    "3oDhQJ+h6+kOdNzUloCTXvNxwU7mrPa+dAWTCuZNXKlhqPBhqbj8ksW6eLWVXL+W\n" +
//                    "29P8psrrKS2BIFcHpAX6ZJoL6PFWrv4lqVNHEWeKU/SmZoCjchO62ul3Pd/hA4nA\n" +
//                    "kb9i0t/P0uMiJ0uJFwubyPmEP+IORDOhhAhT5OepkWaRkCpJpySKdnoEreVvLrmU\n" +
//                    "XbPkeZakKC3k8hceJ8C5Sgu9HLY+g7C4WvSiyYvzT55wVRXnDB2IH4JxxZbhUxjB\n" +
//                    "jU1N4o9pS+1JRHvtUzToA3NqXc7CttFyowai7VQ3DzXtIfoCAJ131KAlTGoEcC0i\n" +
//                    "LzXFPekUvxF02rIK91Dk5CoPs+ZJBE0VRGfe6g4CksltYQVd+b8cBJCyqc3GH8Ms\n" +
//                    "O3qM2Oe6o8b33Wzxi4zzG4A/P/YqzQ53GVDNB8a0+fH40jrOWpKSbNjKhxQzgK9k\n" +
//                    "b9l0C+jUTZUcdLULotVn/u+xYunV+jvAWBA8pzXM6jGzDQPjsw9qdlvweIwpccrH\n" +
//                    "zU9cxBSHHHr4Gm0tN7Mt/NQxj+EqyQYhTjnC9wSb3yt8Nab/q9VUCLhHmHpG5QOR\n" +
//                    "3sFJF1Ue62ZWGMAzkUJgnenvshTDyfX0RuxqEnBrxi4QnvTm9NDt1PKNe2GFGEOq\n" +
//                    "86glI9rVQVfaynA764ZbKiIehlF1JaB064Ak\n" +
//                    "-----END CERTIFICATE-----\n";

            InputStream inputStream = new ByteArrayInputStream(certificate.getBytes(StandardCharsets.UTF_8));

//            InputStream inputStream = ((Context)context).getResources().openRawResource(R.raw.profile_server);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate caCert = (X509Certificate) cf.generateCertificate(inputStream);
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
        throw new Exception("See logs above..");
    }
}
