package org.fermat.redtooth.loc;

import com.google.common.annotations.VisibleForTesting;

import org.fermat.redtooth.locnet.Explorer;
import org.fermat.redtooth.locnet.NodeInfo;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Created by furszy on 6/9/17.
 */

public class LocTest {

    @Test
    public void exploreTest() throws ExecutionException, InterruptedException {

        /*// Explore profile servers around Argentina
        Explorer explorer = new Explorer( NodeInfo.ServiceType.Profile, new NodeInfo.GpsLocation(34.7667f, 58.4000f), 10000, 10 );
        FutureTask< List<NodeInfo> > task = new FutureTask<>(explorer);
        task.run();
        List<NodeInfo> resultNodes = task.get();

        System.out.println("Found " + resultNodes.size() + " matching nodes");
        for (NodeInfo node : resultNodes)
        { System.out.println("  " + node); }
*/
    }

}
