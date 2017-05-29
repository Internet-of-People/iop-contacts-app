package org.fermat.redtooth.governance.propose;

/**
 * Created by mati on 05/12/16.
 */

public interface ProposalsContractDao {

    boolean isLockedOutput(String hashHex, long index);

}
