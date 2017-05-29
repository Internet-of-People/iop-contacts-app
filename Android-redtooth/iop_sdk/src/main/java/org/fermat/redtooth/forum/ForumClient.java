package org.fermat.redtooth.forum;


import org.fermat.redtooth.forum.wrapper.AdminNotificationException;
import org.fermat.redtooth.forum.wrapper.CantGetProposalsFromServerException;
import org.fermat.redtooth.global.exceptions.ConnectionRefusedException;
import org.fermat.redtooth.global.exceptions.NotValidParametersException;
import org.fermat.redtooth.governance.propose.Proposal;

/**
 * Created by mati on 28/11/16.
 */
public interface ForumClient {



    ForumProfile getForumProfile();

    boolean isRegistered();

    boolean registerUser(String username, String password, String email) throws InvalidUserParametersException, AdminNotificationException;

    boolean connect(String username, String password) throws InvalidUserParametersException, ConnectionRefusedException, AdminNotificationException;

    int createTopic(String title, String category, String raw) throws CantCreateTopicException, AdminNotificationException;

    boolean updatePost(String title, int forumId, String category, String toForumBody) throws CantUpdatePostException;

    Proposal getProposal(int forumId) throws AdminNotificationException;

    Proposal getProposalFromWrapper(int forumId) throws AdminNotificationException, CantGetProposalsFromServerException;

    void getAndCheckValid(Proposal proposal) throws NotValidParametersException, AdminNotificationException;

    void clean();

    boolean replayTopic(int forumId, String text) throws CantReplayPostException;
}
