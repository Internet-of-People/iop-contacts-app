package org.libertaria.world.forum.wrapper;

/**
 * Created by mati on 23/12/16.
 */
public class CantGetProposalsFromServerException extends Exception {
    public CantGetProposalsFromServerException(String s, Exception e) {
        super(s,e);
    }

    public CantGetProposalsFromServerException(String s) {
        super(s);
    }

    public CantGetProposalsFromServerException(Exception e) {
        super(e);
    }
}
