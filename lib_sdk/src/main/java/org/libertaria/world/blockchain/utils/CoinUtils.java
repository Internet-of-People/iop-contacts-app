package org.libertaria.world.blockchain.utils;

import org.bitcoinj.core.Coin;

/**
 * Created by mati on 19/12/16.
 */

public class CoinUtils {

    public static String coinToString(long amount){
        String value = Coin.valueOf(amount).toPlainString();
        if (value.length()==4 || value.length()==3) return value;
        int pointIndex = value.indexOf('.');
        if (pointIndex!=-1){
            if (value.length()>pointIndex+2)
                value = value.substring(0,pointIndex+3);
            else
                value = value.substring(0,pointIndex+2);
        }
        return value;
    }


}
