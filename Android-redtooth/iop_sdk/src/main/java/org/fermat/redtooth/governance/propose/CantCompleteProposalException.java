package org.fermat.redtooth.governance.propose;

/**
 * Created by mati on 17/01/17.
 */
public class CantCompleteProposalException extends Exception {

    public CantCompleteProposalException(String s) {
        super(s);
    }

    public CantCompleteProposalException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
