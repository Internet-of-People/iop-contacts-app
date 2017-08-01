/*
package org.libertaria.world.profile_server;

import com.google.protobuf.ByteString;

import org.libertaria.world.core.RedtoothContext;
import org.libertaria.world.core.RedtoothProfileConnection;
import CryptoWrapperJava;
import KeyEd25519Java;
import CryptoBytes;
import CallProfileAppService;
import EngineListener;
import BaseMsgFuture;
import MsgListenerFuture;
import KeyEd25519;
import org.libertaria.world.profile_server.utils.*;
import org.libertaria.world.profile_server.utils.SslContextFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

*
 * Created by furszy on 5/23/17.



public class AppServiceCallsTest {


    RedtoothContext context = new RedtoothContext() {
        @Override
        public ProfileServerConfigurations createProfSerConfig() {
            return new ProfileConfigurationImp();
        }
    };

    @Test
    public void establishCallTest() throws Exception {

        final Logger logger = LoggerFactory.getLogger("establishCallTest");

        // threads for two profiles
        ExecutorService executorService = Executors.newFixedThreadPool(1);

        // run first profile
        Future<Boolean> future = executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // Profile connected waiting for call
                ProfileConfigurationImp confProf1 = new ProfileConfigurationImp();
                RedtoothProfileConnection redtoothConnectionProf1 = new RedtoothProfileConnection(context,confProf1,new CryptoWrapperJava(),new org.libertaria.world.profile_server.utils.SslContextFactory());

                redtoothConnectionProf1.setProfileName("Mati_1495076957266");
                KeyEd25519 keyEd25519 = KeyEd25519Java.wrap(
                        CryptoBytes.fromHexToBytes("6d0596ce8503cfb6c514c0df504dce319c221fd2bf30fed67ac25f8ff8529da5"), // private key
                        CryptoBytes.fromHexToBytes("e4a38df7ab806749d29b0ac901b3fc84517fd0bbd0134d0fc7a7c141584f29c9") // public key
                );

                confProf1.setIsCreated(true);
                confProf1.setMainPsPrimaryPort(16987);
                confProf1.setMainPsNonClPort(16988);
                confProf1.setMainPfClPort(16988);
                confProf1.setMainAppServicePort(16988);
                confProf1.setIsRegistered(true);
                redtoothConnectionProf1.setProfileKeys(keyEd25519);
                redtoothConnectionProf1.getProfileServerConfigurations().setProfileType("chat");
                MsgListenerFuture<Boolean> initFuture = new MsgListenerFuture<Boolean>();
                redtoothConnectionProf1.init(initFuture);

                redtoothConnectionProf1.setProfServerEngineListener(new EngineListener() {

                    @Override
                    public void newCallReceived(CallProfileAppService callProfileAppService) {
                        callProfileAppService.setMsgListener(new CallProfileAppService.CallMessagesListener() {
                            @Override
                            public void onMessage(byte[] msg) {
                                logger.info("Receiver call listener  Message arrive -> " + ByteString.copyFrom(msg).toStringUtf8());
                            }
                        });
                    }
                });

                initFuture.get();

                MsgListenerFuture msgListenerFuture = new MsgListenerFuture();
                redtoothConnectionProf1.addApplicationService("chat",msgListenerFuture);
                msgListenerFuture.get();

                return msgListenerFuture.getStatus()==200;

            }
        });
        while (!future.isDone()){

        }
        if (future.get()) {

            // profile 2 call profile 1
            ProfileConfigurationImp conf = new ProfileConfigurationImp();
            RedtoothProfileConnection redtoothProfileConnection = new RedtoothProfileConnection(context,conf,new CryptoWrapperJava(),new org.libertaria.world.profile_server.utils.SslContextFactory());
            redtoothProfileConnection.setProfileName("Mati_1495076957266");
            KeyEd25519 keyEd25519 = KeyEd25519Java.wrap(
                    CryptoBytes.fromHexToBytes("c760a122337df6ee22c23e8cddb64303a6f7894c1f1d02efab22ae0af4063089"), // private key
                    CryptoBytes.fromHexToBytes("47aedf4685288670b13e573c15c08d9f0ecbb4f8b7049d3af937b13fc5263030") // public key
            );
            conf.setIsCreated(true);
            conf.setMainPsPrimaryPort(16987);
            conf.setMainPsNonClPort(16988);
            conf.setMainPfClPort(16988);
            conf.setMainAppServicePort(16988);
            conf.setIsRegistered(true);
            redtoothProfileConnection.setProfileKeys(keyEd25519);
            redtoothProfileConnection.getProfileServerConfigurations().setProfileType("chat");
            MsgListenerFuture<Boolean> initFuture = new MsgListenerFuture<>();
            redtoothProfileConnection.init(initFuture);

            // wait until the profile server is connected
            initFuture.get();
            // return an object call
            MsgListenerFuture<CallProfileAppService> msgListenerFuture = new MsgListenerFuture();
            redtoothProfileConnection.callProfileAppService("e4a38df7ab806749d29b0ac901b3fc84517fd0bbd0134d0fc7a7c141584f29c9", "chat", false, msgListenerFuture);
            CallProfileAppService callProfileAppService = msgListenerFuture.get();

            assert callProfileAppService.getStatus() == CallProfileAppService.Status.CALL_AS_ESTABLISH:"Call stablished succesfully";
        }
    }


    @Test
    public void sendMsgInActiveAppServiceCallTest() throws Exception {
        final Logger logger = LoggerFactory.getLogger("sendMsgInActiveAppServiceCallTest");

        // threads for two profiles
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // run first profile
        Future<Boolean> future = executorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // Profile connected waiting for call
                RedtoothContext context = new RedtoothContext() {
                    @Override
                    public ProfileServerConfigurations createProfSerConfig() {
                        return new ProfileConfigurationImp();
                    }
                };
                ProfileConfigurationImp confProf1 = new ProfileConfigurationImp();
                RedtoothProfileConnection redtoothConnectionProf1 = new RedtoothProfileConnection(context,confProf1,new CryptoWrapperJava(),new SslContextFactory());

                redtoothConnectionProf1.setProfileName("Mati_1495076957266");
                KeyEd25519 keyEd25519 = KeyEd25519Java.wrap(
                        CryptoBytes.fromHexToBytes("6d0596ce8503cfb6c514c0df504dce319c221fd2bf30fed67ac25f8ff8529da5"), // private key
                        CryptoBytes.fromHexToBytes("e4a38df7ab806749d29b0ac901b3fc84517fd0bbd0134d0fc7a7c141584f29c9") // public key
                );

                confProf1.setIsCreated(true);
                confProf1.setMainPsPrimaryPort(16987);
                confProf1.setMainPsNonClPort(16988);
                confProf1.setMainPfClPort(16988);
                confProf1.setMainAppServicePort(16988);
                confProf1.setIsRegistered(true);
                redtoothConnectionProf1.setProfileKeys(keyEd25519);
                redtoothConnectionProf1.getProfileServerConfigurations().setProfileType("chat");
                MsgListenerFuture<Boolean> initFuture = new MsgListenerFuture<Boolean>();
                redtoothConnectionProf1.init(initFuture);
                redtoothConnectionProf1.setProfServerEngineListener(new EngineListener() {

                    @Override
                    public void newCallReceived(CallProfileAppService callProfileAppService) {
                        try {
                            logger.info("new call received");

                            callProfileAppService.setMsgListener(new CallProfileAppService.CallMessagesListener() {
                                @Override
                                public void onMessage(byte[] msg) {
                                    logger.info("Receiver call listener  Message arrive -> " + ByteString.copyFrom(msg).toStringUtf8());
                                }
                            });

                            for (int i=0;i<100000;i++){

                            }
                            MsgListenerFuture<Boolean> sendFuture = new MsgListenerFuture<>();
                            callProfileAppService.sendMsgStr("Hello mate!", sendFuture);
                        } catch (CantSendMessageException e) {
                            e.printStackTrace();
                        } catch (CantConnectException e) {
                            e.printStackTrace();
                        }
                    }
                });

                // wait until the profile server is connected
                initFuture.get();

                MsgListenerFuture msgListenerFuture = new MsgListenerFuture();
                redtoothConnectionProf1.addApplicationService("chat",msgListenerFuture);
                msgListenerFuture.get();

                return msgListenerFuture.getStatus()==200;

            }
        });
        while (!future.isDone()){

        }
        if (future.get()) {

            // profile 2 call profile 1
            ProfileConfigurationImp conf = new ProfileConfigurationImp();
            RedtoothProfileConnection redtoothProfileConnection = new RedtoothProfileConnection(context,conf,new CryptoWrapperJava(),new org.libertaria.world.profile_server.utils.SslContextFactory());
            redtoothProfileConnection.setProfileName("Mati_1495076957266");
            KeyEd25519 keyEd25519 = KeyEd25519Java.wrap(
                    CryptoBytes.fromHexToBytes("c760a122337df6ee22c23e8cddb64303a6f7894c1f1d02efab22ae0af4063089"), // private key
                    CryptoBytes.fromHexToBytes("47aedf4685288670b13e573c15c08d9f0ecbb4f8b7049d3af937b13fc5263030") // public key
            );

            conf.setIsCreated(true);
            conf.setMainPsPrimaryPort(16987);
            conf.setMainPsNonClPort(16988);
            conf.setMainPfClPort(16988);
            conf.setMainAppServicePort(16988);
            conf.setIsRegistered(true);
            redtoothProfileConnection.setProfileKeys(keyEd25519);
            redtoothProfileConnection.getProfileServerConfigurations().setProfileType("chat");
            MsgListenerFuture<Boolean> initFuture = new MsgListenerFuture<>();
            redtoothProfileConnection.init(initFuture);

            redtoothProfileConnection.setProfServerEngineListener(new EngineListener() {
                @Override
                public void newCallReceived(CallProfileAppService callProfileAppService) {

                }
            });

            // wait until the profile server is connected
            initFuture.get();

            // return an object call
            MsgListenerFuture<CallProfileAppService> msgListenerFuture = new MsgListenerFuture();
            redtoothProfileConnection.callProfileAppService("e4a38df7ab806749d29b0ac901b3fc84517fd0bbd0134d0fc7a7c141584f29c9", "chat", false, msgListenerFuture);
            CallProfileAppService callProfileAppService = msgListenerFuture.get();
            callProfileAppService.setMsgListener(new CallProfileAppService.CallMessagesListener() {
                @Override
                public void onMessage(byte[] msg) {
                    logger.info("Sender call listener  Message arrive -> " + ByteString.copyFrom(msg).toStringUtf8());
                }
            });

            assert callProfileAppService.getStatus() == CallProfileAppService.Status.CALL_AS_ESTABLISH:"Call stablished succesfully";

            // now i send "hello"
            MsgListenerFuture<Boolean> sendHiFuture = new MsgListenerFuture<>();
            sendHiFuture.setListener(new BaseMsgFuture.Listener() {
                @Override
                public void onAction(int messageId, Object object) {
                    logger.info("Sender message sent");
                }

                @Override
                public void onFail(int messageId, int status, String statusDetail) {

                }
            });
            callProfileAppService.sendMsgStr("Hello",sendHiFuture);
            sendHiFuture.get();

            assert sendHiFuture.getStatus()==200:"test pass successfully!";

        }

    }


}
*/
