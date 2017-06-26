package org.fermat.redtooth.profile_server.client;

import org.fermat.redtooth.profile_server.model.Profile;

/**
 * Created by mati on 06/02/17.
 */

public class ProfNodeConnection {

    /** User identity */
    private Profile profile;
    /** If the profile is already registered in the server */
    private boolean isRegistered;
    /** If the profile use or want to use this connection as home */
    private boolean isHome;
    /** Random 32 bytes number created by the client and sended to the server in the StartConversationRequest message */
    private byte[] connectionChallenge;
    /** Signed client challenge from the server  */
    private byte[] signedConnectionChallenge;
    /** Random 32 bytes number created by the node and obtained by the client in the StartConversationResponse */
    private byte[] nodeChallenge;
    /** Node's identity */
    private byte[] nodePubKey;
    /*  **/
    private boolean needRegisterProfile;

    public ProfNodeConnection(Profile profile,boolean isRegistered,boolean isHome,byte[] connectionChallenge) {
        this.profile = profile;
        this.isHome = isHome;
        this.connectionChallenge = connectionChallenge;
        this.isRegistered = isRegistered;
    }

    public void setSignedConnectionChallenge(byte[] signedConnectionChallenge) {
        this.signedConnectionChallenge = signedConnectionChallenge;
    }

    public void setNodeChallenge(byte[] nodeChallenge) {
        this.nodeChallenge = nodeChallenge;
    }

    public byte[] getSignedConnectionChallenge() {
        return signedConnectionChallenge;
    }

    public byte[] getConnectionChallenge() {
        return connectionChallenge;
    }

    public byte[] getNodeChallenge() {
        return nodeChallenge;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setIsRegistered(boolean isRegistered) {
        this.isRegistered = isRegistered;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public boolean isHome() {
        return isHome;
    }

    /**
     * No creo que estos metodos vayan acá..
     */

    public void setNeedRegisterProfile(boolean needRegisterProfile) {
        this.needRegisterProfile = needRegisterProfile;
    }

    public boolean isNeedRegisterProfile() {
        return needRegisterProfile;
    }

}
