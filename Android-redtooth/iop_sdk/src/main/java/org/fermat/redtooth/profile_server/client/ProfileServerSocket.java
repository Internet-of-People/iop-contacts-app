package org.fermat.redtooth.profile_server.client;


import com.google.protobuf.InvalidProtocolBufferException;

import org.fermat.redtooth.IoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.IoSession;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;

/**
 * Created by mati on 08/11/16.
 */

public class ProfileServerSocket implements IoSession<IopProfileServer.Message> {

    private static final Logger logger = LoggerFactory.getLogger(ProfileServerSocket.class);

    private int port;
    private String host;
    /** Server role type */
    private IopProfileServer.ServerRoleType portType;
    /** Socket factory */
    private SocketFactory socketFactory;
    /** Blocking Socket */
    private Socket socket;
    /** Handler */
    private IoHandler<IopProfileServer.Message> handler;
    /** Reader thread */
    private Thread readThread;

    public ProfileServerSocket(SocketFactory socketFactory, String host, int port,IopProfileServer.ServerRoleType portType) throws Exception {
        this.socketFactory = socketFactory;
        if (port<=0) throw new IllegalArgumentException(portType+" port is 0");
        this.port = port;
        this.host = host;
        this.portType = portType;
    }

    public void connect() throws IOException {
        if ((socket!=null && readThread!=null) && (socket.isConnected() || readThread.isAlive())) throw new IllegalStateException("ProfileServerSocket is running");
        this.socket = socketFactory.createSocket(host,port);
        readThread = new Thread(new Reader());
        readThread.start();
    }

    public void setHandler(IoHandler<IopProfileServer.Message> handler) {
        this.handler = handler;
    }

    public void write(IopProfileServer.Message message) throws CantSendMessageException {
        try {
            int messageSize = message.toByteArray().length;
            IopProfileServer.MessageWithHeader messageWithHeaderBuilder = IopProfileServer.MessageWithHeader.newBuilder()
                    .setHeader(messageSize+computeProtocolOverhead(messageSize))
                    .setBody(message)
                    .build();
            byte[] messageToSend = messageWithHeaderBuilder.toByteArray();
            logger.info("Message lenght to send: "+messageToSend.length+", Message lenght in the header: "+messageWithHeaderBuilder.getHeader());
            socket.getOutputStream().write(messageToSend);
            socket.getOutputStream().flush();
            handler.messageSent(this,message);
//            read();
        }catch (Exception e){
            throw new CantSendMessageException(e);
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

    private synchronized void read(){
        int count;
        byte[] buffer = new byte[8192];
        try {
            // read reply
            count = socket.getInputStream().read(buffer);
            logger.info("Reciving data..");
            IopProfileServer.MessageWithHeader message1 = null;
            if (count>0) {
                ByteBuffer byteBufferToRead = ByteBuffer.allocate(count);
                byteBufferToRead.put(buffer, 0, count);
                message1 = IopProfileServer.MessageWithHeader.parseFrom(byteBufferToRead.array());
                handler.messageReceived(this, message1.getBody());
            }else {
                // todo: ver porqué se vá por acá
            }
        } catch (InvalidProtocolBufferException e) {
//                throw new InvalidProtocolViolation("Invalid message",e);
            e.printStackTrace();
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
        socket.close();
        readThread.interrupt();
    }

    @Override
    public boolean isActive() {
        return socket.isConnected();
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

                for (;;) {
                    if (!socket.isClosed()) {
                        read();
                        TimeUnit.SECONDS.sleep(5);
                    } else {
                        TimeUnit.SECONDS.sleep(3);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
