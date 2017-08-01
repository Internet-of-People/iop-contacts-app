package org.libertaria.world.governance.vote;

import org.libertaria.world.governance.propose.Proposal;

import java.io.Serializable;

;

/**
 * Created by mati on 23/12/16.
 */

public class VoteWrapper implements Serializable {

    private Vote vote;
    private Proposal proposal;

    public VoteWrapper(Vote vote, Proposal proposal) {
        this.vote = vote;
        this.proposal = proposal;
    }

    public Vote getVote() {
        return vote;
    }

    public Proposal getProposal() {
        return proposal;
    }

    @Override
    public String toString() {
        return "VoteWrapper{" +
                "vote=" + vote +
                ", proposal=" + proposal +
                '}';
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
    }
}
