package org.libertaria.world.locnet;

import org.libertaria.world.global.GpsLocation;

import java.util.List;
import java.util.concurrent.FutureTask;



public class Main
{
    public static void main(String[] args) {
        try
        {
            // Explore profile servers around Budapest
            Explorer explorer = new Explorer( org.libertaria.world.locnet.NodeInfo.ServiceType.Profile,
                new GpsLocation(47.497912f, 19.040235f), 10000, 10 );
            FutureTask< List<org.libertaria.world.locnet.NodeInfo> > task = new FutureTask<>(explorer);
            task.run();
            List<org.libertaria.world.locnet.NodeInfo> resultNodes = task.get();

            System.out.println("Found " + resultNodes.size() + " matching nodes");
            for (org.libertaria.world.locnet.NodeInfo node : resultNodes)
                { System.out.println("  " + node); }
        }
        catch (Exception ex) { ex.printStackTrace(); }

/*
        String host = args.length >= 1 ? args[0] : "127.0.0.1";
        int port    = args.length >= 2 ? Integer.parseInt(args[1]) : 16981;
        System.out.println("Connecting to " + host + ":" + port);

        try
        {
            Session session = new Session(host, port);

            System.out.println("Querying neighbours");
            List<NodeInfo> neighbours = session.getNeighbourhood();
            System.out.println("Got " + neighbours.size() + " neighbours");
            for (NodeInfo node : neighbours)
                { System.out.println("  Neighbour: " + node); }

            System.out.println("Querying close nodes");
            List<NodeInfo> closeNodes = session.getClosestNodes(
                new NodeInfo.GpsLocation(0, 0),
                10000, 100, true );
            System.out.println("Got " + closeNodes.size() + " nodes");
            for (NodeInfo node : closeNodes)
                { System.out.println("  Node: " + node); }
        }
        catch (Exception ex) { ex.printStackTrace(); }
*/
    }
}
