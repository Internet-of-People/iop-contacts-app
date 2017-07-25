package org.fermat.redtooth.services.interfaces;

import org.fermat.redtooth.profile_server.CantConnectException;
import org.fermat.redtooth.profile_server.CantSendMessageException;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.engine.futures.MsgListenerFuture;
import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;

import org.fermat.redtooth.global.Module;
import org.fermat.redtooth.profile_server.model.Profile;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by furszy on 7/19/17.
 *
 * todo: this interface should be on the sdk and not in this layer.. move me please..
 *
 */

public interface ProfilesModule extends Module {

    String registerProfile(String name,String type, byte[] img, int latitude, int longitude, String extraData) throws Exception;

    File backupProfile(File backupDir, String password) throws IOException;

    void scheduleBackupProfileFile(Profile profile, File backupDir, String password);

    String registerProfile(String name, byte[] img) throws Exception;

    void connect(String pubKey) throws Exception;

    int updateProfile(String pubKey , String name, byte[] img, int latitude, int longitude, String extraData, final ProfSerMsgListener<Boolean> msgListener) throws Exception;
    void updateProfile(String name, byte[] profImgData, MsgListenerFuture<Boolean> listenerFuture) throws Exception;

    File backupOverwriteProfile(Profile localProfile, File file, String backupPassword) throws IOException;

    void addService(String localProfilePubKey,String name);

    boolean isProfileConnectedOrConnecting();

    List<ProfileInformation> getKnownProfiles();

    ProfileInformation getKnownProfile(String remotePk);

    boolean isProfileRegistered();

    ProfileInformation getProfile();

    boolean isIdentityCreated();

    void getProfileInformation(String profPubKey, ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException;

    void getProfileInformation(String profPubKey, boolean getInfo, ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException;

    void restoreProfileFrom(File file, String password);
}
