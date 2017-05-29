package iop.org.iop_sdk_android.core.profile_server;

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

import iop.org.iop_sdk_android.R;


/**
 * Created by mati on 08/11/16.
 */

public class SslContextFactory implements org.fermat.redtooth.profile_server.SslContextFactory{

    private static final String TAG = "SslContextFactory";

    private final Context context;

    public SslContextFactory(Context context) {
        this.context = context;
    }

    public SSLContext buildContext() throws Exception{
        try {
            //InputStream inputStream = ((Context)context).getResources().openRawResource(R.raw.profile_server);
            String certificate = "-----BEGIN CERTIFICATE-----\n" +
                    "MIIFtzCCA5+gAwIBAgIJAJsIU2JVlQIbMA0GCSqGSIb3DQEBBQUAMEUxCzAJBgNV\n" +
                    "BAYTAkFVMRMwEQYDVQQIEwpTb21lLVN0YXRlMSEwHwYDVQQKExhJbnRlcm5ldCBX\n" +
                    "aWRnaXRzIFB0eSBMdGQwIBcNMTcwNTIyMTgwMDU2WhgPMzAxNjA5MjIxODAwNTZa\n" +
                    "MEUxCzAJBgNVBAYTAkFVMRMwEQYDVQQIEwpTb21lLVN0YXRlMSEwHwYDVQQKExhJ\n" +
                    "bnRlcm5ldCBXaWRnaXRzIFB0eSBMdGQwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAw\n" +
                    "ggIKAoICAQDBy7bjpcimFdYsUPs9PsRc98bqq7Qi90zAgBY5itVVK6jtvQzvph/D\n" +
                    "StAVebYYPMecoy0SXfCz/Gg0f9M/Nf4ZIFQXAE3GoZPws01Otl3GAucEN2DR83O+\n" +
                    "CWSqb4FKHQ85wrnwceBlFIOh5hWC7LfIWgpsthKv5qSEIDFQMYPQ838Td09fwjPw\n" +
                    "pDzQu2npncT5WlGds7nnNqFaZvlQZhzev1GZTEMwIDAejzHPL7HlpHvLvNKePEwF\n" +
                    "57E1qFzJUb0ttv8N5qQji9EMdEgdfbnDC9KWEzNTavQvQ6bkTChnXhvb+VOWg2Hr\n" +
                    "U/rMkeYSAvCsFOu2Gvwt0NdJNKupsjM2d41C4Zrk9/tipG7fQ5Ccx7xEG9lwdhQ5\n" +
                    "sAeCT5p7ckLOKbVxeBjaW+2Wtfc4adHSqJFR9Nxw7SMhfz8H2ZF1PcIUrUjCmTsN\n" +
                    "MVxKiQZMOZsqedlbjtLDQOxCam6yyf6p+bIrVteo3ibmtEovKhJxUAGzrD81TEm0\n" +
                    "RoR44/aQqfC1zD5AmQ5lLqk2SUzgF6PaMrZmczu4UCWMYNIV5vZ7lotSBMwq/3cw\n" +
                    "L6S9Nwb8aWaooxY/qoBB1lpekUc2OQZs+mtI9/KsvnhCIx+yYVkjCRAIAHqjqSq8\n" +
                    "FsPm6xtYvC3NqwMHMZUpcuQ3g0UvMKm3BSd5pmowv3bLoAvGU6yWVwIDAQABo4Gn\n" +
                    "MIGkMB0GA1UdDgQWBBRzaDRUcynN81d9h+1Rl+kpFUWoFjB1BgNVHSMEbjBsgBRz\n" +
                    "aDRUcynN81d9h+1Rl+kpFUWoFqFJpEcwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgT\n" +
                    "ClNvbWUtU3RhdGUxITAfBgNVBAoTGEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZIIJ\n" +
                    "AJsIU2JVlQIbMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADggIBACmrKF2b\n" +
                    "3oDhQJ+h6+kOdNzUloCTXvNxwU7mrPa+dAWTCuZNXKlhqPBhqbj8ksW6eLWVXL+W\n" +
                    "29P8psrrKS2BIFcHpAX6ZJoL6PFWrv4lqVNHEWeKU/SmZoCjchO62ul3Pd/hA4nA\n" +
                    "kb9i0t/P0uMiJ0uJFwubyPmEP+IORDOhhAhT5OepkWaRkCpJpySKdnoEreVvLrmU\n" +
                    "XbPkeZakKC3k8hceJ8C5Sgu9HLY+g7C4WvSiyYvzT55wVRXnDB2IH4JxxZbhUxjB\n" +
                    "jU1N4o9pS+1JRHvtUzToA3NqXc7CttFyowai7VQ3DzXtIfoCAJ131KAlTGoEcC0i\n" +
                    "LzXFPekUvxF02rIK91Dk5CoPs+ZJBE0VRGfe6g4CksltYQVd+b8cBJCyqc3GH8Ms\n" +
                    "O3qM2Oe6o8b33Wzxi4zzG4A/P/YqzQ53GVDNB8a0+fH40jrOWpKSbNjKhxQzgK9k\n" +
                    "b9l0C+jUTZUcdLULotVn/u+xYunV+jvAWBA8pzXM6jGzDQPjsw9qdlvweIwpccrH\n" +
                    "zU9cxBSHHHr4Gm0tN7Mt/NQxj+EqyQYhTjnC9wSb3yt8Nab/q9VUCLhHmHpG5QOR\n" +
                    "3sFJF1Ue62ZWGMAzkUJgnenvshTDyfX0RuxqEnBrxi4QnvTm9NDt1PKNe2GFGEOq\n" +
                    "86glI9rVQVfaynA764ZbKiIehlF1JaB064Ak\n" +
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
