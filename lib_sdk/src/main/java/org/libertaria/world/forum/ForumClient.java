package org.libertaria.world.forum;


import org.libertaria.world.forum.wrapper.CantGetProposalsFromServerException;

/**
 * Created by mati on 28/11/16.
 */
public interface ForumClient {



    ForumProfile getForumProfile();

    boolean isRegistered();

    boolean registerUser(String username, String password, String email) throws InvalidUserParametersException, org.libertaria.world.forum.wrapper.AdminNotificationException;

    boolean connect(String username, String password) throws InvalidUserParametersException, org.libertaria.world.global.exceptions.ConnectionRefusedException, org.libertaria.world.forum.wrapper.AdminNotificationException;

    int createTopic(String title, String category, String raw) throws CantCreateTopicException, org.libertaria.world.forum.wrapper.AdminNotificationException;

    boolean updatePost(String title, int forumId, String category, String toForumBody) throws CantUpdatePostException;

    org.libertaria.world.governance.propose.Proposal getProposal(int forumId) throws org.libertaria.world.forum.wrapper.AdminNotificationException;

    org.libertaria.world.governance.propose.Proposal getProposalFromWrapper(int forumId) throws org.libertaria.world.forum.wrapper.AdminNotificationException, CantGetProposalsFromServerException;

    void getAndCheckValid(org.libertaria.world.governance.propose.Proposal proposal) throws org.libertaria.world.global.exceptions.NotValidParametersException, org.libertaria.world.forum.wrapper.AdminNotificationException;

    void clean();

    boolean replayTopic(int forumId, String text) throws CantReplayPostException;
}
