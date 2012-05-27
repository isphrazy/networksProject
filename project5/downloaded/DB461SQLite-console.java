package edu.uw.cs.cse461.sp12.DB461;

import java.io.File;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

import edu.uw.cs.cse461.sp12.DB461.DB461;
import edu.uw.cs.cse461.sp12.util.Log;

//----------------------------------------
// sqlite4java version
//----------------------------------------

/**
 * Base class for console SQLite DBs.
 * Handles generic operations, using DB descriptor passed in by DB client.
 * @author zahorjan
 *
 */
public class DB461SQLite extends DB461 {
	private static final String TAG="DB461(Console)";
	
	protected SQLiteConnection mDB;
	
	public class ConsoleRecordIterator implements DBRecordIterator {
		private SQLiteStatement mSt;
		ConsoleRecordIterator(SQLiteStatement st) { mSt = st; }
		public int getInt(int column) throws SQLiteException { return mSt.columnInt(column); }
		public double getDouble(int column) throws SQLiteException { return mSt.columnDouble(column); }
		public String getString(int column) throws SQLiteException { return mSt.columnString(column); }
		public byte[] getBlob(int column) throws SQLiteException { return mSt.columnBlob(column); }
		public boolean next() throws SQLiteException { return mSt.step(); }
		public void close() { mSt.dispose(); }
	}


	public DB461SQLite(String dbName) throws DB461Exception {
		super(dbName);
		try {
			// turn off annoying sqlite4java debug output
			java.util.logging.Logger.getLogger("com.almworks.sqlite4java").setLevel(java.util.logging.Level.WARNING);
		} catch (Exception e) {
			String msg = "Constructor caught exception\n" + e.getMessage(); 
			Log.e(TAG, msg);
			throw new DB461Exception(msg);
		}
	}
	
	@Override
	public boolean dbExists() {
		File dbFile = new File(mDBName);
		return dbFile.exists();
	}


	@Override
	public void openOrCreateDatabase() throws Exception {
		boolean doCreateTables = !dbExists();
		mDB = new SQLiteConnection(new File(mDBName));
		mDB.open(true);
		if ( doCreateTables ) _createTables();
	}

	@Override
	public void close() {
		if ( mDB != null ) mDB.dispose();
		mDB = null;
	}
	
	@Override
	public void exec(String query) throws Exception { 
		if ( mDB == null ) throw new Exception("DB461SQLite.query(" + query + "): db is not open");
		try {
			mDB.exec(query);
		} catch (SQLiteException e) {
			Log.e(TAG, "SQLiteException on exec(" + query + "): " + e.getMessage());
			throw new Exception("SQLiteException on exec(" + query + "): " + e.getMessage());
		}
	}
	
	@Override
	public DBRecordIterator _queryTable(String query) throws DB461Exception {
		if ( mDB == null ) throw new DB461Exception("DB461SQLite.query(" + query + "): db is not open");
		try {
			return new ConsoleRecordIterator(mDB.prepare(query));
		} catch (SQLiteException e) {
			Log.e(TAG, "SQLiteException on query '" + query + "': " + e.getMessage());
			throw new DB461Exception("SQLiteException on query '" + query + "': " + e.getMessage());
		}
	}
}
