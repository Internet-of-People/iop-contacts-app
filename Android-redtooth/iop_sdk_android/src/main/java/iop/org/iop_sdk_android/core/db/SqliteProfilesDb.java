package iop.org.iop_sdk_android.core.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.global.Version;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.imp.ProfileInformationImp;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profiles_manager.ProfilesManager;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import iop.org.iop_sdk_android.core.profile_server.PrivateStorage;

/**
 * Created by furszy on 5/25/17.
 */

public class SqliteProfilesDb extends SQLiteOpenHelper implements ProfilesManager {


    public static final int DATABASE_VERSION = 12;

    public static final String DATABASE_NAME = "profiles";
    public static final String CONTACTS_TABLE_NAME = "contacts";
    public static final String CONTACTS_COLUMN_ID = "id";
    public static final String CONTACTS_COLUMN_NAME = "name";
    public static final String CONTACTS_COLUMN_VERSION = "ver";
    public static final String CONTACTS_COLUMN_TYPE = "type";
    public static final String CONTACTS_COLUMN_IMG = "img";
    public static final String CONTACTS_COLUMN_LAT = "lat";
    public static final String CONTACTS_COLUMN_LON = "lon";
    public static final String CONTACTS_COLUMN_EXTRA_DATA = "extra";
    public static final String CONTACTS_COLUMN_PUB_KEY = "pubKey";
    public static final String CONTACTS_COLUMN_UPDATE_TIMESTAMP = "up_time";
    // Local profile who knows this profile, todo: this should be splitted in different tables..
    public static final String CONTACTS_COLUMN_PAIR = "pair_status";
    public static final String CONTACTS_COLUMN_DEVICE_PROFILE_PUB_KEY = "local_pub_key";
    public static final String CONTACTS_COLUMN_APP_SERVICES = "app_services";
    public static final String CONTACTS_COLUMN_HOME_HOST = "home_host";


    public static final int CONTACTS_POS_COLUMN_ID = 0;
    public static final int CONTACTS_POS_COLUMN_NAME = 1;
    public static final int CONTACTS_POS_COLUMN_VERSION = 2;
    public static final int CONTACTS_POS_COLUMN_TYPE = 3;
    public static final int CONTACTS_POS_COLUMN_IMG = 4;
    public static final int CONTACTS_POS_COLUMN_LAT = 5;
    public static final int CONTACTS_POS_COLUMN_LON = 6;
    public static final int CONTACTS_POS_COLUMN_EXTRA_DATA = 7;
    public static final int CONTACTS_POS_COLUMN_PUB_KEY = 8;
    public static final int CONTACTS_POS_COLUMN_UPDATE_TIMESTAMP = 9;
    public static final int CONTACTS_POS_COLUMN_PAIR = 10;
    public static final int CONTACTS_POS_COLUMN_DEVICE_PROFILE_PUB_KEY = 11;
    public static final int CONTACTS_POS_COLUMN_APP_SERVICES = 12;
    public static final int CONTACTS_POS_COLUMN_HOME_HOST = 13;

    public SqliteProfilesDb(Context context) {
        super(context, DATABASE_NAME , null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                "create table " +CONTACTS_TABLE_NAME+
                        "(" +
                        CONTACTS_COLUMN_ID + " INTEGER primary key autoincrement, "+
                        CONTACTS_COLUMN_NAME + " TEXT, "+
                        CONTACTS_COLUMN_VERSION + " BLOB, "+
                        CONTACTS_COLUMN_TYPE + " TEXT, "+
                        CONTACTS_COLUMN_IMG + " BLOB, "+
                        CONTACTS_COLUMN_LAT + " INTEGER, "+
                        CONTACTS_COLUMN_LON + " INTEGER, "+
                        CONTACTS_COLUMN_EXTRA_DATA + " TEXT ,"+
                        CONTACTS_COLUMN_PUB_KEY + " TEXT ,"+
                        CONTACTS_COLUMN_UPDATE_TIMESTAMP + " LONG ,"+
                        CONTACTS_COLUMN_PAIR + " TEXT ,"+
                        CONTACTS_COLUMN_DEVICE_PROFILE_PUB_KEY + " TEXT ,"+
                        CONTACTS_COLUMN_APP_SERVICES + " TEXT ,"+
                        CONTACTS_COLUMN_HOME_HOST + " TEXT "
                +")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS contacts");
        onCreate(db);
    }

    public long insertContact (String localProfileOwnerOfThisContact,ProfileInformation profile) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = db.insert(CONTACTS_TABLE_NAME, null, buildContent(profile,localProfileOwnerOfThisContact));
        return id;
    }

    public ContentValues buildContent(ProfileInformation profile,String localProfileWhoKnowThis){
        ContentValues contentValues = new ContentValues();
        contentValues.put(CONTACTS_COLUMN_NAME, profile.getName());
        contentValues.put(CONTACTS_COLUMN_TYPE, profile.getType());
        if (profile.getVersion()!=null)
        contentValues.put(CONTACTS_COLUMN_VERSION, profile.getVersion().toByteArray());
        contentValues.put(CONTACTS_COLUMN_PUB_KEY, CryptoBytes.toHexString(profile.getPublicKey()));
        contentValues.put(CONTACTS_COLUMN_UPDATE_TIMESTAMP,profile.getLastUpdateTime());
        contentValues.put(CONTACTS_COLUMN_PAIR,profile.getPairStatus().name());
        contentValues.put(CONTACTS_COLUMN_DEVICE_PROFILE_PUB_KEY,localProfileWhoKnowThis);
        contentValues.put(CONTACTS_COLUMN_APP_SERVICES,convertToString(profile.getServices()));
        contentValues.put(CONTACTS_COLUMN_HOME_HOST,profile.getHomeHost());
        if (profile.getImg()!=null && profile.getImg().length>0)
            contentValues.put(CONTACTS_COLUMN_IMG,profile.getImg());
        contentValues.put(CONTACTS_COLUMN_LAT,profile.getLatitude());
        contentValues.put(CONTACTS_COLUMN_LON,profile.getLongitude());
        return contentValues;
    }

    public ProfileInformationWrapper buildFrom(Cursor cursor){
        try {
            String name = cursor.getString(CONTACTS_POS_COLUMN_NAME);
            byte[] pubKey = CryptoBytes.fromHexToBytes(cursor.getString(CONTACTS_POS_COLUMN_PUB_KEY));
            String extraData = cursor.getString(CONTACTS_POS_COLUMN_EXTRA_DATA);
            String type = cursor.getString(CONTACTS_POS_COLUMN_TYPE);
            long timestamp = cursor.getLong(CONTACTS_POS_COLUMN_UPDATE_TIMESTAMP);
            String localProfilePubKey = cursor.getString(CONTACTS_POS_COLUMN_DEVICE_PROFILE_PUB_KEY);
            ProfileInformationImp.PairStatus pairStatus = ProfileInformationImp.PairStatus.valueOf(cursor.getString(CONTACTS_POS_COLUMN_PAIR));
            Set<String> appServices = convertToSet(cursor.getString(CONTACTS_POS_COLUMN_APP_SERVICES));
            String homeHost = cursor.getString(CONTACTS_POS_COLUMN_HOME_HOST);
            byte[] img = cursor.getBlob(CONTACTS_POS_COLUMN_IMG);
            int lat = cursor.getInt(CONTACTS_POS_COLUMN_LAT);
            int lon = cursor.getInt(CONTACTS_POS_COLUMN_LON);
            byte[] version = cursor.getBlob(CONTACTS_POS_COLUMN_VERSION);
            ProfileInformationImp profile = new ProfileInformationImp();
            if (version!=null)
            profile.setVersion(Version.fromByteArray(version));
            profile.setName(name);
            profile.setType(type);
            profile.setPubKey(pubKey);
            profile.setUpdateTimestamp(timestamp);
            profile.setPairStatus(pairStatus);
            profile.setExtraData(extraData);
            profile.addAllAppServices(appServices);
            profile.setHomeHost(homeHost);
            profile.setImg(img);
            profile.setLongitude(lon);
            profile.setLatitude(lat);
            return new ProfileInformationWrapper(localProfilePubKey, profile);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new DbCursorBuildException("AppServices hashset malformed");
        }
    }

    private Set<String> convertToSet(String jsonString) throws JSONException {
        Set<String> set = new HashSet<>();
        JSONArray jsonArray = new JSONArray(jsonString);
        for (int i=0;i<jsonArray.length();i++){
            set.add(jsonArray.getString(i));
        }
        return set;
    }

    private String convertToString(Set<String> set){
        JSONArray jsonArray = new JSONArray(set);
        return jsonArray.toString();
    }

    public Cursor getData(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+CONTACTS_TABLE_NAME+" where "+CONTACTS_COLUMN_ID+"="+id+"", null );
        return res;
    }

    public Cursor getData(String localProfileOwnerOfContacts,String pubKey) {
        if (pubKey==null) throw new IllegalArgumentException("pubKey cannot be null");
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+CONTACTS_TABLE_NAME+" where "+CONTACTS_COLUMN_PUB_KEY+"='"+pubKey+"' and "+CONTACTS_COLUMN_DEVICE_PROFILE_PUB_KEY+" = '"+localProfileOwnerOfContacts+"'", null );
        return res;
    }

    public int numberOfRows(){
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, CONTACTS_TABLE_NAME);
        return numRows;
    }

    public boolean updateContact (String localProfileOwnerOfThisContact,ProfileInformation profile) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = buildContent(profile,localProfileOwnerOfThisContact);
        db.update(CONTACTS_TABLE_NAME, contentValues, CONTACTS_COLUMN_PUB_KEY+" = ? and "+CONTACTS_COLUMN_DEVICE_PROFILE_PUB_KEY+" = ?", new String[] { profile.getHexPublicKey(),localProfileOwnerOfThisContact } );
        return true;
    }

    private void updateFieldByKey(byte[] publicKey, String column, boolean value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(column,value);
        db.update(CONTACTS_TABLE_NAME,contentValues,CONTACTS_COLUMN_PUB_KEY+"=?",new String[]{CryptoBytes.toHexString(publicKey)});
    }

    private boolean updateFieldByKey(byte[] publicKey, String column, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(column,value);
        return db.update(CONTACTS_TABLE_NAME,contentValues,CONTACTS_COLUMN_PUB_KEY+"=?",new String[]{CryptoBytes.toHexString(publicKey)})==1;
    }

    public Integer deleteContact (Integer id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(CONTACTS_TABLE_NAME,
                CONTACTS_COLUMN_ID+" = ? ",
                new String[] { Integer.toString(id) });
    }

    public ArrayList<ProfileInformation> getAllCotacts(String localProfileOwnerOfThisContact) {
        ArrayList<ProfileInformation> list = new ArrayList<>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+CONTACTS_TABLE_NAME, null );
        if(res.moveToFirst()) {
            do {
                list.add(buildFrom(res).profileInformation);
            } while (res.moveToNext());
        }
        return list;
    }

    @Override
    public List<ProfileInformation> listAll(String localProfilePubKeyOwnerOfContact) {
        return getAllCotacts(localProfilePubKeyOwnerOfContact);
    }

    public void truncate() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(CONTACTS_TABLE_NAME,null,null);
    }

    @Override
    public boolean updatePaired(String localProfilePubKeyOwnerOfContact,String remotePubKey, ProfileInformationImp.PairStatus value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(CONTACTS_COLUMN_PAIR, value.name());
        return db.update(
                CONTACTS_TABLE_NAME,
                contentValues,
                CONTACTS_COLUMN_PUB_KEY+"=? and "+CONTACTS_COLUMN_DEVICE_PROFILE_PUB_KEY+" =?",
                new String[]{remotePubKey,localProfilePubKeyOwnerOfContact})==1;
    }

    @Override
    public boolean updateRemoteServices(String localProfilePubKey, String remotePubKey, Set<String> services) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(CONTACTS_COLUMN_APP_SERVICES, convertToString(services));
        return db.update(
                CONTACTS_TABLE_NAME,
                contentValues,
                CONTACTS_COLUMN_PUB_KEY+"=? and "+CONTACTS_COLUMN_DEVICE_PROFILE_PUB_KEY+" =?",
                new String[]{remotePubKey,localProfilePubKey})==1;
    }

    @Override
    public void saveAllProfiles(String localProfilePubKey, List<ProfileInformation> profileInformationList) {
        //todo: improve this shit.. lazy lazy save.
        for (ProfileInformation profileInformation : profileInformationList) {
            saveProfile(localProfilePubKey,profileInformation);
        }
    }


    @Override
    public long saveProfile(String localProfilePubKeyOwnerOfContact, ProfileInformation profile) {
        return insertContact(localProfilePubKeyOwnerOfContact,profile);
    }

    public void saveOrUpdateProfile(String localProfilePubKeyOwnerOfContact, ProfileInformation profile){
        ProfileInformation dbProf = null;
        if ((dbProf = getProfile(localProfilePubKeyOwnerOfContact,profile.getHexPublicKey()))==null){
            saveProfile(localProfilePubKeyOwnerOfContact,profile);
        }else {
            profile.setPairStatus(dbProf.getPairStatus());
            profile.setLastUpdateTime(System.currentTimeMillis());
            updateProfile(localProfilePubKeyOwnerOfContact,profile);
        }
    }

    @Override
    public boolean updateProfile(String localProfilePubKeyOwnerOfContact, ProfileInformation profile) {
        return updateContact(localProfilePubKeyOwnerOfContact,profile);
    }

    @Override
    public ProfileInformation getProfile(long id) {
        Cursor cursor = getData(id);
        if (cursor.moveToFirst()){
            return buildFrom(cursor).profileInformation;
        }
        return null;
    }

    @Override
    public ProfileInformation getProfile(String localProfileOwnerOfContacts, String pubKey) {
        Cursor cursor = getData(localProfileOwnerOfContacts,pubKey);
        if (cursor.moveToFirst()){
            return buildFrom(cursor).profileInformation;
        }
        return null;
    }

    @Override
    public List<ProfileInformation> listOwnProfiles(String localProfileOwnerOfContacts) {
        return null;
    }

    @Override
    public List<ProfileInformation> listConnectedProfiles(String localProfileOwnerOfContacts) {
        ArrayList<ProfileInformation> list = new ArrayList<>();
        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+CONTACTS_TABLE_NAME
                +" where "+CONTACTS_COLUMN_DEVICE_PROFILE_PUB_KEY+" = '" +localProfileOwnerOfContacts+"' " +
                "AND "+CONTACTS_COLUMN_PAIR+"='"+ ProfileInformationImp.PairStatus.PAIRED.name()+"'", null );
        if(res.moveToFirst()) {
            do {
                list.add(buildFrom(res).profileInformation);
            } while (res.moveToNext());
        }
        return list;
    }



    private class ProfileInformationWrapper{
        String localProfilePubKey;
        ProfileInformation profileInformation;

        public ProfileInformationWrapper(String localProfilePubKey, ProfileInformation profileInformation) {
            this.localProfilePubKey = localProfilePubKey;
            this.profileInformation = profileInformation;
        }
    }


}
