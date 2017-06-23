package iop.org.iop_sdk_android.core.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.fermat.redtooth.core.services.pairing.PairingMsgTypes;
import org.fermat.redtooth.profile_server.ProfileInformation;
import org.fermat.redtooth.profiles_manager.PairingRequest;
import org.fermat.redtooth.profiles_manager.PairingRequestsManager;

import java.util.List;

/**
 * Created by furszy on 6/6/17.
 */

public class SqlitePairingRequestDb extends AbstractSqliteDb<PairingRequest> implements PairingRequestsManager {

    public static final int DATABASE_VERSION = 2;

    public static final String DATABASE_NAME = "requests";
    public static final String CONTACTS_TABLE_NAME = "pairing_request";
    public static final String CONTACTS_COLUMN_ID = "id";
    public static final String CONTACTS_COLUMN_SENDER_KEY = "senderPubKey";
    public static final String CONTACTS_COLUMN_REMOTE_KEY = "remotePubKey";
    public static final String CONTACTS_COLUMN_REMOTE_SERVER_ID = "remoteServerId";
    public static final String CONTACTS_COLUMN_REMOTE_SERVER_HOST_ID = "remoteHost";
    public static final String CONTACTS_COLUMN_SENDER_NAME = "senderName";
    public static final String CONTACTS_COLUMN_TIMESTAMP = "timestamp";
    public static final String CONTACTS_COLUMN_STATUS = "status";

    public static final int CONTACTS_COLUMN_POS_ID = 0;
    public static final int CONTACTS_COLUMN_POS_SENDER_KEY = 1;
    public static final int CONTACTS_COLUMN_POS_REMOTE_KEY = 2;
    public static final int CONTACTS_COLUMN_POS_REMOTE_SERVER_ID = 3;
    public static final int CONTACTS_COLUMN_POS_REMOTE_SERVER_HOST_ID = 4;
    public static final int CONTACTS_COLUMN_POS_SENDER_NAME = 5;
    public static final int CONTACTS_COLUMN_POS_TIMESTAMP = 6;
    public static final int CONTACTS_COLUMN_POS_STATUS = 7;

    public SqlitePairingRequestDb(Context context) {
        super(context, DATABASE_NAME , null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                "create table " +CONTACTS_TABLE_NAME+
                        "(" +
                        CONTACTS_COLUMN_ID + " INTEGER primary key autoincrement, "+
                        CONTACTS_COLUMN_SENDER_KEY + " TEXT, "+
                        CONTACTS_COLUMN_REMOTE_KEY + " TEXT, "+
                        CONTACTS_COLUMN_REMOTE_SERVER_ID + " TEXT, "+
                        CONTACTS_COLUMN_REMOTE_SERVER_HOST_ID + " TEXT, "+
                        CONTACTS_COLUMN_SENDER_NAME + " TEXT, "+
                        CONTACTS_COLUMN_TIMESTAMP + " LONG , "+
                        CONTACTS_COLUMN_STATUS + " TEXT "
                        +")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS "+CONTACTS_TABLE_NAME);
        onCreate(db);
    }

    @Override
    String getTableName() {
        return CONTACTS_TABLE_NAME;
    }

    @Override
    public ContentValues buildContent(PairingRequest obj) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CONTACTS_COLUMN_SENDER_KEY,obj.getSenderPubKey());
        contentValues.put(CONTACTS_COLUMN_REMOTE_KEY,obj.getRemotePubKey());
        if (obj.getRemoteServerId()!=null)
            contentValues.put(CONTACTS_COLUMN_REMOTE_SERVER_ID,obj.getRemoteServerId());
        contentValues.put(CONTACTS_COLUMN_SENDER_NAME,obj.getSenderName());
        contentValues.put(CONTACTS_COLUMN_TIMESTAMP,obj.getTimestamp());
        contentValues.put(CONTACTS_COLUMN_STATUS,obj.getStatus().getType());
        if (obj.getRemotePubKey()!=null)
            contentValues.put(CONTACTS_COLUMN_REMOTE_SERVER_HOST_ID,obj.getRemoteHost());
        return contentValues;
    }

    @Override
    public PairingRequest buildFrom(Cursor cursor) {
        int id = cursor.getInt(CONTACTS_COLUMN_POS_ID);
        String senderKey = cursor.getString(CONTACTS_COLUMN_POS_SENDER_KEY);
        String senderName = cursor.getString(CONTACTS_COLUMN_POS_SENDER_NAME);
        String remoteKey = cursor.getString(CONTACTS_COLUMN_POS_REMOTE_KEY);
        String remoteServerId = cursor.getString(CONTACTS_COLUMN_POS_REMOTE_SERVER_ID);
        long timestamp = cursor.getLong(CONTACTS_COLUMN_POS_TIMESTAMP);
        PairingMsgTypes status = PairingMsgTypes.getByName(cursor.getString(CONTACTS_COLUMN_POS_STATUS));
        String remotePsHost = cursor.getString(CONTACTS_COLUMN_POS_REMOTE_SERVER_HOST_ID);
        return new PairingRequest(id,senderKey,remoteKey,remoteServerId,remotePsHost,senderName,timestamp,status);
    }

    @Override
    public int savePairingRequest(PairingRequest pairingRequest) {
        return (int) insert(pairingRequest);
    }
    @Override
    public int saveIfNotExistPairingRequest(PairingRequest pairingRequest) {
        if(getPairingRequest(pairingRequest.getSenderPubKey(),pairingRequest.getRemotePubKey())!=null) return 0;
        return (int) insert(pairingRequest);
    }

    @Override
    public PairingRequest getPairingRequest(String senderPubKey,String remotePubkey) {
        // todo: do this ok with both values for more apps and not just one..
        return get(CONTACTS_COLUMN_REMOTE_KEY,remotePubkey);
    }

    @Override
    public List<PairingRequest> openPairingRequests(String senderPubKey) {
        return list();
    }

    @Override
    public boolean updateStatus(String senderPubKey,String remotePubKey,PairingMsgTypes status){
        return updateFieldByKey(CONTACTS_COLUMN_REMOTE_KEY,remotePubKey,CONTACTS_COLUMN_STATUS,status.getType())==1;
    }

    @Override
    public int removeRequest(String senderPubKey, String remotePubkey) {
        // todo: do this ok with both values for more apps and not just one..
        return delete(CONTACTS_COLUMN_REMOTE_KEY,remotePubkey);
    }

}
