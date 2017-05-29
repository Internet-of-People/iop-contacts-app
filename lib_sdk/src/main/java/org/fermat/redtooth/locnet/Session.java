package org.fermat.redtooth.locnet;

import org.fermat.redtooth.locnet.protocol.IopLocNet;

import java.io.IOException;
import java.util.*;




public class Session
{
    Connection connection;


    public Session(NodeInfo node) throws IOException
    {
        this( node.getContact().getAddress().getHostAddress(), node.getContact().getPort() );
//        System.out.println("Connecting node " + node);
    }

    public Session(String host, int port) throws IOException
        { connection = new Connection(host, port); }


    public List<NodeInfo> getNeighbourhood() throws IOException
    {
        IopLocNet.ClientRequest.Builder request = IopLocNet.ClientRequest.newBuilder();
        request.getGetNeighbourNodesBuilder();

        connection.SendRequest( request.build() );
        IopLocNet.ClientResponse response = connection.ReceiveResponse();

        if ( response.getClientResponseTypeCase() != IopLocNet.ClientResponse.ClientResponseTypeCase.GETNEIGHBOURNODES )
            { throw new IllegalStateException("Received unexpected response type"); }
        return Converter.fromProtoBuf( response.getGetNeighbourNodes().getNodesList() );
    }


    public List<NodeInfo> getClosestNodes(NodeInfo.GpsLocation location,
        float maxRadiusKm, int maxNodeCount, boolean includeNeighbours) throws IOException
    {
        IopLocNet.ClientRequest.Builder request = IopLocNet.ClientRequest.newBuilder();
        request.getGetClosestNodesBuilder()
            .setLocation( Converter.toProtoBuf(location) )
            .setMaxRadiusKm(maxRadiusKm)
            .setMaxNodeCount(maxNodeCount)
            .setIncludeNeighbours(includeNeighbours);

        connection.SendRequest( request.build() );
        IopLocNet.ClientResponse response = connection.ReceiveResponse();

        if ( response.getClientResponseTypeCase() != IopLocNet.ClientResponse.ClientResponseTypeCase.GETCLOSESTNODES )
            { throw new IllegalStateException("Received unexpected response type"); }
        return Converter.fromProtoBuf( response.getGetClosestNodes().getNodesList() );
    }


    public NodeInfo getNodeInfo() throws IOException
    {
        IopLocNet.ClientRequest.Builder request = IopLocNet.ClientRequest.newBuilder();
        request.getGetNodeInfoBuilder();

        connection.SendRequest( request.build() );
        IopLocNet.ClientResponse response = connection.ReceiveResponse();

        if ( response.getClientResponseTypeCase() != IopLocNet.ClientResponse.ClientResponseTypeCase.GETNODEINFO )
            { throw new IllegalStateException("Received unexpected response type"); }
        return Converter.fromProtoBuf( response.getGetNodeInfo().getNodeInfo() );
    }
}
