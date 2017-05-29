package org.fermat.redtooth.profile_server;

import javax.net.ssl.SSLContext;

/**
 * Created by mati on 25/12/16.
 */

public interface SslContextFactory {

    SSLContext buildContext() throws Exception;

}
