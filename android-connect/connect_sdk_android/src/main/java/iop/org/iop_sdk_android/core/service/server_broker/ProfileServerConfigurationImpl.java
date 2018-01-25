package iop.org.iop_sdk_android.core.service.server_broker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.libertaria.world.profile_server.ProfileServerConfiguration;
import org.libertaria.world.profile_server.model.ProfServerData;
import org.spongycastle.util.encoders.Base64;

import java.util.ArrayList;
import java.util.List;

import iop.org.iop_sdk_android.core.service.db.AbstractSqliteDb;


/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 20/12/2017.
 */

public class ProfileServerConfigurationImpl extends AbstractSqliteDb<ProfServerData> implements ProfileServerConfiguration {

    //VARIABLE DECLARATION
    public static final int DATABASE_VERSION = 12;

    public static final String DATABASE_NAME = "profile_server_configuration";

    public static final String CONFIGURATIONS_TABLE_NAME = "configurations";

    public static final String CONFIGURATIONS_COLUMN_HOST_IP = "host_ip";
    public static final String CONFIGURATIONS_COLUMN_HOST_PORT = "host_port";
    public static final String CONFIGURATIONS_COLUMN_HOST_NODE_ID = "host_node_id";
    public static final String CONFIGURATIONS_COLUMN_CUST_PORT = "cust_port";
    public static final String CONFIGURATIONS_COLUMN_NON_CUST_PORT = "non_cust_port";
    public static final String CONFIGURATIONS_COLUMN_APP_PORT = "app_port";
    public static final String CONFIGURATIONS_COLUMN_LATITUDE = "latitude";
    public static final String CONFIGURATIONS_COLUMN_LONGITUDE = "longitude";
    public static final String CONFIGURATIONS_COLUMN_SELECTED = "selected";
    public static final String CONFIGURATIONS_COLUMN_CERTIFICATE = "certificate";


    public static final int CONFIGURATIONS_POS_COLUMN_HOST_IP = 0;
    public static final int CONFIGURATIONS_POS_COLUMN_HOST_PORT = 1;
    public static final int CONFIGURATIONS_POS_COLUMN_HOST_NODE_ID = 2;
    public static final int CONFIGURATIONS_POS_COLUMN_CUST_PORT = 3;
    public static final int CONFIGURATIONS_POS_COLUMN_NON_CUST_PORT = 4;
    public static final int CONFIGURATIONS_POS_COLUMN_APP_PORT = 5;
    public static final int CONFIGURATIONS_POS_COLUMN_LATITUDE = 6;
    public static final int CONFIGURATIONS_POS_COLUMN_LONGITUDE = 7;
    public static final int CONFIGURATIONS_POS_COLUMN_SELECTED = 8;
    public static final int CONFIGURATIONS_POS_COLUMN_CERTIFICATE = 9;


    private List<ProfServerData> registeredServers = new ArrayList<>();
    private ProfServerData selectedServer = null;

    //CONSTRUCTORS
    public ProfileServerConfigurationImpl(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //PUBLIC METHODS
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + CONFIGURATIONS_TABLE_NAME +
                        "(" +
                        CONFIGURATIONS_COLUMN_HOST_IP + " TEXT PRIMARY KEY, " +
                        CONFIGURATIONS_COLUMN_HOST_PORT + " INTEGER, " +
                        CONFIGURATIONS_COLUMN_HOST_NODE_ID + " TEXT, " +
                        CONFIGURATIONS_COLUMN_CUST_PORT + " INTEGER, " +
                        CONFIGURATIONS_COLUMN_NON_CUST_PORT + " INTEGER, " +
                        CONFIGURATIONS_COLUMN_APP_PORT + " INTEGER, " +
                        CONFIGURATIONS_COLUMN_LATITUDE + " LONG, " +
                        CONFIGURATIONS_COLUMN_LONGITUDE + " LONG," +
                        CONFIGURATIONS_COLUMN_SELECTED + " TEXT," +
                        CONFIGURATIONS_COLUMN_CERTIFICATE + " TEXT)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + CONFIGURATIONS_TABLE_NAME);
        onCreate(db);
    }

    @Override
    public ProfServerData getSelectedProfileServer() {
        if (selectedServer == null) {
            selectedServer = buildFrom(getOne(CONFIGURATIONS_COLUMN_SELECTED, Boolean.TRUE));
        }
        return selectedServer;
    }

    @Override
    public List<ProfServerData> getRegisteredServers() {
        if (registeredServers.isEmpty()) {
            Cursor res = getData("1", "1");
            if (res.moveToFirst()) {
                do {
                    try {
                        ProfServerData serverData = buildFrom(res);
                        if (serverData.isHome()) {
                            selectedServer = serverData;
                        }
                        registeredServers.add(serverData);
                    } catch (Exception e) {
                        e.printStackTrace(); //Let's ignore only this record...
                    }
                } while (res.moveToNext());
            }
        }
        return registeredServers;
    }

    @Override
    public ProfServerData registerNewServer(String host, Integer port) {
        ProfServerData profServerData = new ProfServerData(host);
        profServerData.setpPort(port);
        profServerData.setHome(false);
        insert(profServerData);
        registeredServers.add(profServerData);
        return profServerData;
    }

    @Override
    public void updateServer(ProfServerData profServerData) {
        update(CONFIGURATIONS_COLUMN_HOST_IP, profServerData.getHost(), profServerData);
    }

    @Override
    public void selectServer(ProfServerData profServerData) {
        updateFieldByKey(CONFIGURATIONS_COLUMN_HOST_IP, profServerData.getHost(), CONFIGURATIONS_COLUMN_SELECTED, true);
        updateDifferentFrom(CONFIGURATIONS_COLUMN_HOST_IP, profServerData.getHost(), CONFIGURATIONS_COLUMN_SELECTED, String.valueOf(false));
    }

    @Override
    protected String getTableName() {
        return CONFIGURATIONS_TABLE_NAME;
    }

    //PRIVATE METHODS
    @Override
    protected ContentValues buildContent(ProfServerData profServerData) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CONFIGURATIONS_COLUMN_HOST_IP, profServerData.getHost());
        contentValues.put(CONFIGURATIONS_COLUMN_HOST_PORT, profServerData.getpPort());
        contentValues.put(CONFIGURATIONS_COLUMN_HOST_NODE_ID, Base64.toBase64String(profServerData.getNetworkId()));
        contentValues.put(CONFIGURATIONS_COLUMN_CUST_PORT, profServerData.getCustPort());
        contentValues.put(CONFIGURATIONS_COLUMN_NON_CUST_PORT, profServerData.getNonCustPort());
        contentValues.put(CONFIGURATIONS_COLUMN_APP_PORT, profServerData.getAppServicePort());
        contentValues.put(CONFIGURATIONS_COLUMN_LATITUDE, profServerData.getLatitude());
        contentValues.put(CONFIGURATIONS_COLUMN_LONGITUDE, profServerData.getLongitude());
        contentValues.put(CONFIGURATIONS_COLUMN_SELECTED, String.valueOf(profServerData.isHome()));
        contentValues.put(CONFIGURATIONS_COLUMN_SELECTED, profServerData.getServerCertificate());
        return contentValues;
    }

    @Override
    protected ProfServerData buildFrom(Cursor cursor) {
        int hostPort = cursor.getInt(CONFIGURATIONS_POS_COLUMN_HOST_PORT);
        byte[] hostNodeId = Base64.decode(cursor.getString(CONFIGURATIONS_POS_COLUMN_HOST_NODE_ID));
        String hostIp = cursor.getString(CONFIGURATIONS_POS_COLUMN_HOST_IP);
        int custPort = cursor.getInt(CONFIGURATIONS_POS_COLUMN_CUST_PORT);
        int nonCustPort = cursor.getInt(CONFIGURATIONS_POS_COLUMN_NON_CUST_PORT);
        int appPort = cursor.getInt(CONFIGURATIONS_POS_COLUMN_APP_PORT);
        float latitude = cursor.getLong(CONFIGURATIONS_POS_COLUMN_LATITUDE);
        float longitude = cursor.getLong(CONFIGURATIONS_POS_COLUMN_LONGITUDE);
        boolean selected = Boolean.valueOf(cursor.getString(CONFIGURATIONS_POS_COLUMN_SELECTED));
        String serverCertificate = String.valueOf(cursor.getString(CONFIGURATIONS_POS_COLUMN_CERTIFICATE));
        return new ProfServerData(hostNodeId, hostIp, hostPort, custPort, nonCustPort, appPort, selected, true, latitude, longitude, serverCertificate);
    }

}
