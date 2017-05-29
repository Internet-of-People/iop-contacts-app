package org.fermat.redtooth.governance.vote;

import java.util.List;

import org.fermat.redtooth.governance.propose.Proposal;

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

    List<VoteWrapper> getVoteWrappers(List<Proposal> proposals);
}
