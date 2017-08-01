package org.libertaria.world.governance.vote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Created by mati on 14/03/17.
 */

public class VoteUtils {


    /**
     * Return the amount of votes
     * @param ioptoshis
     * @return
     */
    public static BigDecimal calculateVoteAmountFromIoPtoshis(long ioptoshis, Vote.VoteType voteType){
        BigDecimal bigInteger = BigDecimal.valueOf(ioptoshis);
        bigInteger = bigInteger.movePointLeft(8);
        if (voteType== Vote.VoteType.NO){
            bigInteger = bigInteger.multiply(new BigDecimal(5));
        }
        return bigInteger;
    }

    public static BigDecimal calculateVoteAmountFromIoPtoshis(long ioptoshis){
        BigDecimal bigInteger = BigDecimal.valueOf(ioptoshis);
        bigInteger = bigInteger.movePointLeft(8);
        return bigInteger;
    }


    /**
     * Calcula la cantidad en IoPtoshis que equivalen los votos a enviar.
     * @param votes
     * @return
     */
    public static long calculateValueFromVotes(BigDecimal votes, Vote.VoteType voteType){
        if (voteType == Vote.VoteType.NEUTRAL) throw new IllegalArgumentException("calculate on neutral votes not allowed");
        BigDecimal res = votes.movePointRight(8);
        if (voteType == Vote.VoteType.NO){
            res = res.divide(new BigDecimal(5), RoundingMode.CEILING);
        }
        return res.longValueExact();
    }


    public static String format(BigDecimal votesAmount,int fractionDigistMin,int fractionDigitsMax){
        votesAmount = votesAmount.setScale(fractionDigitsMax, BigDecimal.ROUND_DOWN);
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(fractionDigitsMax);
        df.setMinimumFractionDigits(fractionDigistMin);
        df.setGroupingUsed(false);
        return df.format(votesAmount);
    }
}
