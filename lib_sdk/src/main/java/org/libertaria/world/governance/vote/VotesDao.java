package org.libertaria.world.governance.vote;

import org.libertaria.world.governance.propose.Proposal;

import java.util.List;

/**
 * Created by mati on 26/12/16.
 */

public interface VotesDao {

    public boolean isLockedOutput(String parentVoteTransactionHash, long index);

    /**
     * Acá chequeo si el voto existe -> id == genesis transaction hash, si existe chequeo el tipo de voto (yes,no,neutral) si es igual devuelvo true, si existe pero tiene otro voto lanzo una excepcion o veo que carajo hago..
     * //todo: lazy lazy implementation
     *  @param vote
     * @return
     */
    public boolean exist(Vote vote);

    public boolean lockOutput(String genesisHash, String lockedOutputHex, int lockedOutputIndex);

    boolean unlockOutput(String genesisTxHash);
    /**
     *
     * @param vote
     * @return  vote ID
     */
    public long addUpdateIfExistVote(Vote vote);

    /**
     ¿     *
     ¿     * @return
     */
    public List<Vote> listVotes();

    void removeIfExist(Vote vote);

    void clean();


    Vote getVote(String genesisTxHash);

    long getTotalLockedBalance();

    List<org.libertaria.world.governance.vote.VoteWrapper> getVoteWrappers(List<Proposal> proposals);
}
