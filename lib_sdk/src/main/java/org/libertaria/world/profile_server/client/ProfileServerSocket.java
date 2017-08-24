package org.libertaria.world.profile_server.client;


import com.google.protobuf.InvalidProtocolBufferException;
import com.sun.org.apache.xpath.internal.operations.Bool;

import org.libertaria.world.profile_server.CantSendMessageException;
import org.libertaria.world.profile_server.IoSession;
import org.libertaria.world.profile_server.protocol.IopProfileServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

/**
 * Created by mati on 08/11/16.
 */

public class ProfileServerSocket implements IoSession<IopProfileServer.Message> {

    private static final Logger logger = LoggerFactory.getLogger(ProfileServerSocket.class);
    /** socket id */
    private String tokenId;
    private int port;
    private String host;
    /** Server role type */
    private IopProfileServer.ServerRoleType portType;
    /** Socket factory */
    private SocketFactory socketFactory;
    /** Blocking Socket */
    private Socket socket;
    /** Handler */
    private PsSocketHandler<IopProfileServer.Message> handler;
    /** Reader thread */
    private Thread readThread;
    private ExecutorService executorService;

    public ProfileServerSocket(SocketFactory socketFactory, String host, int port,IopProfileServer.ServerRoleType portType) throws Exception {
        this.socketFactory = socketFactory;
        if (port<=0) throw new IllegalArgumentException(portType+" port is 0");
        this.port = port;
        this.host = host;
        this.portType = portType;
        this.executorService = Executors.newFixedThreadPool(3);
    }

    public ProfileServerSocket(SocketFactory socketFactory, String host, int port,IopProfileServer.ServerRoleType portType,String tokenId) throws Exception {
        this(socketFactory,host,port,portType);
        this.tokenId = tokenId;
    }

    public void connect() throws IOException {
        if ((socket!=null && readThread!=null) && (socket.isConnected() || readThread.isAlive())) throw new IllegalStateException("ProfileServerSocket is running");
        logger.info("connect: "+host+", port "+port);
        this.socket = socketFactory.createSocket(host,port);
        readThread = new Thread(new Reader(),"Thread-reader-host-"+host+"-port-"+port);
        readThread.start();
        handler.portStarted(portType);
    }

    public void setHandler(PsSocketHandler<IopProfileServer.Message> handler) {
        this.handler = handler;
    }

    @Override
    public String getSessionTokenId() {
        return tokenId;
    }

    public void write(IopProfileServer.Message message) throws CantSendMessageException {
        try {
            int messageSize = message.toByteArray().length;
            IopProfileServer.MessageWithHeader messageWithHeaderBuilder = IopProfileServer.MessageWithHeader.newBuilder()
                    .setHeader(messageSize+computeProtocolOverhead(messageSize))
                    .setBody(message)
                    .build();
            final byte[] messageToSend = messageWithHeaderBuilder.toByteArray();
            logger.info("Message "+message.getId()+" lenght to send: "+messageToSend.length+", Message lenght in the header: "+messageWithHeaderBuilder.getHeader());
            // timeout
            final Future<Boolean> future = executorService.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try {
                        socket.getOutputStream().write(messageToSend);
                        socket.getOutputStream().flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                    return true;
                }
            });
            boolean ret = future.get(15,TimeUnit.SECONDS);
            if (ret) {
                logger.info("message sent: " + message.getId());
                handler.messageSent(this, message);
            }else {
                checkSocket();
            }
        }catch (Exception e){
            e.printStackTrace();
            checkSocket();
            throw new CantSendMessageException(e);
        }
    }

    private void checkSocket(){
        try {
            if (socket.isClosed()) {
                closeNow();
            }
        }catch (Exception e1){
            e1.printStackTrace();
        }
    }

    private int computeProtocolOverhead(int lenght){
        if (lenght<0) throw new IllegalArgumentException("lenght < 0");
        int overhead = 0;
        if (lenght<=127){
            // 1 byte overhead + 1 byte type
            overhead = 2;
        }else if (lenght<=16383){
            // 2 byte  overhead + 1 byte type
            overhead = 3;
        } else{
            // 3 byte overhead + 1 byte type
            overhead = 4;
        }
        return overhead;
    }

    private synchronized void read() {
        int count;
        byte[] buffer = new byte[8192];
        try {
            // read reply
            if (!socket.isInputShutdown()) {
                count = socket.getInputStream().read(buffer);
                logger.info("Reciving data..");
                IopProfileServer.MessageWithHeader message1 = null;
                if (count > 0) {
                    ByteBuffer byteBufferToRead = ByteBuffer.allocate(count);
                    byteBufferToRead.put(buffer, 0, count);
                    message1 = IopProfileServer.MessageWithHeader.parseFrom(byteBufferToRead.array());
                    handler.messageReceived(this, message1.getBody());
                } else {
                    // read < 0 -> connection closed
                    logger.info("Connection closed, read<0 with portType: " + portType +" , read: "+count+ " , removing socket");
                    closeNow();
                }
            } else {
                // input stream closed
                logger.info("Connection closed, input stream shutdown with portType: " + portType + " , removing socket");
                closeNow();
            }
        } catch (InvalidProtocolBufferException e) {
//                throw new InvalidProtocolViolation("Invalid message",e);
            e.printStackTrace();
        } catch (javax.net.ssl.SSLException e) {
            e.printStackTrace();
            // something bad happen..
            logger.info("Connection closed, sslException with portType: " + portType + " , " + tokenId + " removing socket");
            try {
                closeNow();
            } catch (IOException e1) {
                // nothing..
            }
        } catch (SocketException e){
            e.printStackTrace();
            logger.info("Connection closed, sslException with portType: " + portType + " , " + tokenId + " removing socket");
            try {
                closeNow();
            } catch (IOException e1) {
                // nothing..
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public IopProfileServer.ServerRoleType getPortType() {
        return portType;
    }

    @Override
    public void closeNow() throws IOException {
        logger.info("Closing socket port: "+portType);
        if (!readThread.isInterrupted())
            readThread.interrupt();
        if (executorService!=null){
            executorService.shutdownNow();
            executorService = null;
        }
        if (!socket.isClosed())
            socket.close();
        // notify upper layers
        if (handler!=null){
            try {
                handler.sessionClosed(this);
            } catch (Exception e) {
                // swallow
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isActive() {
        if (socket==null) throw new ConnectionException("socket null for some reason.., "+port);
        return !socket.isClosed();
    }

    @Override
    public boolean isConnected() {
        return socket.isConnected();
    }

    @Override
    public Socket getChannel() {
        return socket;
    }

    @Override
    public boolean isReadSuspended() {
        return false;
    }

    @Override
    public boolean isWriteSuspended() {
        return false;
    }


    private class Reader implements Runnable{

        @Override
        public void run() {
            try {
                logger.info("Reader started for: "+port);

                for (;;) {
                    if (!socket.isClosed()) {
                        read();

                        if (!Thread.interrupted()) {
                            TimeUnit.SECONDS.sleep(1);
                        }
                    }else {
                        if (!Thread.currentThread().isInterrupted())
                            Thread.currentThread().interrupt();
                    }
                }
            } catch (InterruptedException e){
                // this happen when the thread is sleep and someone interrupt it.
                logger.info("InterruptedException on port: "+port);
                try {
                    closeNow();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (Exception e){
                logger.info("Exception on port: "+port);
                e.printStackTrace();
                try {
                    closeNow();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @Override
    public String toString() {
        return "ProfileServerSocket{" +
                "tokenId='" + tokenId + '\'' +
                ", port=" + port +
                ", host='" + host + '\'' +
                ", portType=" + portType +
                '}';
    }
}
