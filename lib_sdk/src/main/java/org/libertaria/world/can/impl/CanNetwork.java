package org.libertaria.world.can.impl;


import org.libertaria.world.can.ClientApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CanNetwork implements org.libertaria.world.global.p2p.PeerToPeerNetwork<ClientApi>, org.libertaria.world.global.p2p.Bootstrappable {

    private static final Logger logger = LoggerFactory.getLogger(CanNetwork.class);

    static final String[] seeds = new String[] {
        "/ip4/104.199.219.45/tcp/14001/ipfs/QmfJGzktqwZK8S7LT55Q2nxDWCHRHzEgHgmVTyyrjqHpF5",  // ham4.fermat.cloud
        "/ip4/104.196.57.34/tcp/14001/ipfs/QmWXUcfL47AXgyThRmDBKDte8zaTTXtdUJvU38FLqPj4eF",   // ham5.fermat.cloud
        "/ip4/104.199.118.223/tcp/14001/ipfs/QmaM1ZXoWKXh7vc88HV51fwmsgMxjgmt6GLWMcm1CTU1zv", // ham6.fermat.cloud
        "/ip4/104.196.161.16/tcp/14001/ipfs/QmSgkC7sPsBfqWcTPgfFKjtk1zJsHBnh4iF5Ck9Yv4CJ2Z",  // ham7.fermat.cloud
    };

    private static final Pattern dumbMultiaddrPattern = Pattern.compile("/ip4/(\\d+\\.\\d+\\.\\d+\\.\\d+)/tcp/(\\d+)/ipfs/(.+)");

    private final org.libertaria.world.global.p2p.NodeSet<CanNode> nodeSet = new org.libertaria.world.global.p2p.NodeSet<>(CanNode.class);

    public CanNetwork() {
        for (String seed : seeds) {
            CanNode node = toCanNode(seed);
            if (node != null) nodeSet.add(node);
        }
    }

    private CanNode toCanNode(String multiaddr) {
        Matcher matcher = dumbMultiaddrPattern.matcher(multiaddr);
        if (!matcher.matches())
            return null;

        InetAddress address;
        try {
            address = InetAddress.getByName(matcher.group(1));
        } catch (UnknownHostException e) {
            return null;
        }

        int port;
        try {
            port = Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException e) {
            return null;
        }

        String id = matcher.group(3);
        return new CanNode(address, port, id);
    }

    // TODO
    @Override public synchronized void discoveryStep() {
        CanClientProxy proxy = getRandomNode();
        CanNode[] peers = proxy.getPeers();
        for (CanNode peer: peers) nodeSet.add(peer);
    }

    @Override
    public int getNodeCount() {
        return this.nodeSet.getCount();
    }

    // TODO
    @Override public synchronized org.libertaria.world.can.ClientApi connectRandomNode() throws IOException {
        return getRandomNode();
    }

    private CanClientProxy getRandomNode() {
        CanNode node = nodeSet.getRandomNode();
        return new CanClientProxy(node);
    }

    class CanNode {
        private final InetAddress address;
        private final int port;
        private final String id;

        CanNode(InetAddress address, int port, String id) {
            if (address == null)
                throw new IllegalArgumentException("address is null");
            if (id == null)
                throw new IllegalArgumentException("id is null");
            if (port < 1)
                throw new IllegalArgumentException("port is not positive");
            if (port > 65535)
                throw new IllegalArgumentException("port is too big");

            this.address = address;
            this.port = port;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CanNode)) return false;

            CanNode other = (CanNode) o;

            return port == other.port && address.equals(other.address) && id.equals(other.id);

        }

        @Override
        public int hashCode() {
            int result = address.hashCode();
            result = 31 * result + port;
            result = 31 * result + id.hashCode();
            return result;
        }

        public InetAddress getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }

        public String getId() {
            return id;
        }
    }

    class CanClientProxy implements org.libertaria.world.can.ClientApi {

        private final CanNode node;
        private org.libertaria.world.forum.discourge.com.wareninja.opensource.discourse.utils.WebClient webClient;

        public CanClientProxy(CanNode node) {
            this.node = node;
        }

        // TODO

        /**
         *
         * These method consist in two different request:
         *
         * IPNS and IPFS
         *
         * @param identifier A multihash encoded identifier of the profile
         * @return
         */
        @Override public org.libertaria.world.can.CanProfile getById(String identifier) {
            webClient = new org.libertaria.world.forum.discourge.com.wareninja.opensource.discourse.utils.WebClient(node.address.getHostAddress()+":"+node.getPort());
            String response = webClient.get("/api/v0/name/resolve?args="+identifier+"&recursive=true&nocache=false");
            logger.info("can ipns response: "+response);
            return null;
        }

        // TODO
        public CanNode[] getPeers() {
            CanNode[] a = new CanNode[]{
                    toCanNode("/ip4/104.199.219.44/tcp/14001/ipfs/QmeJGzktqwZK8S7LT55Q2nxDWCHRHzEgHgmVTyyrjqHpF5")
            };
            return a;
        }
    }
}
