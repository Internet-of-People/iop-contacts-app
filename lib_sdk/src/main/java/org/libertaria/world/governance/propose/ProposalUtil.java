package org.libertaria.world.governance.propose;


/**
 * Created by mati on 29/12/16.
 */

public class ProposalUtil {


    /**
     * Return time in minutes
     *
     * @param proposal
     * @return
     */
    public static double getEstimatedTimeToContractExecution(Proposal proposal){
        long startBlockHeight = proposal.getVotingPeriod()+1000;
        long endBlock = startBlockHeight+proposal.getEndBlock();
        return (endBlock-startBlockHeight)*10;
    }

    public static String getContractUserState(int chainHead,Proposal proposal){
        int chainVetoPeriodStart = chainHead-proposal.getVotingPeriod();;

        String ret;

        Proposal.ProposalState state = proposal.getState();
        if (state == Proposal.ProposalState.SUBMITTED || state == Proposal.ProposalState.APPROVED || state == Proposal.ProposalState.NOT_APPROVED){
            ret = "voting";
        } else if (state == Proposal.ProposalState.QUEUED_FOR_EXECUTION){
            ret = "veto";
        } else {
            ret = state.toString().replace("_"," ").toLowerCase();
        }
        return ret;
    }

    public static String getBlocksLeftToChangeState(int chainHead,Proposal proposal){
        long vetoBlockStart = proposal.getStartingBlock()-1008;
        if (chainHead>proposal.getStartingBlock()){
            return "";
        }else if (chainHead<vetoBlockStart){
            return org.libertaria.world.utils.StringUtils.formatNumberToString(vetoBlockStart-chainHead)+" blocks";
        }else if (chainHead>vetoBlockStart){
            return org.libertaria.world.utils.StringUtils.formatNumberToString(proposal.getStartingBlock()-chainHead) + " blocks";
        }
        return "";
    }


}
