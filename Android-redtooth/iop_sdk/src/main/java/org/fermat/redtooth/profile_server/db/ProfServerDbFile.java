package org.fermat.redtooth.profile_server.db;

import java.io.File;
import java.util.concurrent.ConcurrentMap;

import org.fermat.redtooth.profile_server.ProfileServerConfigurations;
import org.fermat.redtooth.profile_server.engine.ProfSerDb;

/**
 * Created by mati on 07/02/17.
 */

public class ProfServerDbFile implements ProfSerDb {

    private final String MAP_PROFILE_REGISTRATION = "profilesRegistrationMap";

    private String dbFileName;

    //private Map<String,String> registered;

    public ProfServerDbFile(File dir) {
        //this.registered = new HashMap<>();
        dbFileName = dir.getAbsolutePath()+"/prof_ser_db";
    }

    @Override
    public void setProfileRegistered(String host, String profilePublicKey) {
//        registered.put(profilePublicKey,host);
//        DB db = DBMaker.fileDB(dbFileName).make();
//        ConcurrentMap map = db.hashMap(MAP_PROFILE_REGISTRATION).createOrOpen();
//        map.put(profilePublicKey,host);
//        db.close();
    }

    @Override
    public void removeProfileRegistered(String profilePublicKey, String host) {
//        DB db = DBMaker.fileDB(dbFileName).make();
//        ConcurrentMap map = db.hashMap(MAP_PROFILE_REGISTRATION).createOrOpen();
//        if(map.containsKey(profilePublicKey)){
//            if (map.get(profilePublicKey).equals(host)){
//                map.remove(profilePublicKey);
//            }
//        }
//        db.close();
    }

    @Override
    public boolean isRegistered(String host, String profilePublicKey) {
//        DB db = DBMaker.fileDB(dbFileName).make();
//        ConcurrentMap map = db.hashMap(MAP_PROFILE_REGISTRATION).createOrOpen();
//        boolean isRegister = map.containsKey(profilePublicKey) && map.get(profilePublicKey).equals(host);
//        db.close();
//        return isRegister;
        return false;
    }

    @Override
    public void setClPort(int port) {

    }

    @Override
    public void setNonClPort(int port) {

    }
}
