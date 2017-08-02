package iop.org.iop_sdk_android.core.service.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.libertaria.world.crypto.CryptoBytes;
import org.libertaria.world.global.Version;
import org.libertaria.world.profile_server.engine.app_services.AppService;
import org.libertaria.world.profile_server.model.Profile;
import org.libertaria.world.profiles_manager.LocalProfilesDao;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import iop.org.iop_sdk_android.core.crypto.KeyEd25519;

/**
 * Created by furszy on 7/27/17.
 */

public class LocalProfilesDb extends AbstractSqliteDb<Profile> implements LocalProfilesDao {

    public static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "local_profiles";
    public static final String TABLE_NAME = "local_profiles";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_VERSION = "ver";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_IMG = "img";
    public static final String COLUMN_LAT = "lat";
    public static final String COLUMN_LON = "lon";
    public static final String COLUMN_EXTRA_DATA = "extra";
    public static final String COLUMN_PUB_KEY = "pubKey";
    public static final String COLUMN_PRIV_KEY = "privKey";
    public static final String COLUMN_APP_SERVICES = "app_services";
    public static final String COLUMN_HOME_HOST = "home_host";


    public static final int POS_COLUMN_ID = 0;
    public static final int POS_COLUMN_NAME = 1;
    public static final int POS_COLUMN_VERSION = 2;
    public static final int POS_COLUMN_TYPE = 3;
    public static final int POS_COLUMN_IMG = 4;
    public static final int POS_COLUMN_LAT = 5;
    public static final int POS_COLUMN_LON = 6;
    public static final int POS_COLUMN_EXTRA_DATA = 7;
    public static final int POS_COLUMN_PUB_KEY = 8;
    public static final int POS_COLUMN_PRIV_KEY = 9;
    public static final int POS_COLUMN_APP_SERVICES = 10;
    public static final int POS_COLUMN_HOME_HOST = 11;

    public LocalProfilesDb(Context context) {
        super(context, DATABASE_NAME , null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "create table " +TABLE_NAME+
                        "(" +
                        COLUMN_ID + " INTEGER primary key autoincrement, "+
                        COLUMN_NAME + " TEXT, "+
                        COLUMN_VERSION + " BLOB, "+
                        COLUMN_TYPE + " TEXT, "+
                        COLUMN_IMG + " BLOB, "+
                        COLUMN_LAT + " INTEGER, "+
                        COLUMN_LON + " INTEGER, "+
                        COLUMN_EXTRA_DATA + " TEXT ,"+
                        COLUMN_PUB_KEY + " TEXT ,"+
                        COLUMN_PRIV_KEY + " TEXT ,"+
                        COLUMN_APP_SERVICES + " TEXT ,"+
                        COLUMN_HOME_HOST + " TEXT "
                        +")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
    }

    @Override
    String getTableName() {
        return TABLE_NAME;
    }

    public ContentValues buildContent(Profile profile){
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_NAME, profile.getName());
        contentValues.put(COLUMN_TYPE, profile.getType());
        if (profile.getVersion()!=null)
            contentValues.put(COLUMN_VERSION, profile.getVersion().toByteArray());
        contentValues.put(COLUMN_PUB_KEY, profile.getHexPublicKey());
        contentValues.put(COLUMN_PRIV_KEY, profile.getPrivKeyHex());
        Set<String> services = profile.getAppServices();
        if (services!=null) {
            contentValues.put(COLUMN_APP_SERVICES, convertToString(services));
        }
        contentValues.put(COLUMN_HOME_HOST,profile.getHomeHost());
        if (profile.getImg()!=null && profile.getImg().length>0)
            contentValues.put(COLUMN_IMG,profile.getImg());
        contentValues.put(COLUMN_LAT,profile.getLatitude());
        contentValues.put(COLUMN_LON,profile.getLongitude());
        return contentValues;
    }

    public Profile buildFrom(Cursor cursor){
        try {
            long id = cursor.getInt(POS_COLUMN_ID);
            String name = cursor.getString(POS_COLUMN_NAME);
            byte[] pubKey = CryptoBytes.fromHexToBytes(cursor.getString(POS_COLUMN_PUB_KEY));
            byte[] seed = CryptoBytes.fromHexToBytes(cursor.getString(POS_COLUMN_PRIV_KEY));
            String extraData = cursor.getString(POS_COLUMN_EXTRA_DATA);
            String type = cursor.getString(POS_COLUMN_TYPE);
            Set<String> appServices = convertToSet(cursor.getString(POS_COLUMN_APP_SERVICES));
            String homeHost = cursor.getString(POS_COLUMN_HOME_HOST);
            byte[] img = cursor.getBlob(POS_COLUMN_IMG);
            int lat = cursor.getInt(POS_COLUMN_LAT);
            int lon = cursor.getInt(POS_COLUMN_LON);
            byte[] version = cursor.getBlob(POS_COLUMN_VERSION);
            Profile profile = new Profile();
            if (version!=null)
                profile.setVersion(Version.fromByteArray(version));
            profile.setName(name);
            profile.setType(type);
            profile.setKey(KeyEd25519.wrap(seed,pubKey));
            profile.addAllServices(appServices);
            profile.setExtraData(extraData);
            profile.setHomeHost(homeHost);
            profile.setImg(img);
            profile.setLongitude(lon);
            profile.setLatitude(lat);
            profile.setId(id);
            return profile;
        } catch (JSONException e) {
            e.printStackTrace();
            throw new DbCursorBuildException("AppServices hashset malformed");
        }
    }

    private String convertToString(Set<String> set){
        JSONArray jsonArray = new JSONArray(set);
        return jsonArray.toString();
    }

    private Set<String> convertToSet(String jsonString) throws JSONException {
        Set<String> set = new HashSet<>();
        JSONArray jsonArray = new JSONArray(jsonString);
        for (int i=0;i<jsonArray.length();i++){
            set.add(jsonArray.getString(i));
        }
        return set;
    }


    @Override
    public long save(Profile profile) {
        long id = insert(profile);
        profile.setId(id);
        return id;
    }

    @Override
    public void updateProfile(Profile profile) {
        update(COLUMN_ID,String.valueOf(profile.getId()),profile);
    }

    @Override
    public Profile getProfile(String profilePublicKey) {
        return get(COLUMN_PUB_KEY,profilePublicKey);
    }
}
