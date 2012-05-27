package cse461.snet.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;

import cse461.snet.os.DDNSFullName;
import cse461.snet.os.OS;

//import cse461.snet.util.DB461SQLite;
//import edu.uw.cs.cse461.sp12.OS.DDNSFullName;
//import edu.uw.cs.cse461.sp12.OS.OS;
//import edu.uw.cs.cse461.sp12.util.Log;

/**
 * Abstracts the underlying sqlite database into a set of easy-to-use methods.
 * <p>
 * ONLY THE THREAD THAT OPENS AN SQLITE4JAVA DATABASE CAN OPERATE ON IT.
 * This means you should not cache a PhotoManager.  Instead, create one
 * one entry to your code, use it while that thread executes, and then discard
 * it before returning.
 * <p>
 * The db has two tables: 
 * <ol>
 * <li>Community
 * <ul>
 * <li> name [TEXT] - Person's fully qualified ddns name
 * <li> generation [INTEGER] - The generation number assigned by the friend for the two stored photos
 * <li> myphoto [INTEGER] - An int MD5 hash of the latest photo the person has taken.
 * <li> chosenphoto [INTEGER] - An int MD5 hash of the photo the person chose from among his/her friends' offerings.
 * <li> isFriend [BOOLEAN] - True if this person is a friend; false if just a member of the community. 
 * </ul>
 * <p>
 * <li>Photos
 * <ul>
 * <li> hash [INTEGER] - The MD5 hash of the photo
 * <li> refcnt [INTEGER] - A reference count for the photo.
 * <li> filename [TEXT] - The name of the file the photo is stored in.
 * </ul>
 * </ol>
 * <p>
 * @author zahorjan
 *
 */


public class SNetDB461 extends DB461SQLite {
	private static final String TAG="SNetDB461";
	private static final String DBFILENAME = new DDNSFullName(OS.hostname()) + "snet.db"; 
	
	//-----------------------------------------------------------------------------
	//-----------------------------------------------------------------------------
	// Community table and record types
	
	private static String[] communityFields[] = { {"name", "TEXT"},
		  									      {"generation", "INTEGER"},
		  									      {"myhash", "INTEGER"},
		  									      {"chosenhash", "INTEGER"},
		  									      {"isfriend", "BOOLEAN"}
											    };
	public class CommunityTable extends DB461Table<String, CommunityRecord> {
		public CommunityTable() { super("community", communityFields, "name" ); }
		
		@Override
		public CommunityRecord createRecord() { return new CommunityRecord(); }
		@Override
		public RecordSet<CommunityRecord> createRecordSet() { return new RecordSet<CommunityRecord>(); }
		
		/**
		 * Ensures that the DB contains the given record for the friend it names.
		 * Creates it if no record for that friend currently exists; updates the existing
		 * record otherwise.  DOES NOT alter photo table in any way.
		 * @param record
		 * @throws DB461Exception
		 */
		public void write(CommunityRecord record) throws DB461Exception {
			StringBuilder sb = new StringBuilder().append("INSERT OR REPLACE INTO ")
												  .append(name())
												  .append(" VALUES('")
												  .append(record.name)
												  .append("',")
												  .append(record.generation)
												  .append(",")
												  .append(record.myPhotoHash)
												  .append(",")
												  .append(record.chosenPhotoHash)
												  .append(",")
												  .append(record.isFriend?1:0)
												  .append(")");
			query(sb.toString());
		}
		
		

	}
	public CommunityTable COMMUNITYTABLE;

	/**
	 * One community record.
	 * <p>
	 * Fields:
	 * <ul>
	 * <li>name - The name associated with the record.
	 * <li>type - Primarily used to indicate "no record found," or else whether it was a host lookup or an app lookup
	 * <li>value - The value associated with the (name, type) pair.
	 * <li>authoritative - 1 for true; 0 for false
	 * <li>timestamp - The Unix time when the record was last written.
	 * </ul>
	 * @author zahorjan
	 *
	 */
	public class CommunityRecord extends Record {
		public String  name;
		public int     generation;
		public int     myPhotoHash;
		public int     chosenPhotoHash;
		public boolean isFriend;
		@Override
		protected CommunityRecord initialize(DBRecordIterator it) throws DB461Exception {
			try {
				name = it.getString(0); 
				if ( name.equals("null") ) name = null;
				generation = it.getInt(1);
				myPhotoHash = it.getInt(2);
				chosenPhotoHash = it.getInt(3);
				isFriend = it.getInt(4)==0 ? false : true;
			} catch (Exception e) {

			}
			return this;
		}
		@Override
		public String toString() {
			final String SEP = "  ";
			StringBuilder sb = new StringBuilder();
			sb.append("[")
			.append(SEP).append("Name: '").append(name).append("'")
			.append(SEP).append("Generation: ").append(generation)
			.append(SEP).append("MyPhotoHash: ").append(myPhotoHash)
			.append(SEP).append("ChosenPhotoHash: ").append(chosenPhotoHash)
			.append("]");
			return sb.toString();
		}
	}
	
	// Community table and record types
	//-----------------------------------------------------------------------------
	//-----------------------------------------------------------------------------
	

	//-----------------------------------------------------------------------------
	//-----------------------------------------------------------------------------
	// Photo table and record types
	
	private static String[] photoFields[] = { {"hash", "INTEGER"},
		 									  {"refcount", "INTEGER"},
		 									  {"photofile", "TEXT"},
		   									};
	public class PhotoTable extends DB461Table<Integer, PhotoRecord> {
		public PhotoTable() { super("photos", photoFields, "hash"); }
		
		@Override
		public PhotoRecord createRecord() { return new PhotoRecord(); }
		@Override
		public RecordSet<PhotoRecord> createRecordSet() { return new RecordSet<PhotoRecord>(); }
		
		/**
		 * Inserts a photo into the DB.  Normally, the photo should not already be in the DB.
		 * If it is, this method silently replaces it.
		 * The reference count of the photo is set to 1.
		 * @param hash
		 * @param photo
		 * @throws DB461Exception
		 */
		@Override
		public void write(PhotoRecord record) throws DB461Exception {
			StringBuilder sb = new StringBuilder().append("INSERT OR REPLACE INTO " + name()  + "(hash, refcount, photofile) VALUES(")
												  .append(record.hash)
												  .append(",")
												  .append(record.refCount)
												  .append(",'")
												  .append(record.file!=null?record.file.getAbsolutePath():null)
												  .append("')");
			query(sb.toString());
		}
	}
	public PhotoTable PHOTOTABLE;
		

	/**
	 * One photo record.
	 * <p>
	 * Fields:
	 * <ul>
	 * <li> hash - The md5 hash of the photo.
	 * <li> refCount - Current reference count value for the record.
	 * <li> photo - The photo, as a byte[]
	 * </ul>
	 * @author zahorjan
	 *
	 */
	public class PhotoRecord extends Record {
		public int       hash;
		public int  	 refCount;
		public File		 file; 
		@Override
		protected PhotoRecord initialize(DBRecordIterator it) throws DB461Exception {
			try {
				hash = it.getInt(0);
				refCount = it.getInt(1);
				String fullPath = it.getString(2); 
				if ( fullPath.equals("null")) file = null;
				else file = new File(fullPath);
			} catch (Exception e) {
				throw new DB461Exception( "photo record initialization caught exception: " + e.getMessage() );
			}
			return this;
		}
		@Override
		public String toString() {
			final String SEP = "  ";
			StringBuilder sb = new StringBuilder();
			sb.append("[")
			  .append(SEP).append("Hash: ").append(hash)
			  .append(SEP).append("RefCount: ").append(refCount)
			  .append(SEP).append("Filename: ").append(file)
			  .append("]");
			return sb.toString();
		}
	}

	// Photo table and record definitions
	//-----------------------------------------------------------------------------
	//-----------------------------------------------------------------------------
	
	//------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------
	// Photo class

	/**
	 * Utility class that wraps a photo file and provides handy functionality.
	 * @author zahorjan
	 *
	 */
	static public class Photo {
		private int    mHash;        // the hash value of the file's contents
		private File   mPhotoFile;   // handle to the file
		
		/**
		 * Wraps a photo file.  If the source file isn't in our own gallery, creates a
		 * copy there.
		 * @param photoFile
		 * @throws IOException
		 * @throws FileNotFoundException
		 */
		public Photo(File photoFile) throws IOException, FileNotFoundException {
			mPhotoFile = photoFile;
			mHash = _computeHash(mPhotoFile);
		}
		
		/**
		 * You might sometimes know the hash and not want to recompute it just to create a Photo object.
		 * @param hash
		 * @param file
		 */
		public Photo(int hash, File file) {
			mHash = hash;
			mPhotoFile = new File(file.getAbsolutePath());
		}

		public int hash() { return mHash; }
		public File file() { return mPhotoFile; }
		
		/**
		 * Set hash based on contents in file named by the Photo object.
		 * @param photo
		 */
		private static int _computeHash(File photoFile) throws IOException, FileNotFoundException {
			final int BUFLEN = 4096;
			byte buffer[] = new byte[BUFLEN];

			int read;
			FileInputStream in = new FileInputStream(photoFile.getAbsolutePath());
			try {
				MessageDigest md = MessageDigest.getInstance("MD5");
				while ( (read = in.read(buffer)) >= 0 ) {
					if ( read > 0 ) {
						md.update(buffer, 0, read);
					}
					else Log.w(TAG, "_setHash read 0 bytes");
				}
				return ByteBuffer.wrap(md.digest()).getInt();
			} catch (NoSuchAlgorithmException e) {
				System.err.println("_computeHash: no such algorithm: MD5");
			}
			finally {
				if ( in != null ) in.close();
			}
			return 0;
		}
		
		@Override
		public String toString() {
			return "[Hash: " + String.format("%0x", hash()) + "  File: " + mPhotoFile + "  Length: " + mPhotoFile.length() + "]";
		}
	}

	// Photo class
	//------------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------------
	
	/**
	 * If the database doesn't already exist, creates it and creates the two tables it uses.
	 * (No content is added to the tables.)  Otherwise, opens the existing database.
	 * @throws DB461Exception
	 */
	public SNetDB461() throws DB461Exception {
		super(DBFILENAME);
		Log.d(TAG, "dbFilename = " + DBFILENAME);
		PHOTOTABLE = new PhotoTable();
		addTable(PHOTOTABLE);
		COMMUNITYTABLE = new CommunityTable();
		addTable(COMMUNITYTABLE);
		
		try {
			openOrCreateDatabase();
		} catch (Exception e) {
			String msg = "Couldn't open or create db: " + e.getMessage();
			Log.e(TAG, msg);
			throw new DB461Exception(msg);
		}
	}
	
	/**
	 * Because of Java restrictions on nested classes, this record has be created by an SNetDB461 object.
	 * @return
	 */
	public CommunityRecord createCommunityRecord() {
		return new CommunityRecord();
	}
	
	/**
	 * Because of Java restrictions on nested classes, this record has be created by an SNetDB461 object.
	 * @return
	 */
	public PhotoRecord createPhotoRecord() {
		return new PhotoRecord();
	}
	
	/**
	 * When you're done using an SNetDB461, it's good practice to call discard().
	 */
	public void discard() {
		close();
	}
	
	/**
	 * Called only to make sure that the a member is registered.  If s/he already is,
	 * has no effect.  Otherwise, creates a record for the user initialized to
	 * indicate no information is available.
	 * <p>
	 * This is not intended to supplant write()!  It's here only because you should
	 * make sure the root and the local host are both members every time you start the
	 * app (since the DB might not exist - e.g., the first run).
	 * @param user
	 */
	public void registerMember(String member) throws DB461Exception {
		CommunityRecord r = COMMUNITYTABLE.readOne(member);
		if ( r != null ) return;

		r = COMMUNITYTABLE.createRecord();
		r.name = member;
		r.generation = -1;
		r.myPhotoHash = 0;      // 0 is used to indicate no photo.  What are the odds some photo will hash to 0?
		r.chosenPhotoHash = 0;
		COMMUNITYTABLE.write(r);
	}

	/**
	 * Checks db for consistency violations and tries to fix  them.
	 * Consistency requirements:
	 * <ul>
	 * <li> Every photo hash in community table should have a photo table entry
	 * <li> Ref count should be correct
	 * <li> If a photo table entry has a file name, the file should exist, and file's hash should correspond to the photo record key
	 * <li> If a file is in the gallery directory, it should be referenced by some photo record
	 * </ul>
	 */
	public void checkAndFixDB(File galleryDir) throws DB461Exception {
		HashMap<Integer,Integer> photoRefCntMap = new HashMap<Integer,Integer>();
		HashSet<File> referencedPhotoFileSet = new HashSet<File>();

		// Collect info on refs to photos
		RecordSet<CommunityRecord> friendVec = COMMUNITYTABLE.readAll();
		for ( CommunityRecord r : friendVec ) {
			if ( r.myPhotoHash != 0 ) photoRefCntMap.put(r.myPhotoHash, 0);
			if ( r.chosenPhotoHash != 0 ) photoRefCntMap.put(r.chosenPhotoHash, 0);
		}
		for ( CommunityRecord r : friendVec ) {
			if ( r.myPhotoHash != 0 ) photoRefCntMap.put(r.myPhotoHash, photoRefCntMap.get(r.myPhotoHash)+1 );
			if ( r.chosenPhotoHash != 0 ) photoRefCntMap.put(r.chosenPhotoHash, photoRefCntMap.get(r.chosenPhotoHash)+1 );
		}

		// Now compare with photos table entries
		RecordSet<PhotoRecord> photoVec = PHOTOTABLE.readAll();
		for ( PhotoRecord r : photoVec ) {
			if ( r.file != null ) referencedPhotoFileSet.add(r.file);
			
			Integer dbRefCnt = photoRefCntMap.get(r.hash);
			if ( dbRefCnt == null ) {
				Log.e(TAG, "Entry " + r.hash + " in photo table not referenced by any community records.  Deleting.");
				PHOTOTABLE.delete(r.hash);
				continue;
			}
			if ( r.refCount != dbRefCnt ) {
				Log.e(TAG, "Entry " + r.hash + " has ref cnt " + r.refCount + " in photo table but actual cnt is " + dbRefCnt + ".  Fixing.");
				r.refCount = dbRefCnt;
				PHOTOTABLE.write(r);
			}
			if ( r.file!=null && !r.file.exists() ) {
				Log.e(TAG, "Entry for photo " + r.hash + " references file " + r.file.getAbsolutePath() + " but that file doesn't exist.  Fixing.");
				r.file = null;
				PHOTOTABLE.write(r);
				continue;
			}
			Photo photo = null;
			try {
				photo = new Photo(r.file);
				if ( r.hash != photo.mHash ) {
					Log.e(TAG, "Entry for photo " + r.hash + " references file " + r.file + " but that file has hash " + photo.mHash + ".  Fixing.");
					throw new DB461Exception("delete exception");
				}
			} catch (Exception e) {
				// if we can't compute the hash, the file isn't there
				PHOTOTABLE.delete(r.hash);
				for ( CommunityRecord c : friendVec ) {
					if ( c.myPhotoHash == r.hash ) c.myPhotoHash = 0;
					if ( c.chosenPhotoHash == r.hash ) c.chosenPhotoHash = 0;
					COMMUNITYTABLE.write(c);
				}
			}
		}
		
		// Delete files in gallery directory that don't appear in any record
		if ( galleryDir == null ) {
			Log.w(TAG, "Gallery directory arg is null.  Skipping check for superfluous files.");
			return;
		}
		File[] fileList = galleryDir.listFiles();
		if ( fileList == null ) return;
		for ( File file : fileList ) {
			if ( !file.isFile() ) continue;
			//String absPath = file.getAbsolutePath();
			if ( referencedPhotoFileSet.contains(file) ) continue;
			Log.w(TAG, "There are no references to gallery file " + file.getAbsolutePath() + ". Deleting file.");
			file.delete();
		}
	}
}
