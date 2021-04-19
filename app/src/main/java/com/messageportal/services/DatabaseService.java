package com.messageportal.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import com.messageportal.entities.SMSEntity;
import com.messageportal.entities.SMSPreferencesEntity;
import com.messageportal.utils.Constants;
import com.messageportal.utils.Utils;

import java.io.File;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DatabaseService extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static String DB_DIR = "/data/data/android.example/databases/";
    private static String DB_NAME = "msgPortal.sqlite";
    private static String DB_PATH = DB_DIR + DB_NAME;
    private final Context myContext;

    public DatabaseService(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
        myContext = context;
        // Get the path of the database that is based on the context.
        DB_PATH = myContext.getDatabasePath(DB_NAME).getAbsolutePath();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createSMSTable = "CREATE TABLE IF NOT EXISTS sms(id INTEGER PRIMARY KEY, receiver TEXT NOT NULL, body TEXT NOT NULL, sent_date DATETIME NOT NULL, status TEXT, status_message TEXT)";
        db.execSQL(createSMSTable);
        String createSMSPrefTable = "CREATE TABLE IF NOT EXISTS sms_pref(id INTEGER PRIMARY KEY AUTOINCREMENT, sms_limit INTEGER NOT NULL, unlimited INTEGER NOT NULL, invoice_day INTEGER NOT NULL, device_port TEXT, sim INTEGER, auth_key TEXT NOT NULL)";
        db.execSQL(createSMSPrefTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //don't delete and re-do DB on upgrade, keep data
    }

    private void checkIfEmptyPrefTable(SQLiteDatabase db){
        String countQuery = "SELECT count(*) FROM sms_pref";
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        if(count > 0) {
            //leave, preferences already saved
        } else {
            //populate table with default values and auth key
            saveSMSPref(generateDefaultSettings());
        }
    }

    private SMSPreferencesEntity generateDefaultSettings(){
        SMSPreferencesEntity smsPreferencesEntity = new SMSPreferencesEntity();
        smsPreferencesEntity.setUnlimitedMessaging(false);
        smsPreferencesEntity.setSmsCreditsLimit(Utils.SMS_LIMIT);
        smsPreferencesEntity.setInvoiceDay(Utils.INVOICE_DAY);
        smsPreferencesEntity.setDevicePort(Utils.DEFAULT_PORT);
        //creating the SMS Pref object, generate auth key
        smsPreferencesEntity.setAuthKey(Utils.generateAuthKey());
        return smsPreferencesEntity;
    }

    /*For SMS sent through the gateway without an id parameter set, generate the next available id automatically
    * No autoincrement was used for this table since it allows to track the sms by id through other DBs.*/
    private Long generateNextSMSId(SQLiteDatabase db){
        String query = "SELECT MAX(id) AS max_id FROM sms";
        Cursor cursor = db.rawQuery(query, null);
        Long id = 0L;
        if (cursor.moveToFirst())
        {
            do
            {
                id = cursor.getLong(0);
                id = id + 1;
            } while(cursor.moveToNext());
        }
        return id;
    }

    public Long saveSMS(SMSEntity sms, SQLiteDatabase db) {
        Long smsId = 0L;
        try {
            ContentValues values = new ContentValues();
            if(sms.getId() != null) { //id is sent from the desktop gateway app or via http url to match DBs
                values.put("id", sms.getId());
                smsId = sms.getId();
            } else{

                Long nextID = generateNextSMSId(db);
                System.out.println("aiic___"+ nextID);
                values.put("id", nextID);
                smsId = nextID;
            }

            values.put("receiver", sms.getReceiver());
            values.put("body", sms.getBody());

            if (sms.getSentOn() != null) {
                values.put("sent_date", Utils.getStringFromDate(sms.getSentOn()));
            }
            values.put("status", sms.getStatus());
            values.put("status_message", sms.getStatusMessage());

            db.insert("sms", null, values);
        } catch(Exception ex){
            Log.e("saveSMS","Error saving the new SMS.", ex);
        }
        return smsId;
    }

    public void saveSMSPref(SMSPreferencesEntity smsPreferencesEntity) {
        try {
            SQLiteDatabase db  = this.getWritableDatabase();
            ContentValues values = new ContentValues();

            values.put("sms_limit", smsPreferencesEntity.getSmsCreditsLimit());

            int unlimited = 0; // false by default
            if (smsPreferencesEntity.isUnlimitedMessaging())
                unlimited = 1;
            values.put("unlimited", unlimited);

            values.put("invoice_day", smsPreferencesEntity.getInvoiceDay());
            values.put("device_port", smsPreferencesEntity.getDevicePort());
            values.put("auth_key", smsPreferencesEntity.getAuthKey());

            db.insert("sms_pref", null, values);
        }catch(Exception ex){
            Log.e("saveSMSPref","Error saving the new preferences.", ex);
        }
    }

    public List<SMSEntity> getAllSMSList(){
        List<SMSEntity> sentSMSList = new ArrayList<>();
        try {
            String selectQuery = "SELECT * FROM sms ORDER BY sent_date DESC";
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(selectQuery, null);

            if (cursor.moveToFirst()) {
                do {
                    SMSEntity sms = new SMSEntity();
                    sms.setId((long) cursor.getInt(0));
                    sms.setReceiver(cursor.getString(1));
                    sms.setBody(cursor.getString(2));
                    sms.setSentOn(Utils.getDateFromString(cursor.getString(3)));
                    sms.setStatus(cursor.getString(4));
                    sms.setStatusMessage(cursor.getString(5));
                    sentSMSList.add(sms);
                } while (cursor.moveToNext());
            }
        }catch(Exception ex){
            Log.e("getListSentSMS","Error getting the SMS list.", ex);
        }
        return sentSMSList;
    }

    public List<SMSEntity> getSentSMSList(){
        List<SMSEntity> sentSMSList = new ArrayList<>();
        try {
            String selectQuery = "SELECT * FROM sms WHERE status = '" + Constants.SMS_STATUS.SENT.getName()+ "' ORDER BY sent_date DESC";
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(selectQuery, null);

            if (cursor.moveToFirst()) {
                do {
                    SMSEntity sms = new SMSEntity();
                    sms.setId((long) cursor.getInt(0));
                    sms.setReceiver(cursor.getString(1));
                    sms.setBody(cursor.getString(2));
                    sms.setSentOn(Utils.getDateFromString(cursor.getString(3)));
                    sms.setStatus(cursor.getString(4));
                    sms.setStatusMessage(cursor.getString(5));
                    sentSMSList.add(sms);
                } while (cursor.moveToNext());
            }
        }catch(Exception ex){
            Log.e("getSentSMSList","Error getting the SMS list.", ex);
        }
        return sentSMSList;
    }

    public List<SMSEntity> getFailedSMSList(){
        List<SMSEntity> sentSMSList = new ArrayList<>();
        try {
            String selectQuery = "SELECT * FROM sms WHERE status = '" + Constants.SMS_STATUS.ERROR.getName()+ "' ORDER BY sent_date DESC";
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(selectQuery, null);

            if (cursor.moveToFirst()) {
                do {
                    SMSEntity sms = new SMSEntity();
                    sms.setId((long) cursor.getInt(0));
                    sms.setReceiver(cursor.getString(1));
                    sms.setBody(cursor.getString(2));
                    sms.setSentOn(Utils.getDateFromString(cursor.getString(3)));
                    sms.setStatus(cursor.getString(4));
                    sms.setStatusMessage(cursor.getString(5));
                    sentSMSList.add(sms);
                } while (cursor.moveToNext());
            }
        }catch(Exception ex){
            Log.e("getFailedSMSList","Error getting the SMS list.", ex);
        }
        return sentSMSList;
    }

    public List<SMSEntity> getSentSMSListWithinTime(int invoiceDay){
        List<SMSEntity> sentSMSListWithinTime = new ArrayList<>();
        try{
            Date currentMonth = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1);
            calendar.set(Calendar.DAY_OF_MONTH, invoiceDay);
            Date lastInvoiceDay = calendar.getTime();

            String selectQuery = "SELECT * FROM sms WHERE DATE(sent_date) BETWEEN '" + Utils.getStringFromDate(lastInvoiceDay)+ "' AND '" + Utils.getStringFromDate(currentMonth) + "' AND status = '" + Constants.SMS_STATUS.SENT.getName() + "' ORDER BY sent_date DESC";

            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(selectQuery, null);

            if (cursor.moveToFirst()) {
                do {
                    SMSEntity sms = new SMSEntity();
                    sms.setId((long)cursor.getInt(0));
                    sms.setReceiver(cursor.getString(1));
                    sms.setBody(cursor.getString(2));
                    sms.setSentOn(Utils.getDateFromString(cursor.getString(3)));
                    sms.setStatus(cursor.getString(4));
                    sms.setStatusMessage(cursor.getString(5));
                    sentSMSListWithinTime.add(sms);
                } while (cursor.moveToNext());
            }
        }catch(Exception ex){
            Log.e("sentSMSListWithinTime","Error getting the SMS list.", ex);
        }
        return sentSMSListWithinTime;
    }

    public void deleteSMS(Long id) {
        try{
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete("sms", "id= ?", new String[]{String.valueOf(id)});
        }catch(Exception ex){
            Log.e("deleteSMS","Error deleting the SMS with id:" + id, ex);
        }
    }

    public String getSMSStatusById(String id){
        Cursor cursor = null;
        SQLiteDatabase db = this.getReadableDatabase();
        String status = "";
        try {
            cursor = db.rawQuery("SELECT status FROM sms WHERE id=?", new String[] {id + ""});
            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                status = cursor.getString(cursor.getColumnIndex("status"));
            }
        }catch(Exception ex){
            Log.e("getSMSStatusById","Error getting the SMS with id:" + id, ex);
        } finally {
            if(cursor != null)
                cursor.close();
        }
        return status;
    }

    public String getSMSStatusMessageById(String id){

        Cursor cursor = null;
        SQLiteDatabase db = this.getReadableDatabase();
        String statusMsg = "";

        try {
            cursor = db.rawQuery("SELECT status_message FROM sms WHERE id=?", new String[] {id + ""});
            if(cursor.getCount() > 0) {
                cursor.moveToFirst();
                statusMsg = cursor.getString(cursor.getColumnIndex("status_message"));
            }
        }catch(Exception ex){
            Log.e("getSMSStatusMessageById","Error getting the SMS status message for SMS with id:" + id, ex);
        }finally {
            if(cursor != null)
                cursor.close();
        }
        return statusMsg;
    }

    public void updateSMSPref(SMSPreferencesEntity smsPreferencesEntity){
        try{
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("sms_limit", smsPreferencesEntity.getSmsCreditsLimit());

            int unlimited = 0; // false by default
            if(smsPreferencesEntity.isUnlimitedMessaging())
                unlimited = 1;
            values.put("unlimited", unlimited);

            values.put("invoice_day", smsPreferencesEntity.getInvoiceDay());
            values.put("device_port", smsPreferencesEntity.getDevicePort());

            db.update("sms_pref", values, "id = "+smsPreferencesEntity.getId(), null);
        }catch(Exception ex){
            Log.e("updateSMSPref","Error updating the sms preferences.", ex);
        }
    }

    public void updateSMSSIM(int simChoice, Long savedPrefID){
        try{
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("sim", simChoice);
            db.update("sms_pref", values, "id = " + savedPrefID, null);
        }catch(Exception ex){
            Log.e("updateSMSPref","Error updating the sms preferences.", ex);
        }
    }

    public SMSPreferencesEntity getSavedPreferences(){

        List<SMSPreferencesEntity> savedPref = new ArrayList<>();
        String selectQuery = "SELECT * FROM sms_pref";

        try{
            SQLiteDatabase db = this.getWritableDatabase();
            checkIfEmptyPrefTable(db);
            Cursor cursor = db.rawQuery(selectQuery, null);

            if (cursor.moveToFirst()) {
                do {
                    SMSPreferencesEntity smsPref = new SMSPreferencesEntity();
                    smsPref.setId((long)cursor.getInt(0));
                    smsPref.setSmsCreditsLimit(cursor.getInt(1));

                    if(cursor.getInt(2) == 0)
                        smsPref.setUnlimitedMessaging(false);
                    else if(cursor.getInt(2)== 1)
                        smsPref.setUnlimitedMessaging(true);

                    smsPref.setInvoiceDay(cursor.getInt(3));
                    smsPref.setDevicePort(cursor.getString(4));
                    if(cursor.getInt(5) != 0)
                        smsPref.setSimChoice(Constants.SIM.getNameByCode(cursor.getInt(5)));
                    smsPref.setAuthKey(cursor.getString(6));
                    savedPref.add(smsPref);
                } while (cursor.moveToNext());
            }
        }catch(Exception ex){
            Log.e("getSavedPreferences","Error getting the sms preferences.", ex);
        }
        if(savedPref != null && !savedPref.isEmpty())
            return savedPref.get(0);
        else
            return null;
    }

    public String getAuthKey(){
        String selectAuthQuery = "SELECT auth_key FROM sms_pref";

        try{
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(selectAuthQuery, null);
            String authKey = null;
            if (cursor.moveToFirst()) {
                do {
                    authKey = cursor.getString(0);
                } while (cursor.moveToNext());
            }
            return authKey;
        }catch(Exception ex){
            Log.e("getSavedPreferences","Error getting the authentication key", ex);
        }
        return null;
    }

    public Long saveOrUpdateSMS(SMSEntity smsEntity){
        boolean isSMSSaved = false;
        SQLiteDatabase db = this.getWritableDatabase();
        Long smsId = 0L;

        if(smsEntity.getId() != null) {
            Cursor cursor = null;
            cursor = db.rawQuery("SELECT * FROM sms WHERE id=?", new String[]{smsEntity.getId() + ""});

            if (cursor.getCount() > 0) {
                cursor.close();
                isSMSSaved = true;
            }
            cursor.close();
        }

        //if exists update only the new info
        if(isSMSSaved) {
            smsId = smsEntity.getId();
            ContentValues values = new ContentValues();
            if (smsEntity.getSentOn() != null) {
                values.put("sent_date", Utils.getStringFromDate(smsEntity.getSentOn()));
            }
            values.put("status", smsEntity.getStatus());
            values.put("status_message", smsEntity.getStatusMessage());
            db.update("sms", values, "id = " + smsEntity.getId(), null);
        } else{
            smsId = saveSMS(smsEntity, db);
        }
        return smsId;
    }
}
