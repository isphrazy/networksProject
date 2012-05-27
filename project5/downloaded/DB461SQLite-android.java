package edu.uw.cs.cse461.sp12.DB461;

import java.io.File;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import edu.uw.cs.cse461.sp12.DB461.DB461;
import edu.uw.cs.cse461.sp12.DB461.DB461.DB461Exception;
import edu.uw.cs.cse461.sp12.DB461.DB461.DBRecordIterator;
import edu.uw.cs.cse461.sp12.OS.ContextManager;
import edu.uw.cs.cse461.sp12.util.Log;

//----------------------------------------
// Android version
//----------------------------------------

/**
 * Base class for Android SQLite DBs.
 * Handles generic operations, using DB descriptor passed in by DB client.
 * @author zahorjan
 *
 */
public class DB461SQLite extends DB461 {
	private static final String TAG="DB461(Console)";
	public static final int DBVERSION = 1;
	
	private SQLiteDatabase mDB;
	
	public class AndroidRecordIterator implements DBRecordIterator {
		private Cursor mSt;
		AndroidRecordIterator(Cursor st) { mSt = st; }
		public int getInt(int column) throws SQLiteException { return mSt.getInt(column); }
		public double getDouble(int column) throws SQLiteException { return mSt.getDouble(column); }
		public String getString(int column) throws SQLiteException { return mSt.getString(column); }
		public byte[] getBlob(int column) throws SQLiteException { return mSt.getBlob(column); }
		public boolean next() throws SQLiteException { return mSt.moveToNext(); }
		public void close() { mSt.close(); }
	}


	public DB461SQLite(String dbName) throws DB461Exception {
		super(dbName);
	}
	
	@Override
	public boolean dbExists() {
		File dbFile = new File(mDBName);
		return dbFile.exists();
	}

	private class DBOpenHelper extends SQLiteOpenHelper {
		public boolean needCreate;
		
		public DBOpenHelper(String dbName) {
			super(ContextManager.getContext(), dbName, null, DBVERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			needCreate = true;
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

	@Override
	public void openOrCreateDatabase() throws Exception {
		DBOpenHelper dbHelper = new DBOpenHelper(mDBName);
		mDB = dbHelper.getWritableDatabase();
		if ( dbHelper.needCreate ) _createTables();
	}

	@Override
	public void close() {
		if ( mDB != null ) mDB.close();
		mDB = null;
	}
	
	@Override
	public void exec(String query) throws Exception {
		if ( mDB == null ) throw new DB461Exception("DBSQLite.exec(" + query + "): db is not open");
		try {
			mDB.execSQL(query);
		} catch (SQLiteException e) {
			Log.e(TAG, "SQLiteException on query '" + query + "': " + e.getMessage());
			throw new Exception("SQLiteException on query '" + query + "': " + e.getMessage());
		}
	}
	
	/**
	 * Query method that returns a system-specific iterator type to access results.
	 * @param query
	 * @return
	 * @throws Exception
	 */
	@Override
	protected DBRecordIterator _queryTable(String query) throws DB461Exception {
		if ( mDB == null ) throw new DB461Exception("DB461SQLite._queryTable(" + query + "): db is not open");
		Cursor cursor = mDB.rawQuery(query, null);
		return new AndroidRecordIterator(cursor);
	}
	
}
