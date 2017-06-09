/*
package org.fermat.redtooth.profile_server;

import org.fermat.redtooth.core.RedtoothProfileConnection;
import org.fermat.redtooth.core.RedtoothContext;
import org.fermat.redtooth.core.pure.CryptoWrapperJava;
import org.fermat.redtooth.core.pure.KeyEd25519Java;
import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.profile_server.engine.app_services.CallProfileAppService;
import org.fermat.redtooth.profile_server.engine.listeners.EngineListener;
import org.fermat.redtooth.profile_server.engine.SearchProfilesQuery;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.futures.SearchMessageFuture;
import org.fermat.redtooth.profile_server.model.KeyEd25519;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;
import org.fermat.redtooth.profile_server.utils.*;
import org.fermat.redtooth.profile_server.utils.SslContextFactory;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

*
 * Created by mati on 09/05/17.



public class ProfileTest {

    RedtoothProfileConnection redtoothProfileConnection;
    ProfileServerConfigurations conf;

    @Before
    public void setup() throws Exception {

        RedtoothContext context = new RedtoothContext() {
            @Override
            public ProfileServerConfigurations createProfSerConfig() {
                return null;
            }
        };
        conf = new ProfileConfigurationImp();
        redtoothProfileConnection = new RedtoothProfileConnection(context,conf,new CryptoWrapperJava(),new SslContextFactory());

    }


    @Test
    public void registerProfileTest() throws Exception {

        final Logger logger = LoggerFactory.getLogger("registerProfileTest");

        redtoothProfileConnection.setProfileName("pepe");
        redtoothProfileConnection.init();

        redtoothProfileConnection.setProfServerEngineListener(new EngineListener() {

            @Override
            public void newCallReceived(CallProfileAppService callProfileAppService) {

            }
        });

        // wait until the profile server is connected
        waitUntilProfileServerIsConnected();

        MsgListenerFuture msgListenerFuture = new MsgListenerFuture();
        redtoothProfileConnection.updateProfile("pepe",null,null,msgListenerFuture);
        msgListenerFuture.get();

        assert msgListenerFuture.getStatus()==200;

    }

    @Test
    public void addApplicationServiceTest() throws Exception {

        final Logger logger = LoggerFactory.getLogger("addApplicationServiceTest");

        redtoothProfileConnection.setProfileName("Mati_1495076957266");
        KeyEd25519 keyEd25519 = KeyEd25519Java.wrap(
                CryptoBytes.fromHexToBytes("6d0596ce8503cfb6c514c0df504dce319c221fd2bf30fed67ac25f8ff8529da5"), // private key
                CryptoBytes.fromHexToBytes("e4a38df7ab806749d29b0ac901b3fc84517fd0bbd0134d0fc7a7c141584f29c9") // public key
        );

        conf.setIsCreated(true);
        conf.setMainPsPrimaryPort(16987);
        conf.setMainPsNonClPort(16988);
        conf.setMainPfClPort(16988);
        conf.setIsRegistered(true);
        redtoothProfileConnection.setProfileKeys(keyEd25519);
        redtoothProfileConnection.getProfileServerConfigurations().setProfileType("chat");
        redtoothProfileConnection.init();

        redtoothProfileConnection.setProfServerEngineListener(new EngineListener() {

            @Override
            public void newCallReceived(CallProfileAppService callProfileAppService) {

            }
        });

        // wait until the profile server is connected
        waitUntilProfileServerIsConnected();

        MsgListenerFuture msgListenerFuture = new MsgListenerFuture();
        redtoothProfileConnection.addApplicationService("chat",msgListenerFuture);
        msgListenerFuture.get();

        assert msgListenerFuture.getStatus()==200:"chat application services added";

        // wait unninterrumply
        for (;;){

            TimeUnit.SECONDS.sleep(3);

        }

    }

    @Test
    public void searchProfileTest() throws Exception {

        final Logger logger = LoggerFactory.getLogger("getIdentityInformationTest");

        redtoothProfileConnection.setProfileName("Mati_"+System.currentTimeMillis());
        redtoothProfileConnection.init();

        redtoothProfileConnection.setProfServerEngineListener(new EngineListener() {

            @Override
            public void newCallReceived(CallProfileAppService callProfileAppService) {

            }
        });

        // wait until the profile server is connected
        waitUntilProfileServerIsConnected();

        // search profile for type
        // todo: preguntar porqué no se puede buscar el profile por app service..
        SearchProfilesQuery searchProfilesQuery = new SearchProfilesQuery();
        searchProfilesQuery.setProfileType("test");
        SearchMessageFuture<List<IopProfileServer.ProfileQueryInformation>> future = redtoothProfileConnection.searchProfiles(searchProfilesQuery);
        List<IopProfileServer.ProfileQueryInformation> list = future.get();

        if (!list.isEmpty()){
            for (IopProfileServer.ProfileQueryInformation profileQueryInformation : list) {
                logger.info("profile query info: "+profileQueryInformation);
            }
        }
    }

*
     * todo: this test fail,  response: to msg id: 195936478 status: ERROR_PROTOCOL_VIOLATION el id que vuelve es cualquier cosa y viene sin nada
     * @throws Exception


    @Test
    public void getRegisteredProfileInformationTest() throws Exception {

        final Logger logger = LoggerFactory.getLogger("getIdentityInformationTest");

        redtoothProfileConnection.setProfileName("Mati_1495076957266");
        KeyEd25519 keyEd25519 = KeyEd25519Java.wrap(
                CryptoBytes.fromHexToBytes("6d0596ce8503cfb6c514c0df504dce319c221fd2bf30fed67ac25f8ff8529da5"), // private key
                CryptoBytes.fromHexToBytes("e4a38df7ab806749d29b0ac901b3fc84517fd0bbd0134d0fc7a7c141584f29c9") // public key
        );

        conf.setIsCreated(true);
        conf.setMainPsPrimaryPort(16987);
        conf.setMainPsNonClPort(16988);
        conf.setMainPfClPort(16988);
        conf.setIsRegistered(true);
        redtoothProfileConnection.setProfileKeys(keyEd25519);
        redtoothProfileConnection.getProfileServerConfigurations().setProfileType("chat");
        redtoothProfileConnection.init();

        redtoothProfileConnection.setProfServerEngineListener(new EngineListener() {
            @Override
            public void newCallReceived(CallProfileAppService callProfileAppService) {

            }
        });

        // wait until the profile server is connected
        waitUntilProfileServerIsConnected();

        MsgListenerFuture msgListenerFuture = new MsgListenerFuture();
        redtoothProfileConnection.addApplicationService("chat",msgListenerFuture);
        msgListenerFuture.get();

        assert msgListenerFuture.getStatus()==200:"chat application services added";

        // get profile

        MsgListenerFuture<IopProfileServer.GetProfileInformationResponse> msgProfFuture = new MsgListenerFuture();
        msgProfFuture.setMsgName("getProfileInformation");

        redtoothProfileConnection.getProfileInformation("e4a38df7ab806749d29b0ac901b3fc84517fd0bbd0134d0fc7a7c141584f29c9",true,msgProfFuture);
        IopProfileServer.GetProfileInformationResponse profileInformation = msgProfFuture.get();

        logger.info("profile found: "+profileInformation.toString());


        assert CryptoBytes.toHexString(profileInformation.getSignedProfile().getProfile().getPublicKey().toByteArray()).equals("e4a38df7ab806749d29b0ac901b3fc84517fd0bbd0134d0fc7a7c141584f29c9");

    }


    @Test
    public void onlineProfileWaitingForIncomingNotificationsTest() throws Exception {

        final Logger logger = LoggerFactory.getLogger("onlineProfileWaitingForIncomingNotificationsTest");

        redtoothProfileConnection.setProfileName("Mati_1495076957266");
        KeyEd25519 keyEd25519 = KeyEd25519Java.wrap(
                CryptoBytes.fromHexToBytes("6d0596ce8503cfb6c514c0df504dce319c221fd2bf30fed67ac25f8ff8529da5"), // private key
                CryptoBytes.fromHexToBytes("e4a38df7ab806749d29b0ac901b3fc84517fd0bbd0134d0fc7a7c141584f29c9") // public key
        );

        conf.setIsCreated(true);
        conf.setMainPsPrimaryPort(16987);
        conf.setMainPsNonClPort(16988);
        conf.setMainPfClPort(16988);
        conf.setIsRegistered(true);
        redtoothProfileConnection.setProfileKeys(keyEd25519);
        redtoothProfileConnection.getProfileServerConfigurations().setProfileType("chat");
        redtoothProfileConnection.init();

        redtoothProfileConnection.setProfServerEngineListener(new EngineListener() {
            @Override
            public void newCallReceived(CallProfileAppService callProfileAppService) {

            }
        });

        // wait until the profile server is connected
        waitUntilProfileServerIsConnected();

        MsgListenerFuture msgListenerFuture = new MsgListenerFuture();
        redtoothProfileConnection.addApplicationService("chat",msgListenerFuture);
        msgListenerFuture.get();

        assert msgListenerFuture.getStatus()==200:"chat application services added";



        // wait unninterrumply
        for (;;){

            TimeUnit.SECONDS.sleep(3);

        }

    }

    @Test
    public void callIdentityApplicationService() throws Exception {
        final Logger logger = LoggerFactory.getLogger("getIdentityInformationTest");

        redtoothProfileConnection.setProfileName("Mati_1495076957266");
        KeyEd25519 keyEd25519 = KeyEd25519Java.wrap(
                CryptoBytes.fromHexToBytes("c760a122337df6ee22c23e8cddb64303a6f7894c1f1d02efab22ae0af4063089"), // private key
                CryptoBytes.fromHexToBytes("47aedf4685288670b13e573c15c08d9f0ecbb4f8b7049d3af937b13fc5263030") // public key
        );

        conf.setIsCreated(true);
        conf.setMainPsPrimaryPort(16987);
        conf.setMainPsNonClPort(16988);
        conf.setMainPfClPort(16988);
        conf.setIsRegistered(true);
        redtoothProfileConnection.setProfileKeys(keyEd25519);
        redtoothProfileConnection.getProfileServerConfigurations().setProfileType("chat");
        redtoothProfileConnection.init();

        redtoothProfileConnection.setProfServerEngineListener(new EngineListener() {
            @Override
            public void newCallReceived(CallProfileAppService callProfileAppService) {

            }
        });

        // wait until the profile server is connected
        waitUntilProfileServerIsConnected();

        // return an object call
        MsgListenerFuture<CallProfileAppService> msgListenerFuture = new MsgListenerFuture();
        redtoothProfileConnection.callProfileAppService("e4a38df7ab806749d29b0ac901b3fc84517fd0bbd0134d0fc7a7c141584f29c9","chat",false,msgListenerFuture);
        CallProfileAppService callProfileAppService = msgListenerFuture.get();

        assert callProfileAppService.getStatus() == CallProfileAppService.Status.CALL_AS_ESTABLISH;

    }

*
     * todo: Acá deberia hardcodear 2 identitdades, como si ya hubiera hecho el search y ya tengo la información básica.
     * todo: luego tengo que hacer el getProfileInformationRequest y chequear que tenga los mismos appServices que el perfil 1.
     * @throws Exception


    @Test
    public void getProfileInformationTest() throws Exception {

        final Logger logger = LoggerFactory.getLogger("getIdentityInformationTest");

        redtoothProfileConnection.setProfileName("Mati_1495076957266");
        KeyEd25519 keyEd25519 = KeyEd25519Java.wrap(
                CryptoBytes.fromHexToBytes("6d0596ce8503cfb6c514c0df504dce319c221fd2bf30fed67ac25f8ff8529da5"), // private key
                CryptoBytes.fromHexToBytes("e4a38df7ab806749d29b0ac901b3fc84517fd0bbd0134d0fc7a7c141584f29c9") // public key
        );

        conf.setIsCreated(true);
        conf.setMainPsPrimaryPort(16987);
        conf.setMainPsNonClPort(16988);
        conf.setMainPfClPort(16988);
        conf.setIsRegistered(true);
        redtoothProfileConnection.setProfileKeys(keyEd25519);
        redtoothProfileConnection.getProfileServerConfigurations().setProfileType("chat");
        redtoothProfileConnection.init();

        redtoothProfileConnection.setProfServerEngineListener(new EngineListener() {
            @Override
            public void newCallReceived(CallProfileAppService callProfileAppService) {

            }
        });

        // wait until the profile server is connected
        waitUntilProfileServerIsConnected();
        // add application service
        MsgListenerFuture msgListenerFuture = new MsgListenerFuture();
        redtoothProfileConnection.addApplicationService("chat_service",msgListenerFuture);
        msgListenerFuture.get();

        if (msgListenerFuture.getStatus()==200){

            IopProfileServer.ProfileInformation profileToTalk = null;

            // search for profile to talk
            SearchProfilesQuery searchProfilesQuery = new SearchProfilesQuery();
            searchProfilesQuery.setProfileName("pepe");
            SearchMessageFuture<List<IopProfileServer.ProfileQueryInformation>> searchMessageFuture = redtoothProfileConnection.searchProfiles(searchProfilesQuery);
            List<IopProfileServer.ProfileQueryInformation> list = searchMessageFuture.get();

            if (list!=null && list.isEmpty()){
                for (IopProfileServer.ProfileQueryInformation profileQueryInformation : list) {
                    if (profileQueryInformation.getSignedProfile().getProfile().getPublicKey().equals("80a22d2f54ff42d9d5737f872956a7e21d1cdc58f9490813f67bbc64998d6afa")){
                        logger.info("pepe encontrado!");
                        profileToTalk = profileQueryInformation.getSignedProfile().getProfile();
                    }
                }
            }

            // get App services information
            if (profileToTalk!=null){

                MsgListenerFuture<IopProfileServer.GetProfileInformationResponse> msgProfFuture = new MsgListenerFuture();
                redtoothProfileConnection.getProfileInformation("80a22d2f54ff42d9d5737f872956a7e21d1cdc58f9490813f67bbc64998d6afa",true,msgProfFuture);
                IopProfileServer.GetProfileInformationResponse profileInformation = msgProfFuture.get();

            }

        }else {
            assert false:" add application service fail, "+msgListenerFuture.getStatusDetail();
        }


    }

    private void waitUntilProfileServerIsConnected(RedtoothProfileConnection redtoothProfileConnection) {
        while (!redtoothProfileConnection.isReady()){
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void waitUntilProfileServerIsConnected() {
        while (!redtoothProfileConnection.isReady()){
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}
*/
