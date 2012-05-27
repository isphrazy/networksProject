package cse461.snet.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

/**
 * Base class for 461 Databases.  This class provides those operations
 * that can sensibly be implemented in a system independent way.
 * @author zahorjan
 *
 */
public abstract class DB461 {
	private static final String TAG="DB461";
	
	protected String mDBName;
	protected HashMap<String, DB461Table> mTableMap;
	
	abstract public boolean dbExists();
	abstract public void openOrCreateDatabase() throws Exception;
	abstract public void exec(String query) throws Exception;  // for operating on the DB, but not on a particular table
	abstract public void close();
	
	abstract protected DBRecordIterator _queryTable(String query) throws DB461Exception;

	//-------------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------------

	/**
	 * Base class for a DB table.  The type parameter, E, is the type of the key field.
	 * @author zahorjan
	 *
	 * @param <K> The type of the key field.  (Only one key field is allowed.)
	 * @param <R> The type of the Record for this table
	 */
	public abstract class DB461Table<K, R extends Record> {
		final String    	mName;
		final String[][]	mDBDescriptor;
		final String        mKeyFieldName;

		public DB461Table(String name, String[][] descriptor, String keyFieldName) {
			mName = name;
			mDBDescriptor = descriptor;
			mKeyFieldName = keyFieldName;
		}
		
		public abstract RecordSet<R> createRecordSet();  // new's up a RecordSet<R> 
		public abstract R	    	 createRecord();     // new's up a Record of type R for the specific subclass table
		
		public abstract void write(R record) throws DB461Exception;  // If record's key matches a record in the DB, record replaces it.
																	 // Otherwise, record is added to the DB.
		
		/**
		 * Returns the single record matching the keyVal, or null if no match
		 * @param keyVal The key value to match.
		 * @return A single record, or null if not matching record.
		 * @throws DB461Exception There is more than one match, or some error reading the DB.
		 */
		public R readOne(K keyVal) throws DB461Exception {
			String query = "SELECT * FROM " + name() + " WHERE " + mKeyFieldName + "=";
			if ( keyVal.getClass() == String.class ) query += "'" + keyVal.toString() + "'";
			else query += keyVal;
			RecordSet<R> result = (RecordSet<R>)query(query);
			if ( result.size() <= 0 ) return null;
			if ( result.size() > 1 ) throw new DB461Exception("Found " + result.size() + " records matching " + mKeyFieldName + " = " + keyVal);
			return result.get(0);
		}
		
		/**
		 * Returns the entire table
		 * @return All records of the tables
		 * @throws DB461Exception
		 */
		public RecordSet<R> readAll() throws DB461Exception {
			return (RecordSet<R>)query("SELECT * FROM " + name());
		}
		
		
		/**
		 * Handles a query that involves only a single table and normally returns records.
		 * @param table
		 * @param query
		 * @return
		 * @throws DB461Exception
		 */
		protected RecordSet<R> query(String query) throws DB461Exception {
			RecordSet<R> result = createRecordSet();
			DBRecordIterator it = _queryTable(query);
			try {
				it = _queryTable(query);
				while(it.next()) {
					result.add( (R)createRecord().initialize(it) );
				}
			} catch (Exception e) {
				String msg = "Exception on query(" + query + "): " + e.getMessage();
				Log.e(TAG, msg);
				e.printStackTrace();
				throw new DB461Exception(msg);
			} finally {
				if ( it != null ) it.close();
			}
			return result;
		}
		
		/**
		 * Deletes the record with matching the argument value, if there is such a record.
		 * @param keyVal  The key value.
		 */
		public void delete(K keyVal) {
			try {
				String query = "DELETE FROM " + name() + " WHERE " + mKeyFieldName + "=";
				if ( keyVal.getClass() == String.class ) query += "'" + keyVal.toString() + "'";
				else query += keyVal;
				query(query);
			} catch (Exception e) {
				// hopefully this means the record doesn't exist, which is fine...
			}
		}
		
		String[][] fields() {
			return mDBDescriptor;
		}

		/**
		 * Gives this table's name (in the DB).
		 * @return
		 */
		public String name() { return mName; }
	}
	//-------------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------------
	
	
	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------
	// Utility classes.
	
	/**
	 * Lets us distinguish exceptions thrown by our own code, if we want.
	 * @author zahorjan
	 *
	 */
	static public class DB461Exception extends Exception {
		public DB461Exception(String msg) { super(msg); }
	}
	
	/**
	 * Base class for records.
	 * Subclasses: FriendRecord and PhotoRecord
	 * @author zahorjan
	 *
	 */
	 public abstract class Record {	
		 /**
		  * Sets record fields using DB record provided by the DB iterator.
		  * @param it
		  * @return
		  */
		 protected abstract Record initialize(DBRecordIterator it) throws DB461Exception; 
	 }

	/**
	 * A possibly-empty vector of records.
	 * 
	 * @author zahorjan
	 */
	static public class RecordSet<E> extends Vector<E> {
		private static final long serialVersionUID = 1L;
		public static final char SEP = '\n';
		
		/**
		 * Returns string representation of all records.
		 */
		@Override
		synchronized public String toString() {
			if ( this.size() <= 0 ) return "(empty)";
			StringBuilder sb = new StringBuilder();
			Iterator<E> it = iterator();
			sb.append(it.next());
			while( it.hasNext() ) {
				sb.append(SEP).append(it.next());
			}
			return sb.toString();
		}
	}
	
	/**
	 * Abstract iterator over records in a query result.  (NOT an iterator
	 * over a RecordSet - just use a Vector iterator for that.)
	 * Concrete class depends on system type.
	 * @author zahorjan
	 *
	 */
	public interface DBRecordIterator {
		boolean next() throws Exception;  // moves to next record.  Returns false if there is no next record
		void close();

		int getInt(int column) throws Exception;
		double getDouble(int column) throws Exception;
		String getString(int column) throws Exception;
		byte[] getBlob(int column) throws Exception;
	}

	// Utility classes.
	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------
		

	protected DB461(String dbName) throws DB461Exception {
		mDBName = dbName;
		mTableMap = new HashMap<String, DB461Table>();
	}
	
	protected DB461 addTable(DB461Table table) {
		mTableMap.put(table.name(), table);
		return this;
	}

	/**
	 * Returns names of tables in this db.
	 * @return
	 */
	Set<String> tables() { return mTableMap.keySet(); }
	
	/**
	 * Creates tables described in the db descriptor.  The first field in each table's descriptor is used as a primary key.
	 */
	protected void _createTables() throws Exception {
		for ( String tablename : tables() ) {
			String[][] fields = mTableMap.get(tablename).fields();
			StringBuilder sb = new StringBuilder().append("CREATE TABLE IF NOT EXISTS ").append(tablename).append(" (")
												  .append(fields[0][0]).append(" ").append(fields[0][1]).append(" PRIMARY KEY");
			for ( int i=1; i<fields.length; i++) {
				sb.append(", ").append(fields[i][0]).append(" ").append(fields[i][1]);
			}
			sb.append(")");
			exec(sb.toString());
		}
	}

	/**
	 * Formats all records in the db.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder().append("DB: ").append(mDBName).append("\n");
		for (String tablename : tables() ) {
			sb.append("\t").append(tablename).append("\n");
			String[][] fields = mTableMap.get(tablename).fields();
			String query = "SELECT * FROM " + tablename;
			try {
			DBRecordIterator it = null;
			try {
				it = _queryTable(query);
				while ( it.next() ) {
					sb.append("\t\t[");
					for ( int column=0; column<fields.length; column++ ) {
						sb.append(fields[column][0]).append(":  ");
						String type = fields[column][1].toUpperCase();
						if ( type.equals("INTEGER"))	sb.append(it.getInt(column));
						else if ( type.equals("REAL"))  sb.append(String.format("%5.3f", it.getDouble(column)));
						else if ( type.equals("TEXT") ) sb.append(it.getString(column));
						else if ( type.equals("BLOB")) 	sb.append(it.getBlob(column).length);
						else if ( type.equals("BOOLEAN")) sb.append((it.getInt(column)==0?"false":"true"));
						else sb.append("Unrecognized type in descriptor: " + fields[column][1] );
						sb.append("  ");
					}
					sb.append("]\n");
				}
			}
			finally {
				if ( it != null ) it.close();
			}
			} catch (Exception e) {
				sb.append("Exception processing record: " + e.getMessage());
				sb.append("\n");
			}
		}
		return sb.toString();
	}
	
	
}
