package org.libertaria.world.locnet;

import com.google.protobuf.ByteString;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;


public class Connection
{
    private static int MessageHeaderSize = 5;
    private static int MessageSizeOffset = 1;


//    String host;
//    int    port;
    Socket socket;
    int    lastMessageId;



    public Connection(String host, int port) throws IOException
    {
//        this.host = host;
//        this.port = port;
//        System.out.println("Connecting to " + host+ ":" + port);
        socket = new Socket(host, port);
    }


//    public String getHost() { return host; }
//    public int getPort() { return port; }


    static int GetMessageSizeFromHeader(byte[] data)
    {
        if (data.length != MessageHeaderSize)
            { throw new IllegalArgumentException("Invalid message header: static assert failed"); }

        // Adapt big endian value from network to local format
        return   ( data[MessageSizeOffset]     & 0xFF ) +
               ( ( data[MessageSizeOffset + 1] & 0xFF ) << 8 ) +
               ( ( data[MessageSizeOffset + 2] & 0xFF ) << 16 ) +
               ( ( data[MessageSizeOffset + 3] & 0xFF ) << 24 );
    }


    org.libertaria.world.locnet.protocol.IopLocNet.Message ReadMessage() throws IOException
    {
        InputStream socketInput = socket.getInputStream();
        byte[] messageHeader = new byte[MessageHeaderSize];
        socketInput.read(messageHeader);

        int bodySize = GetMessageSizeFromHeader(messageHeader);
//        System.out.println("Reading message size " + bodySize);
        byte[] messageBuf = Arrays.copyOf(messageHeader, MessageHeaderSize + bodySize);
        socketInput.read(messageBuf, MessageHeaderSize, bodySize);

        org.libertaria.world.locnet.protocol.IopLocNet.MessageWithHeader message = org.libertaria.world.locnet.protocol.IopLocNet.MessageWithHeader.parseFrom(messageBuf);
        if (! message.hasBody())
            { throw new IllegalArgumentException("Received message frame without body"); }
        return message.getBody();
    }


    public org.libertaria.world.locnet.protocol.IopLocNet.ClientResponse ReceiveResponse() throws IOException
    {
        org.libertaria.world.locnet.protocol.IopLocNet.Message message = ReadMessage();
        if ( message.getMessageTypeCase() != org.libertaria.world.locnet.protocol.IopLocNet.Message.MessageTypeCase.RESPONSE ||
             message.getResponse().getResponseTypeCase() != org.libertaria.world.locnet.protocol.IopLocNet.Response.ResponseTypeCase.CLIENT )
            { throw new IllegalStateException("Expected client response message, got something different"); }

//        System.out.println("Got message");
//        System.out.println( JsonFormat.printer().print(message) );
        return message.getResponse().getClient();
    }

    void WriteMessage(org.libertaria.world.locnet.protocol.IopLocNet.Message message) throws IOException
    {
//        System.out.println("Writing message");
//        System.out.println( JsonFormat.printer().print(message) );
        ++lastMessageId;
        org.libertaria.world.locnet.protocol.IopLocNet.Message.Builder preparedMessage = message.toBuilder()
            .setId(lastMessageId);

        org.libertaria.world.locnet.protocol.IopLocNet.MessageWithHeader.Builder msgToSend = org.libertaria.world.locnet.protocol.IopLocNet.MessageWithHeader.newBuilder()
            .setBody(preparedMessage)
            .setHeader(1);
        msgToSend.setHeader( msgToSend.build().getSerializedSize() - MessageHeaderSize );

        msgToSend.build().writeTo( socket.getOutputStream() );
    }


    public void SendRequest(org.libertaria.world.locnet.protocol.IopLocNet.ClientRequest request) throws IOException
    {
        org.libertaria.world.locnet.protocol.IopLocNet.Request.Builder reqBuilder = org.libertaria.world.locnet.protocol.IopLocNet.Request.newBuilder()
            .setVersion( ByteString.copyFrom( new byte[]{1, 0, 0} ) )
            .setClient(request);
        org.libertaria.world.locnet.protocol.IopLocNet.Message msgToSend = org.libertaria.world.locnet.protocol.IopLocNet.Message.newBuilder()
            .setRequest(reqBuilder).build();
        WriteMessage(msgToSend);
    }
}
