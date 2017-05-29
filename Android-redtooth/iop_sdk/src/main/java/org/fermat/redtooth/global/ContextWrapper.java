package org.fermat.redtooth.global;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by mati on 12/11/16.
 */

public interface ContextWrapper {

    FileOutputStream openFileOutputPrivateMode(String name) throws FileNotFoundException;

    FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException;

    FileInputStream openFileInput(String name) throws FileNotFoundException;

    File getFileStreamPath(String name);

    File getDir(String name, int mode);

    File getDirPrivateMode(String name);

    void startService(int service, String command, Object... args);

    void toast(String text);

    PackageInfoWrapper packageInfoWrapper();

    boolean isMemoryLow();

    InputStream openAssestsStream(String name) throws IOException;

    String getPackageName();


    void sendLocalBroadcast(IntentWrapper intentWrapper);

    void showDialog(String id);

    void showDialog(String showBlockchainOffDialog, String dialogText);

    String[] fileList();


    void stopBlockchainService();
}
