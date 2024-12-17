package com.example.f1app;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DBHandler extends SQLiteOpenHelper {

    private static final String TAG = "DBHandlerERROR";
    private static String DB_PATH = " ";

    private static String DB_NAME = "f1db.db";

    private static final int DB_VERSION = 1;

    // drivers table in sqlite db
    static final String DRIVERS_TABLE_NAME = "drivers";

    static final String DRIVERS_ID_COL = "id";

    static final String DRIVERS_NAME_COL = "driverName";

    static final String DRIVERS_NUMBER_COL = "permanentNumber";

    static final String DRIVERS_CODE_COL = "driversCode";

    static final String DRIVERS_WINS_COL = "totalWins";

    static final String DRIVERS_PODIUM_COL = "totalPodiums";

    static final String DRIVERS_CHAMPIONSHIP_COL = "championshipsCount";

    static final String DRIVERS_POINTS_COL = "totalPoints";

    static final String DRIVERS_FIRSTENTRY_COL = "firstEntry";

    static final String DRIVERS_LASTENTRY_COL = "lastEntry";

    static final String DRIVERS_GPENTERED_COL = "gpEntered";

    static final String DRIVERS_BIRTHDAY_COL = "birthdayDate";

    static final String DRIVERS_NATIONALITY_COL = "nationality";

    static final String DRIVERS_COUNTRY_COL = "driverCountry";

    private final String DRIVERS_CURRENTTEAM_COL = "driversTeam";

    //circuits table in sqlite db
    static final String CIRCUITS_TABLE_NAME = "circuits";

    static final String CIRCUITS_ID_COL = "id";

    static final String CIRCUITS_NAME_COL = "circuitName";

    static final String CIRCUITS_CIRCUITID_COL = "circuitId";

    static final String CIRCUITS_LOCATION_COL = "location";

    static final String CIRCUITS_COUNTRY_COL = "country";

    static final String CIRCUITS_OPENED_COL = "opened";

    static final String CIRCUITS_FIRSTGP_COL = "firstGPyear";

    static final String CIRCUITS_LENGTH_COL = "length";

    static final String CIRCUITS_ALTITUDE_COL = "altitude";

    static final String CIRCUITS_TOTALLAPS_COL = "lapsCount";

    static final String CIRCUITS_DISTANCE_COL = "raceDistance";

    static final String CIRCUITS_RECORDTIME_COL = "lapRecordTime";

    static final String CIRCUITS_RECORDYEAR_COL = "lapRecordYear";

    static final String CIRCUITS_RECORDDRIVER_COL = "lapRecordDriver";

    static final String CIRCUITS_TURNSCOUNT_COL = "turnsCount";


    //results table in sqlite db
    static final String RESULTS_TABLE_NAME = "results";

    static final String RESULTS_ID_COL = "id";


    private Context mContext;


    public DBHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.mContext = context;
        DB_PATH = mContext.getDatabasePath(DB_NAME).getPath();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {    }

    public void addNewDriver(String driverName, String permanentNumber, String totalWins, String totalPodiums,
                             String totalPoints, String firstEntry, String lastEntry, String gpEntered,
                             String birthdayDate, String nationality, String driverCountry,
                             String driversCode, String currentTeam, String championshipsCount) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(DRIVERS_NAME_COL, driverName);
        values.put(DRIVERS_NUMBER_COL, permanentNumber);
        values.put(DRIVERS_CHAMPIONSHIP_COL, championshipsCount);
        values.put(DRIVERS_CODE_COL, driversCode);
        values.put(DRIVERS_WINS_COL, totalWins);
        values.put(DRIVERS_PODIUM_COL, totalPodiums);
        values.put(DRIVERS_POINTS_COL, totalPoints);
        values.put(DRIVERS_FIRSTENTRY_COL, firstEntry);
        values.put(DRIVERS_LASTENTRY_COL, lastEntry);
        values.put(DRIVERS_GPENTERED_COL, gpEntered);
        values.put(DRIVERS_BIRTHDAY_COL, birthdayDate);
        values.put(DRIVERS_NATIONALITY_COL, nationality);
        values.put(DRIVERS_COUNTRY_COL, driverCountry);
        values.put(DRIVERS_CURRENTTEAM_COL, currentTeam);

        db.insert(DRIVERS_TABLE_NAME, null, values);

        db.close();
    }

    public void addNewResults(HashMap<String, HashMap<String, String>> results){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        for (Map.Entry<String, HashMap<String, String>> entry : results.entrySet()){
            HashMap<String, String> list = entry.getValue();
            String driverName = entry.getKey();
            values.put("driverName", driverName);
            Log.i("check_gow_its_working", " " + driverName);
            for (Map.Entry<String, String> value : list.entrySet()){
                for (int i = 0; i < list.size(); i++){
                    String data = value.getValue();
                    String dataKey = value.getKey();
                    values.put("'" + dataKey + "'", data);
                }
            }
        }

        db.insert(RESULTS_TABLE_NAME, null, values);
        db.close();
    }


    public void addNewCircuit(String circuitName, String circuitId, String location, String country,
                              String opened, String firstGPyear, String length, String altitude, String lapsCount,
                              String raceDistance, String lapRecordTime, String lapRecordYear,
                              String lapRecordDriver, String turnsCount) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(CIRCUITS_NAME_COL, circuitName);
        values.put(CIRCUITS_CIRCUITID_COL, circuitId);
        values.put(CIRCUITS_LOCATION_COL, location);
        values.put(CIRCUITS_COUNTRY_COL, country);
        values.put(CIRCUITS_OPENED_COL, opened);
        values.put(CIRCUITS_FIRSTGP_COL, firstGPyear);
        values.put(CIRCUITS_LENGTH_COL, length);
        values.put(CIRCUITS_ALTITUDE_COL, altitude);
        values.put(CIRCUITS_TOTALLAPS_COL, lapsCount);
        values.put(CIRCUITS_DISTANCE_COL, raceDistance);
        values.put(CIRCUITS_RECORDTIME_COL, lapRecordTime);
        values.put(CIRCUITS_RECORDYEAR_COL, lapRecordYear);
        values.put(CIRCUITS_RECORDDRIVER_COL, lapRecordDriver);
        values.put(CIRCUITS_TURNSCOUNT_COL, turnsCount);

        db.insert(CIRCUITS_TABLE_NAME, null, values);

        db.close();
    }

    public void createDataBase() throws IOException
    {

        boolean mDataBaseExist = checkDataBase();
        if(!mDataBaseExist)
        {
            try
            {
                //Copy the database from assests
                copyDataBase();
                Log.e(TAG, "createDatabase database created");
            }
            catch (IOException mIOException)
            {
                mIOException.printStackTrace(); //<<<<<<<<<< might as well include the actual cause in the log
                throw new Error("ErrorCopyingDataBase");
            }
        }
    }

    private boolean checkDataBase()
    {
        File dbFile = new File(DB_PATH);
        if (dbFile.exists()) { Log.i("DB_CHECK", "DB EXIST"); return true; }
        if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();
        if (new File(DB_PATH + "-shm").exists())
            new File(DB_PATH + "-shm").delete();
        if ((new File(DB_PATH + "-wal")).exists())
            new File(DB_PATH + "-wal").delete();
        return false;
    }


    private void copyDataBase() throws IOException
    {
        InputStream mInput = mContext.getAssets().open(DB_NAME);
        String outFileName = DB_PATH; //<<<<<<<<<< just the path used
        OutputStream mOutput = new FileOutputStream(outFileName);
        byte[] mBuffer = new byte[1024];
        int mLength;
        while ((mLength = mInput.read(mBuffer))>0)
        {
            mOutput.write(mBuffer, 0, mLength);
        }
        mOutput.flush();
        mOutput.close();
        mInput.close();
    }

    public void deleteTableResults(){
        SQLiteDatabase db = this.getWritableDatabase();
        String row = "DELETE FROM " + RESULTS_TABLE_NAME;
        db.execSQL(row);
    }

    public void createTable(){
        SQLiteDatabase db = this.getWritableDatabase();
        String row = "CREATE TABLE IF NOT EXISTS " + RESULTS_TABLE_NAME + " ( " + RESULTS_ID_COL
                + " INTEGER PRIMARY KEY AUTOINCREMENT, driverName TEXT NOT NULL UNIQUE)";
        db.execSQL(row);
    }



    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void setCollumnsResults(ArrayList<String> raceNameList){
        SQLiteDatabase db = this.getWritableDatabase();
        for (int i = 0; i < raceNameList.size(); i++){
            db.execSQL("ALTER TABLE " + RESULTS_TABLE_NAME + " ADD COLUMN '" + raceNameList.get(i) + "'");
        }
    }

    public void updateDriverInfo(String driverName, String totalWins, String totalPoints,
                                 String countGPEntered, String lastDriversGP, String podiumCount,
                                 String currentTeam){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + DRIVERS_TABLE_NAME + " SET " + DRIVERS_WINS_COL + " = '" + totalWins +
                "', " + DRIVERS_POINTS_COL + " = '" + totalPoints + "', " + DRIVERS_GPENTERED_COL + " = '" +
                countGPEntered + "', " + DRIVERS_LASTENTRY_COL + " = '" + lastDriversGP + "', " +
                DRIVERS_PODIUM_COL + " = '" + podiumCount + "', " + DRIVERS_CURRENTTEAM_COL + " = '" + currentTeam +
                "' WHERE " + DRIVERS_NAME_COL + " LIKE '" + driverName + "'");
    }

    public void updateCircuitInfo(String circuitId, String lapRecordTime, String lapRecordYear,
                                 String lapRecordDriver){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + CIRCUITS_TABLE_NAME + " SET " + CIRCUITS_RECORDTIME_COL + " = '" + lapRecordTime +
                "', " + CIRCUITS_RECORDDRIVER_COL + " = '" + lapRecordDriver + "', " + CIRCUITS_RECORDYEAR_COL + " = '" +
                lapRecordYear +  "' WHERE " + CIRCUITS_CIRCUITID_COL + " LIKE '" + circuitId + "'");
    }

    public void updateResultsInfo(String driverName, String result, String raceName){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + RESULTS_TABLE_NAME + " SET '" + raceName + "' = '" + result
                +  "' WHERE driverName LIKE '" + driverName + "'");
        db.execSQL("UPDATE " + RESULTS_TABLE_NAME + " SET '" + raceName + "' = 'NP' " +
                "WHERE '" + raceName + "' IS NULL OR '" + raceName + "' = ' '");
    }

    @SuppressLint("Range")
    public ArrayList<HashMap<String, String>> getDriver(String driverName){
        ArrayList<HashMap<String, String>> arrayList = new ArrayList<HashMap<String, String>>();

        String selectQuery = "SELECT  * FROM " + DRIVERS_TABLE_NAME + " WHERE " + DRIVERS_NAME_COL + " LIKE '" + driverName + "'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        cursor.moveToFirst();

        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> datanum = new HashMap<String, String>();
                datanum.put("driverName", cursor.getString(cursor.getColumnIndex(DRIVERS_NAME_COL)));
                datanum.put("totalWins", cursor.getString(cursor.getColumnIndex(DRIVERS_WINS_COL)));
                datanum.put("totalPoints", cursor.getString(cursor.getColumnIndex(DRIVERS_POINTS_COL)));
                datanum.put("totalPodiums", cursor.getString(cursor.getColumnIndex(DRIVERS_PODIUM_COL)));
                datanum.put("lastGP", cursor.getString(cursor.getColumnIndex(DRIVERS_LASTENTRY_COL)));
                datanum.put("enteredGPcount", cursor.getString(cursor.getColumnIndex(DRIVERS_GPENTERED_COL)));
                datanum.put("lastTeam", cursor.getString(cursor.getColumnIndex(DRIVERS_CURRENTTEAM_COL)));
                datanum.put("permanentNumber", cursor.getString(cursor.getColumnIndex(DRIVERS_NUMBER_COL)));
                datanum.put("driverCode", cursor.getString(cursor.getColumnIndex(DRIVERS_CODE_COL)));
                datanum.put("championshipCount", cursor.getString(cursor.getColumnIndex(DRIVERS_CHAMPIONSHIP_COL)));
                datanum.put("firstGP", cursor.getString(cursor.getColumnIndex(DRIVERS_FIRSTENTRY_COL)));
                datanum.put("birthdayDate", cursor.getString(cursor.getColumnIndex(DRIVERS_BIRTHDAY_COL)));
                datanum.put("driverNationality", cursor.getString(cursor.getColumnIndex(DRIVERS_NATIONALITY_COL)));
                datanum.put("driverCountry", cursor.getString(cursor.getColumnIndex(DRIVERS_COUNTRY_COL)));

                arrayList.add(datanum);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return arrayList;
    }

    @SuppressLint("Range")
    public ArrayList<HashMap<String, String>> getCircuit(String circuitId){
        ArrayList<HashMap<String, String>> arrayList = new ArrayList<HashMap<String, String>>();

        String selectQuery = "SELECT  * FROM " + CIRCUITS_TABLE_NAME + " WHERE " + CIRCUITS_CIRCUITID_COL + " LIKE '" + circuitId + "'";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        cursor.moveToFirst();

        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> datanum = new HashMap<String, String>();
                datanum.put("circuitName", cursor.getString(cursor.getColumnIndex(CIRCUITS_NAME_COL)));
                datanum.put("circuitId", cursor.getString(cursor.getColumnIndex(CIRCUITS_CIRCUITID_COL)));
                datanum.put("location", cursor.getString(cursor.getColumnIndex(CIRCUITS_LOCATION_COL)));
                datanum.put("country", cursor.getString(cursor.getColumnIndex(CIRCUITS_COUNTRY_COL)));
                datanum.put("opened", cursor.getString(cursor.getColumnIndex(CIRCUITS_OPENED_COL)));
                datanum.put("firstGPyear", cursor.getString(cursor.getColumnIndex(CIRCUITS_FIRSTGP_COL)));
                datanum.put("length", cursor.getString(cursor.getColumnIndex(CIRCUITS_LENGTH_COL)));
                datanum.put("altitude", cursor.getString(cursor.getColumnIndex(CIRCUITS_ALTITUDE_COL)));
                datanum.put("lapsCount", cursor.getString(cursor.getColumnIndex(CIRCUITS_TOTALLAPS_COL)));
                datanum.put("raceDistance", cursor.getString(cursor.getColumnIndex(CIRCUITS_DISTANCE_COL)));
                datanum.put("lapRecordTime", cursor.getString(cursor.getColumnIndex(CIRCUITS_RECORDTIME_COL)));
                datanum.put("lapRecordYear", cursor.getString(cursor.getColumnIndex(CIRCUITS_RECORDYEAR_COL)));
                datanum.put("lapRecordDriver", cursor.getString(cursor.getColumnIndex(CIRCUITS_RECORDDRIVER_COL)));
                datanum.put("turnsCount", cursor.getString(cursor.getColumnIndex(CIRCUITS_TURNSCOUNT_COL)));
                arrayList.add(datanum);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return arrayList;
    }
}