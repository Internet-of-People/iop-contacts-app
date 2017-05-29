package org.fermat.redtooth.profile_server.engine;

/**
 * Created by mati on 06/02/17.
 */

public interface ProfSerDb {


    /** Save that the profile is already registered in the server
     * //todo: here i have to add more field like the timestamp, regiter plan, etc. to know when the user has to pay.
     * */
    void setProfileRegistered(String host, String profilePublicKey);

    void removeProfileRegistered(String profilePublicKey,String host);

    /**  */
    boolean isRegistered(String host, String profilePublicKey);



    // ports

    void setMainPfClPort(int port);
    void setMainPsNonClPort(int port);
    void setMainAppServicePort(int port);
}
