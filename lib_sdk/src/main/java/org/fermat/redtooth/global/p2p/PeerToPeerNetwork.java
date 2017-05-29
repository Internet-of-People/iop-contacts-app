package org.fermat.redtooth.global.p2p;

import java.io.IOException;

/**
 * Represents a whole peer-to-peer network used by an application.
 * @param <T> The public interface of the network towards its clients.
 */
public interface PeerToPeerNetwork<T> {
    /**
     * Synchronously client to a node on the peer-to-peer network. The node to connect to is chosen
     * randomly among the nodes known on the network. Some implementations might decide to try to
     * connect to multiple nodes in parallel and use the 1st one that was connected successfully.
     *
     * @return The client API of the network
     * @throws IOException If the connection did not succeed because of network connectivity
     *   problems
     */
    T connectRandomNode() throws IOException;
}
