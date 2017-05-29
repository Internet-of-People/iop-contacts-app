package org.fermat.redtooth.locnet;

import org.fermat.redtooth.locnet.protocol.IopLocNet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;



public class Converter
{
    static final float GPS_COORDINATE_PROTOBUF_INT_MULTIPLIER = 1000000.f;


    public static NodeInfo.GpsLocation fromProtoBuf(IopLocNet.GpsLocation location)
    {
        return new NodeInfo.GpsLocation(
            location.getLatitude() / GPS_COORDINATE_PROTOBUF_INT_MULTIPLIER,
            location.getLongitude() / GPS_COORDINATE_PROTOBUF_INT_MULTIPLIER );
    }


    public static NodeInfo.Contact fromProtoBuf(IopLocNet.NodeContact contact) throws UnknownHostException
    {
        return new NodeInfo.Contact(
            InetAddress.getByAddress( contact.getIpAddress().toByteArray() ),
            contact.getClientPort() );
    }


    public static NodeInfo.ServiceType fromProtoBuf(IopLocNet.ServiceType type) throws UnknownHostException
    {
        switch (type)
        {
            case Content:       return NodeInfo.ServiceType.Content;
            case Latency:       return NodeInfo.ServiceType.Latency;
            case Location:      return NodeInfo.ServiceType.Location;
            case Minting:       return NodeInfo.ServiceType.Minting;
            case Profile:       return NodeInfo.ServiceType.Profile;
            case Proximity:     return NodeInfo.ServiceType.Proximity;
            case Relay:         return NodeInfo.ServiceType.Relay;
            case Reputation:    return NodeInfo.ServiceType.Reputation;
            case Token:         return NodeInfo.ServiceType.Unstructured;
            case Unstructured:  return NodeInfo.ServiceType.Unstructured;
            default: throw new IllegalArgumentException("Not implemented for unknown enum value: " + type);
        }
    }


    public static NodeInfo fromProtoBuf(IopLocNet.NodeInfo node) throws UnknownHostException
    {
        List<NodeInfo.ServiceInfo> services = new ArrayList();
        for ( IopLocNet.ServiceInfo info : node.getServicesList() )
        {
            services.add( new NodeInfo.ServiceInfo(
                fromProtoBuf( info.getType() ),
                info.getPort(),
                info.getServiceData().toByteArray() ) );
        }

        return new NodeInfo(
            node.getNodeId().toStringUtf8(),
            fromProtoBuf( node.getContact() ),
            fromProtoBuf( node.getLocation() ),
            services );
    }


    public static List<NodeInfo> fromProtoBuf(List<IopLocNet.NodeInfo> nodes) throws UnknownHostException
    {
        ArrayList<NodeInfo> result = new ArrayList<>();
        for (IopLocNet.NodeInfo node : nodes)
            { result.add( Converter.fromProtoBuf(node) ); }
        return result;
    }


    public static IopLocNet.GpsLocation toProtoBuf(NodeInfo.GpsLocation location)
    {
        return IopLocNet.GpsLocation.newBuilder()
            .setLatitude(  (int)(location.getLatitude()  * GPS_COORDINATE_PROTOBUF_INT_MULTIPLIER) )
            .setLongitude( (int)(location.getLongitude() * GPS_COORDINATE_PROTOBUF_INT_MULTIPLIER) )
            .build();
    }
}
