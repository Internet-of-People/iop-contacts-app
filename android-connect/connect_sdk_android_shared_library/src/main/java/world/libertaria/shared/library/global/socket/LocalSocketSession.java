package world.libertaria.shared.library.global.socket;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import com.google.protobuf.InvalidProtocolBufferException;

import org.libertaria.world.profile_server.CantSendMessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;

import world.libertaria.shared.library.global.ModuleObject;

/**
 * Created by furszy on 7/28/17.
 * todo: improve the thread with an executor..
 */

public class LocalSocketSession {

    private Logger logger = LoggerFactory.getLogger(LocalSocketSession.class);

    public static final int RECEIVE_BUFFER_SIZE = 50000;

    /** Client id */
    private String pkIdentity;
    /** Connect service */
    private String serverName;
    /** */
    private LocalSocket localSocket;
    /** Reader thread */
    private Thread readThread;
    /***/
    private SessionHandler sessionHandler;
    private boolean isServerSide = false;

    public LocalSocketSession(String serverName,String pkIdentity,LocalSocket localSocket,SessionHandler sessionHandler) {
        this.pkIdentity = pkIdentity;
        this.localSocket = localSocket;
        this.serverName = serverName;
        this.sessionHandler = sessionHandler;
    }

    public LocalSocketSession(String serverName,String pkIdentity,LocalSocket localSocket,SessionHandler sessionHandler,boolean isServerSide) {
        this(serverName,pkIdentity,localSocket,sessionHandler);
        this.isServerSide = isServerSide;
    }

    /**
     * Connect method for clients
     */
    public void connect() throws IOException {
        if (localSocket!=null){
            if (!localSocket.isConnected()){
                logger.info("trying to connect local socket..");
                localSocket.connect(new LocalSocketAddress(serverName));
                localSocket.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
                localSocket.setSoTimeout(0);
            }
            startReceiver();
        }
    }

    public void startReceiver(){
        readThread = new Thread(new SessionRunner());
        readThread.start();
    }

    public void write(ModuleObject.ModuleResponse message) throws CantSendMessageException {
        try {
            int messageSize = message.toByteArray().length;
            byte[] messageToSend = message.toByteArray();
            logger.info("Message lenght to send: "+messageSize);
            localSocket.getOutputStream().write(messageToSend);
            localSocket.getOutputStream().flush();
            logger.info("message sent");
            //sessionHandler.messageSent(this,message);
        }catch (Exception e){
            try {
                if (!localSocket.isConnected()) {
                    closeNow();
                }
            }catch (Exception e1){
                e.printStackTrace();
            }
            throw new CantSendMessageException(e);
        }
    }

    public boolean isConnected() {
        return localSocket.isConnected();
    }

    public boolean isAuth() {
        return pkIdentity != null;
    }

    public void setClientId(String clientId) {
        this.pkIdentity = clientId;
    }

    public String getClientId() {
        return pkIdentity;
    }


    private class SessionRunner implements Runnable{

        @Override
        public void run() {
            try {
                logger.info("Reader started for: " + pkIdentity);

                if (localSocket==null){
                    logger.error("local socket null");
                    return;
                }
                for (; ; ) {
                    if (localSocket.isConnected() || isServerSide) {
                        logger.info("prepare to read from local session..");
                        read();
                        logger.info("reading from local session..");
                    } else {
                        logger.warn("local socket is not connected "+pkIdentity);
                        if (!Thread.currentThread().isInterrupted()) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        closeNow();
                        break;
                    }
                }
            } catch (Exception e){
                logger.info("Exception on client: "+pkIdentity);
                e.printStackTrace();

                closeNow();

            }
        }
    }

    private synchronized void read() {
        int count;
        byte[] buffer = new byte[RECEIVE_BUFFER_SIZE];
        try {
            // read reply
            if (localSocket.isConnected() || isServerSide) {
                count = localSocket.getInputStream().read(buffer);
                logger.info("Reciving data..");
                ModuleObject.ModuleResponse response = null;
                if (count > 0) {
                    ByteBuffer byteBufferToRead = ByteBuffer.allocate(count);
                    byteBufferToRead.put(buffer, 0, count);
                    response = ModuleObject.ModuleResponse.parseFrom(byteBufferToRead.array());
                    sessionHandler.onReceive(this,response);
                } else {
                    // read < 0 -> connection closed
                    logger.info("Connection closed, read<0 with connect service , removing socket");
                    closeNow();
                }
            } else {
                // input stream closed
                logger.info("Connection closed, input stream shutdown with connect service , removing socket");
                closeNow();
            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        } catch (SocketException e){
            e.printStackTrace();
            logger.info("Connection closed, sslException with connect service, removing socket");

            closeNow();

        } catch (Exception e){
            e.printStackTrace();
            logger.info("Connection closed, unknown error, removing socket");

            closeNow();
        }
    }

    public void closeNow() {
        logger.info("Closing socket connect service ");
        if (!readThread.isInterrupted())
            readThread.interrupt();
        if (localSocket.isConnected())
            try {
                localSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        // notify upper layers
        if (sessionHandler!=null){
            try {
                sessionHandler.sessionClosed(this,pkIdentity);
            } catch (Exception e) {
                // swallow
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalSocketSession session = (LocalSocketSession) o;

        if (pkIdentity != null ? !pkIdentity.equals(session.pkIdentity) : session.pkIdentity != null)
            return false;
        if (!serverName.equals(session.serverName)) return false;
        return localSocket.equals(session.localSocket);

    }

    @Override
    public int hashCode() {
        int result = pkIdentity != null ? pkIdentity.hashCode() : 0;
        result = 31 * result + serverName.hashCode();
        result = 31 * result + localSocket.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "LocalSocketSession{" +
                "pkIdentity='" + pkIdentity + '\'' +
                ", serverName='" + serverName + '\'' +
                '}';
    }
}
