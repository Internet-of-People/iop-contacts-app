package org.libertaria.world.can;

import org.libertaria.world.can.CanProfile;
import org.libertaria.world.can.impl.CanProfileImp;
import org.libertaria.world.can.impl.Variant;
import org.libertaria.world.can.impl.VariantVisitorImp;
import org.libertaria.world.core.IoPConnectContext;
import org.libertaria.world.core.pure.CryptoWrapperJava;
import org.libertaria.world.crypto.CryptoWrapper;
import org.libertaria.world.profile_server.ProfileServerConfigurations;
import org.libertaria.world.profile_server.SslContextFactory;
import org.libertaria.world.profile_server.utils.ProfileConfigurationImp;
import org.junit.Test;

/**
 * Created by furszy on 5/29/17.
 */

public class CanTest {

    IoPConnectContext ioPConnectContext = new IoPConnectContext() {
        @Override
        public ProfileServerConfigurations createProfSerConfig() {
            return new ProfileConfigurationImp();
        }
    };
    CryptoWrapper cryptoWrapper = new CryptoWrapperJava();
    SslContextFactory sslContextFactory = new org.libertaria.world.profile_server.utils.SslContextFactory();

    @Test
    public void getProfileDateAttributeTest(){
        final long timestamp = System.currentTimeMillis();
        CanProfile.Attribute profileDate = new CanProfileImp.Attribute("timestamp",new Variant.Uint64(timestamp));
        org.libertaria.world.can.VariantVisitor variantVisitor = new VariantVisitorImp() {
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
        /*IoPProfileConnection redtoothProfileConnection = new IoPProfileConnection(ioPConnectContext,,conf,cryptoWrapper,sslContextFactory);
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
