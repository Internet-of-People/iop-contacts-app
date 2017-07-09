package org.fermat.redtooth.profile_server;


import java.io.File;
import java.io.IOException;
import java.util.List;

import org.fermat.redtooth.profile_server.engine.ProfSerDb;
import org.fermat.redtooth.profile_server.model.KeyEd25519;
import org.fermat.redtooth.profile_server.model.ProfServerData;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profile_server.protocol.IopProfileServer;

/**
 * Created by mati on 25/12/16.
 */

public interface ProfileServerConfigurations extends ProfSerDb{

    /**
     *  Main profile server.
     * @return
     */
    ProfServerData getMainProfileServer();

    /**
     * Setters
     */
    void setMainPsPrimaryPort(int primaryPort);
    void setMainPfClPort(int clPort);
    void setMainPsNonClPort(int nonClPort);

    /**
     * Plan contract deal at the first time.
     * @return
     */
    IopProfileServer.HostingPlanContract getMainProfileServerContract();

    /**
     *  Known profile servers
     * @return
     */
    List<ProfServerData> getKnownProfileServers();


    String getUsername();

    byte[] getUserPubKey();

    byte[] getProfileVersion() ;

    boolean isRegisteredInServer();

    boolean isIdentityCreated();

    byte[] getProtocolVersion();

    void setHost(String host);

    void saveUserKeys(Object obj);

    Object getUserKeys();

    void setUsername(String username);


    void setIsRegistered(boolean isRegistered);

    void setIsCreated(boolean isCreated);


    File getUserImageFile();

    KeyEd25519 createUserKeys();

    KeyEd25519 createNewUserKeys();

    String getProfileType();

    void saveProfile(Profile profile);

    void setProfileType(String type);

    boolean isPairingEnable();

    Profile getProfile();

    byte[] getUserImage();

    void saveMainProfileServer(ProfServerData profServerData);


    // service stuff

    long getScheduleServiceTime();

    void saveScheduleServiceTime(long scheduleTime);
}
