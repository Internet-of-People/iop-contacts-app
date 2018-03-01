package iop.org.iop_sdk_android.core.service;

import android.content.Context;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


/**
 * Created by mati on 08/11/16.
 */

public class SslContextFactory implements org.libertaria.world.profile_server.SslContextFactory {

    private static final String TAG = "SslContextFactory";

    private final Context context;

    public SslContextFactory(Context context) {
        this.context = context;
    }

    public SSLContext buildContext() throws Exception {
        //TODO: Right now it's trusting ALL the certs which is NOT OK. REVIEW THIS!
        final TrustManager[] tmf = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf, null);
        return sslContext;
    }


}
