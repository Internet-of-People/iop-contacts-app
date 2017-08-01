package org.libertaria.world.wallet.utils;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.params.RegTestParams;
import org.libertaria.world.global.HardCodedConstans;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mati on 30/09/16.
 */

public class RegtestUtil {


    private static final String LOCAL_PC = HardCodedConstans.HOST;//"186.23.58.203";//"192.168.0.111";

    public static List<PeerAddress> getConnectedPeers(NetworkParameters params,String host){
        if (!(params instanceof RegTestParams)) throw new IllegalArgumentException("NetworkParameters input is not a RegTestParams");

        List<PeerAddress> list = new ArrayList<>();
        // peers
        InetSocketAddress inetSocketAddress = new InetSocketAddress(host,7685);
        list.add(new PeerAddress(params,inetSocketAddress));
//        list.put(new PeerAddress(params,new InetSocketAddress(LOCAL_PC,7686)));
        list.add(new PeerAddress(params,new InetSocketAddress(host,7684)));
        return list;
    }


    public static InetSocketAddress[] getPeersToConnect(NetworkParameters params,String host){
        if (!(params instanceof RegTestParams)) throw new IllegalArgumentException("NetworkParameters input is not a RegTestParams");

        InetSocketAddress[] list = new InetSocketAddress[1];
        // peers
        InetSocketAddress inetSocketAddress = new InetSocketAddress(host,7685);
        list[0]=(inetSocketAddress);
//        list.put(new PeerAddress(params,new InetSocketAddress(LOCAL_PC,7686)));
        return list;
    }


}