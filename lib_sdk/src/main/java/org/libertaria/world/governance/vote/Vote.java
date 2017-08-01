package org.libertaria.world.governance.vote;

import org.libertaria.world.crypto.CryptoBytes;

import java.io.Serializable;


/**
 * Created by mati on 21/12/16.
 */

public class Vote implements Serializable,Cloneable {

    /** Min vote value in IoPtoshis */
    public static final long MIN_POSITIVE_VOTE_VALUE = 10000000000L;
    public static final long MIN_NEGATIVE_VOTE_VALUE = 2000000000L;


    public enum VoteType{
        NO,
        NEUTRAL,
        YES
    }

    /** Application id */
    private long voteId;
    /** contract wich the vote is pointing hex */
    private String genesisHashHex;
    /** Vote -> yes/no */
    private VoteType vote;
    /** freeze outputs -> is the amount of votes that the user is giving as yes or no */
    private long votingPower;
    /** locked values */
    private String lockedOutputHashHex;
    private int lockedOutputIndex = 0;
    private boolean outputFrozen;

    public Vote(String genesisHash, VoteType vote, long votingPower) {
        this.genesisHashHex = genesisHash;
        this.vote = vote;
        this.votingPower = votingPower;
    }

    public Vote(long voteId,String genesisHashHex, VoteType vote, long votingPower, String lockedOutputHashHex, int lockedOutputIndex,boolean isOutputFrozen) {
        this.voteId = voteId;
        this.genesisHashHex = genesisHashHex;
        this.vote = vote;
        this.votingPower = votingPower;
        this.lockedOutputHashHex = lockedOutputHashHex;
        this.lockedOutputIndex = lockedOutputIndex;
        this.outputFrozen = isOutputFrozen;
    }

    /**
     *
     * @return
     */
    public boolean isYesVote() {
        return vote == VoteType.YES;
    }

    public byte[] getGenesisHash() {
        return CryptoBytes.fromHexToBytes(genesisHashHex);
    }


    public String getGenesisHashHex() {
        return genesisHashHex;
    }

    public void setVoteId(long voteId) {
        this.voteId = voteId;
    }

    public void setOutputFrozen(boolean outputFrozen) {
        this.outputFrozen = outputFrozen;
    }

    public void setVoteType(VoteType voteType) {
        this.vote = voteType;
    }

    public void setAmount(long amount) {
        this.votingPower = amount;
    }


    public VoteType getVote() {
        return vote;
    }

    public long getVotingPower() {
        return votingPower;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "outputFrozen=" + outputFrozen +
                ", lockedOutputIndex=" + lockedOutputIndex +
                ", lockedOutputHashHex='" + lockedOutputHashHex + '\'' +
                ", votingPower=" + votingPower +
                ", vote=" + vote +
                ", genesisHashHex='" + genesisHashHex + '\'' +
                ", voteId=" + voteId +
                '}';
    }

    public void setLockedOutputHashHex(String lockedOutputHashHex) {
        this.lockedOutputHashHex = lockedOutputHashHex;
    }

    public void setLockedOutputIndex(int lockedOutputIndex) {
        this.lockedOutputIndex = lockedOutputIndex;
    }

    public String getLockedOutputHex() {
        return lockedOutputHashHex;
    }

    public int getLockedOutputIndex() {
        return lockedOutputIndex;
    }

    public long getVoteId() {
        return voteId;
    }

    public boolean isOutputFrozen() {
        return outputFrozen;
    }


    @Override
    public Vote clone() throws CloneNotSupportedException {
        return (Vote) super.clone();
    }
}
