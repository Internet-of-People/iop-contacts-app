package org.fermat.redtooth.locnet;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;



public class Explorer implements Callable< List<NodeInfo> >
{
    static int DefaultClientPort = 16981;
    static List<String> SeedServers = Arrays.asList(
        "ham4.fermat.cloud",
        "ham5.fermat.cloud",
        "ham6.fermat.cloud",
        "ham7.fermat.cloud" );



    NodeInfo.ServiceType serviceType;
    NodeInfo.GpsLocation searchLocation;
    float                maxRadiusKm;
    int                  maxNodeCount;


    static org.gavaghan.geodesy.GeodeticCalculator geoCalc = new org.gavaghan.geodesy.GeodeticCalculator();
    static org.gavaghan.geodesy.Ellipsoid reference = org.gavaghan.geodesy.Ellipsoid.WGS84;

    double getDistanceKm(NodeInfo.GpsLocation location)
    {
        org.gavaghan.geodesy.GlobalPosition searchLocationGps = new org.gavaghan.geodesy.GlobalPosition(
            searchLocation.getLatitude(), searchLocation.getLongitude(), 0.0);
        org.gavaghan.geodesy.GlobalPosition userPos = new org.gavaghan.geodesy.GlobalPosition(
            location.getLatitude(), location.getLongitude(), 0.0);
        return geoCalc.calculateGeodeticCurve(reference, userPos, searchLocationGps).getEllipsoidalDistance() / 1000.;
    }



    public Explorer(NodeInfo.ServiceType serviceType, NodeInfo.GpsLocation searchLocation,
                    float maxRadiusKm, int maxNodeCount)
    {
        this.searchLocation = searchLocation;
        this.serviceType    = serviceType;
        this.maxRadiusKm    = maxRadiusKm;
        this.maxNodeCount   = maxNodeCount;
    }


    Session createSeedSession() throws IOException
    {
        Random rand = new Random();
        List<String> remainingSeeds = new ArrayList<>(SeedServers);
        while ( ! remainingSeeds.isEmpty() )
        {
            int selectedSeedIdx = rand.nextInt( remainingSeeds.size() );
            String seedHost = remainingSeeds.remove(selectedSeedIdx);
            // TODO log error instead of printing to console
            try { return new Session(seedHost, DefaultClientPort); }
            catch (Exception ex) { System.out.println("Failed to contact seed server " + seedHost); }
        }
        throw new IOException("All seed servers tried and failed");
    }



    @Override public List<NodeInfo> call() throws IOException
    {
        // Find closest node to specified searchLocation
        Session session = createSeedSession();
        NodeInfo oldClosestNode = null;
        NodeInfo newClosestNode = session.getNodeInfo();
        while ( oldClosestNode == null || // Just starting up or successfully got closer to the specified location
                getDistanceKm( newClosestNode.getLocation() ) < getDistanceKm( oldClosestNode.getLocation() ) )
        {
            oldClosestNode = newClosestNode;
            List<NodeInfo> closeNodes = session.getClosestNodes(
                searchLocation, Float.MAX_VALUE, 1, true);
            if ( closeNodes.isEmpty() )
                { break; }
            newClosestNode = closeNodes.get(0);
            try { session = new Session(newClosestNode); }
            catch(Exception ex) { break; }
        }

        // Collect matching nodes starting from closest node found
        // TODO how to properly ensure that enough nodes survive filtering?
        //      Probably the filter itself should be integrated into the protobuf definitions.
        List<NodeInfo> collectedNodes = session.getClosestNodes(
            searchLocation, maxRadiusKm, 2 * maxNodeCount, true);
        List<NodeInfo> matchingNodes = filterType(collectedNodes, serviceType);
        // TODO what algorithmic strategy to follow here to collect more nodes if necessary?
        while ( matchingNodes.size() < maxNodeCount ) {
            //TODO check if there is any chance to get more nodes;
            List<NodeInfo> nodes = filterDistance(matchingNodes, maxRadiusKm);
            //todo: don't fuck it..
        }
        return matchingNodes.subList( 0, Math.min( matchingNodes.size(), maxNodeCount) );
    }



    public interface IPredicate<T> { boolean appliesTo(T item); }


    static <T> List<T> filter(Collection<T> items, IPredicate<T> predicate)
    {
        List<T> result = new ArrayList<>();
        for (T item : items) {
            if ( predicate.appliesTo(item) ) {
                result.add(item); } }
        return result;
    }


    List<NodeInfo> filterType(Collection<NodeInfo> nodes, final NodeInfo.ServiceType serviceType)
    {
        return filter( nodes, new IPredicate<NodeInfo>()
        {
            @Override public boolean appliesTo(NodeInfo node)
            {
                if ( node != null && node.getServiceInfo() != null ) {
                    for ( NodeInfo.ServiceInfo service : node.getServiceInfo() ) {
                        if ( service.getType() == serviceType ) {
                            return true; } } }
                return false;
            }
        } );
    }


    List<NodeInfo> filterDistance(Collection<NodeInfo> nodes, final float maxDistanceKm) {
        return filter( nodes, new IPredicate<NodeInfo>() {
            @Override public boolean appliesTo(NodeInfo node) {
                if ( node != null && node.getLocation() != null ) {
                    return getDistanceKm( node.getLocation() ) < maxDistanceKm ;
                }
                return false;
            }
        });}
    }


