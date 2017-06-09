package org.fermat.redtooth.can;

import org.fermat.redtooth.can.impl.*;
import org.fermat.redtooth.can.impl.Variant;
import org.fermat.redtooth.core.RedtoothContext;
import org.fermat.redtooth.core.RedtoothProfileConnection;
import org.fermat.redtooth.core.pure.CryptoWrapperJava;
import org.fermat.redtooth.crypto.CryptoWrapper;
import org.fermat.redtooth.forum.discourge.com.wareninja.opensource.discourse.utils.WebClient;
import org.fermat.redtooth.profile_server.ProfileServerConfigurations;
import org.fermat.redtooth.profile_server.SslContextFactory;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.utils.ProfileConfigurationImp;
import org.junit.Test;

/**
 * Created by furszy on 5/29/17.
 */

public class CanTest {

    RedtoothContext redtoothContext = new RedtoothContext() {
        @Override
        public ProfileServerConfigurations createProfSerConfig() {
            return new ProfileConfigurationImp();
        }
    };
    CryptoWrapper cryptoWrapper = new CryptoWrapperJava();
    SslContextFactory sslContextFactory = new org.fermat.redtooth.profile_server.utils.SslContextFactory();

    @Test
    public void getProfileDateAttributeTest(){
        final long timestamp = System.currentTimeMillis();
        CanProfile.Attribute profileDate = new CanProfileImp.Attribute("timestamp",new Variant.Uint64(timestamp));
        VariantVisitor variantVisitor = new VariantVisitorImp() {
            @Override
            public void visitLong(long value) {
                assert value==timestamp:"We don't rock!";
            }
        };
        profileDate.getValue().accept(variantVisitor);
    }

    @Test
    public void addIPNSTest() throws Exception {

        ProfileConfigurationImp conf = new ProfileConfigurationImp();
        /*RedtoothProfileConnection redtoothProfileConnection = new RedtoothProfileConnection(redtoothContext,,conf,cryptoWrapper,sslContextFactory);
        redtoothProfileConnection.setProfileName("MatiasCan");
        redtoothProfileConnection.setProfileType("registerCanTest");
        redtoothProfileConnection.init();
        MsgListenerFuture future = new MsgListenerFuture();
        redtoothProfileConnection.init(future);
        future.get();

        if(redtoothProfileConnection.isReady()){

            assert redtoothProfileConnection.isReady();

        }*/


//        WebClient webClient = new WebClient("localhost"+":"+node.getPort());
//        String response = webClient.get("/api/v0/name/resolve?args="+identifier+"&recursive=true&nocache=false");

    }


}
