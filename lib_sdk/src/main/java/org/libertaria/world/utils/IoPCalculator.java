package org.libertaria.world.utils;

/**
 * Created by mati on 21/12/16.
 */

public class IoPCalculator {


    public static long iopToshisToIoPs(long iopToshis){
        return iopToshis/10000000;
    }

    public static long iopToIopToshis(long IoP){
        return IoP*10000000;
    }

}
