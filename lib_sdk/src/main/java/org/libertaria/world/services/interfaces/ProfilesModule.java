package org.libertaria.world.services.interfaces;

import org.libertaria.world.exceptions.IncorrectPasswordException;
import org.libertaria.world.global.Module;
import org.libertaria.world.profile_server.CantConnectException;
import org.libertaria.world.profile_server.CantSendMessageException;
import org.libertaria.world.profile_server.ProfileInformation;
import org.libertaria.world.profile_server.engine.futures.MsgListenerFuture;
import org.libertaria.world.profile_server.engine.listeners.ProfSerMsgListener;
import org.libertaria.world.profile_server.model.Profile;

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

    File backupProfile(String localProfPubKey,File backupDir, String password) throws IOException;

    void scheduleBackupProfileFile(Profile profile, File backupDir, String password);

    String registerProfile(String name, byte[] img) throws Exception;

    void connect(String pubKey) throws Exception;

    void updateProfile(String pubKey , String name, byte[] img, int latitude, int longitude, String extraData, final ProfSerMsgListener<Boolean> msgListener) throws Exception;
    void updateProfile(String localProfPubKey,String name, byte[] profImgData, MsgListenerFuture<Boolean> listenerFuture) throws Exception;

    File backupOverwriteProfile(Profile localProfile, File file, String backupPassword) throws IOException;

    void addService(String localProfilePubKey,String name);

    boolean isProfileConnectedOrConnecting(String localProfilePubKey);

    List<ProfileInformation> getKnownProfiles(String localProfilePubKey);

    ProfileInformation getKnownProfile(String localProfilePubKey,String remotePk);

    List<ProfileInformation> getLocalProfiles();

    boolean isProfileRegistered(String localProfilePubKey);

    ProfileInformation getProfile(String localProfilePubKey);

    boolean isIdentityCreated(String localProfilePubKey);

    void getProfileInformation(String localProfilePubKey,String profPubKey, ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException;

    void getProfileInformation(String localProfilePubKey,String profPubKey, boolean getInfo, ProfSerMsgListener<ProfileInformation> profileFuture) throws CantConnectException, CantSendMessageException;

    void restoreProfileFrom(File file, String password) throws IOException, IncorrectPasswordException;
}
