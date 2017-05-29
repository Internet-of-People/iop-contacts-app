package org.fermat.redtooth.profile_server;


import java.io.File;

import org.fermat.redtooth.profile_server.engine.ProfSerDb;

/**
 * Created by mati on 25/12/16.
 */

public interface ProfileServerConfigurations extends ProfSerDb{

    public String getHost();

    public int getPrimaryPort();

    public int getClPort();

    public int getNonClPort();

    public String getUsername();


    public byte[] getUserPubKey();


    public byte[] getProfileVersion() ;

    public boolean isRegisteredInServer();

    public boolean isIdentityCreated();

    public byte[] getProtocolVersion();

    public void setHost(String host);


    public Object getPrivObject(String name);

    public void savePrivObject(String name, Object obj);

    public void saveUserKeys(Object obj);

    public Object getUserKeys();

    public byte[] getUserPrivKey() ;

    public void setPrivKey(byte[] privKey);

    public void setPrimaryPort(int primaryPort);

    public void setClPort(int clPort);

    public void setNonClPort(int nonClPort);

    public void setUsername(String username);

    public void setUserPubKey(String userPubKeyHex);

    public void setUserPubKey(byte[] userPubKeyHex);

    public void setProfileVersion(byte[] version);


    public void setIsRegistered(boolean isRegistered);

    public void setIsCreated(boolean isCreated);


    File getUserImageFile();
}
