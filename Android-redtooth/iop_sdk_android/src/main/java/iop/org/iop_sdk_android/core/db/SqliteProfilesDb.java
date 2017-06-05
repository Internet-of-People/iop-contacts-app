package iop.org.iop_sdk_android.core.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.fermat.redtooth.crypto.CryptoBytes;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profile_server.imp.ProfileInformationImp;
import org.fermat.redtooth.profile_server.model.Profile;
import org.fermat.redtooth.profiles_manager.ProfilesManager;

import java.util.ArrayList;
import java.util.List;

import iop.org.iop_sdk_android.core.profile_server.PrivateStorage;

/**
 * Created by furszy on 5/25/17.
 */

public class SqliteProfilesDb extends SQLiteOpenHelper implements ProfilesManager {


    public static final int DATABASE_VERSION = 8;

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
    public static final String CONTACTS_COLUMN_PAIR = "pair";


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
                        CONTACTS_COLUMN_PAIR + " INTEGER"
                +")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS contacts");
        onCreate(db);
    }

    public long insertContact (ProfileInformation profile) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = db.insert(CONTACTS_TABLE_NAME, null, buildContent(profile));
        return id;
    }

    public ContentValues buildContent(ProfileInformation profile){
        ContentValues contentValues = new ContentValues();
        contentValues.put(CONTACTS_COLUMN_NAME, profile.getName());
        contentValues.put(CONTACTS_COLUMN_TYPE, profile.getType());
        contentValues.put(CONTACTS_COLUMN_VERSION, profile.getVersion());
        contentValues.put(CONTACTS_COLUMN_PUB_KEY, CryptoBytes.toHexString(profile.getPublicKey()));
        contentValues.put(CONTACTS_COLUMN_UPDATE_TIMESTAMP,profile.getLastUpdateTime());
        contentValues.put(CONTACTS_COLUMN_PAIR,profile.isPaired());
        return contentValues;
    }

    public ProfileInformation buildFrom(Cursor cursor){
        String name = cursor.getString(CONTACTS_POS_COLUMN_NAME);
        byte[] pubKey = CryptoBytes.fromHexToBytes(cursor.getString(CONTACTS_POS_COLUMN_PUB_KEY));
        String extraData = cursor.getString(CONTACTS_POS_COLUMN_EXTRA_DATA);
        String type = cursor.getString(CONTACTS_POS_COLUMN_TYPE);
        long timestamp = cursor.getLong(CONTACTS_POS_COLUMN_UPDATE_TIMESTAMP);
        boolean isPaired = cursor.getInt(CONTACTS_POS_COLUMN_PAIR)==0?false:true;
        ProfileInformationImp profile = new ProfileInformationImp();
        profile.setVersion(new byte[]{0,0,1});
        profile.setName(name);
        profile.setType(type);
        profile.setPubKey(pubKey);
        profile.setUpdateTimestamp(timestamp);
        profile.setIsPaired(isPaired);
        return profile;
    }

    public Cursor getData(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+CONTACTS_TABLE_NAME+" where "+CONTACTS_COLUMN_ID+"="+id+"", null );
        return res;
    }

    public Cursor getData(byte[] pubKey) {
        if (pubKey==null) throw new IllegalArgumentException("pubKey cannot be null");
        SQLiteDatabase db = this.getReadableDatabase();
        String pubKeyStr = CryptoBytes.toHexString(pubKey);
        Cursor res =  db.rawQuery( "select * from "+CONTACTS_TABLE_NAME+" where "+CONTACTS_COLUMN_PUB_KEY+"='"+pubKeyStr+"'", null );
        return res;
    }

    public int numberOfRows(){
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, CONTACTS_TABLE_NAME);
        return numRows;
    }

    public boolean updateContact (ProfileInformation profile) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = buildContent(profile);
        db.update(CONTACTS_TABLE_NAME, contentValues, CONTACTS_COLUMN_PUB_KEY+" = ? ", new String[] { profile.getHexPublicKey() } );
        return true;
    }

    private void updateFieldByKey(byte[] publicKey, String column, boolean value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(column,value);
        db.update(CONTACTS_TABLE_NAME,contentValues,CONTACTS_COLUMN_PUB_KEY+"=?",new String[]{CryptoBytes.toHexString(publicKey)});
    }

    public Integer deleteContact (Integer id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(CONTACTS_TABLE_NAME,
                CONTACTS_COLUMN_ID+" = ? ",
                new String[] { Integer.toString(id) });
    }

    public ArrayList<ProfileInformation> getAllCotacts() {
        ArrayList<ProfileInformation> list = new ArrayList<>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+CONTACTS_TABLE_NAME, null );
        if(res.moveToFirst()) {
            do {
                list.add(buildFrom(res));
            } while (res.moveToNext());
        }
        return list;
    }

    @Override
    public List<ProfileInformation> listAll() {
        return getAllCotacts();
    }

    @Override
    public void updatePaired(byte[] publicKey, boolean value) {
        updateFieldByKey(publicKey,CONTACTS_COLUMN_PAIR,value);
    }

    @Override
    public long saveProfile(ProfileInformation profile) {
        return insertContact(profile);
    }

    @Override
    public boolean updateProfile(ProfileInformation profile) {
        return updateContact(profile);
    }

    @Override
    public ProfileInformation getProfile(long id) {
        Cursor cursor = getData(id);
        if (cursor.moveToFirst()){
            return buildFrom(cursor);
        }
        return null;
    }

    @Override
    public ProfileInformation getProfile(byte[] pubKey) {
        Cursor cursor = getData(pubKey);
        if (cursor.moveToFirst()){
            return buildFrom(cursor);
        }
        return null;
    }

    @Override
    public List<ProfileInformation> listOwnProfiles(byte[] pubKey) {
        return null;
    }

    @Override
    public List<ProfileInformation> listConnectedProfiles(byte[] pubKey) {
        // todo: return the real data and not all profiles
        return getAllCotacts();
    }


}
