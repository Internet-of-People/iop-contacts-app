package org.libertaria.world.locnet;

import org.libertaria.world.global.GpsLocation;

import java.net.InetAddress;
import java.util.List;



public class NodeInfo
{

    public static class Contact
    {
        InetAddress address;
        int         port;

        public Contact(InetAddress address, int port)
        {
            this.address = address;
            this.port    = port;
        }

        public InetAddress getAddress() { return address; }
        public int getPort() { return port; }

        @Override public String toString() { return address + ":" + port; }
    }


    public enum ServiceType {
        Unstructured,
        Content,
        Latency,
        Location,

        Token,
        Profile,
        Proximity,
        Relay,
        Reputation,
        Minting,
    }

    public static class ServiceInfo
    {
        ServiceType type;
        int         port;
        byte[] data;

        public ServiceInfo(ServiceType type, int port, byte[] data)
        {
            this.type = type;
            this.port = port;
            this.data = data;
        }

        public ServiceType getType() { return type; }
        public int getPort() { return port; }
        public byte[] getServiceData() { return data; }

        @Override public String toString() { return type + " on port " + port + " (" + data + ")"; }
    }


    byte[]      nodeId;
    Contact     contact;
    GpsLocation location;
    List<ServiceInfo> services;


    public NodeInfo(byte[] nodeId, Contact contact, GpsLocation location, List<ServiceInfo> services)
    {
        this.nodeId   = nodeId;
        this.contact  = contact;
        this.location = location;
        this.services = services;
    }

    public byte[] getNodeId() { return nodeId; }
    public Contact getContact() { return contact; }
    public GpsLocation getLocation() { return location; }
    public List<ServiceInfo> getServiceInfo() { return services; }

    @Override public String toString() { return nodeId + ": " + location + ", " + contact + " (" + services.size() + " services)"; }
}
