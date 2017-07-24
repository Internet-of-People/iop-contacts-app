package org.fermat.redtooth.services.interfaces;

import org.fermat.redtooth.profile_server.engine.listeners.ProfSerMsgListener;

import org.fermat.redtooth.global.Module;
import org.fermat.redtooth.profile_server.model.Profile;

import java.io.File;
import java.io.IOException;

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

    File backupOverwriteProfile(Profile localProfile, File file, String backupPassword) throws IOException;
}
