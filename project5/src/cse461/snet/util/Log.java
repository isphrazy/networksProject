package cse461.snet.util;

/**
 * This is a simple debug message class that implements
 * filtering based on log level.  It's modeled after android.util.Log.
 * <p>
 * You choose a level at which to produce a message by calling, say,
 * Log.d("some tag", "my message").  You can set the class to filter
 * all messages below a client-specified level.
 * 
 * @author zahorjan
 *
 */
enum DebugLevel {VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT};

public class Log {
	static private int mLevel = 0;
	static private boolean mShowLog = true;
	
	static private boolean _show(int level) {
		return mShowLog && level >= mLevel;
	}
	
	static public int setLevel(int level) {
		int old = mLevel;
		mLevel = level;
		return old;
	}
	
	static public boolean setShowLog(boolean b) {
		boolean old = mShowLog;
		mShowLog = b;
		return old;
	}
	
	static public int v(String Tag, String msg) {
		if ( _show(2) ) wtf(Tag, msg);
		return 0;
	}

	static public int e(String Tag, String msg) {
		if (_show(6)) return wtf(Tag, msg);
		return 0;
	}
	
	static public int d(String Tag, String msg) {
		if ( _show(3) ) return wtf(Tag, msg);
		return 0;
	}
	
	static public int i(String Tag, String msg) {
		if ( _show(4) ) return wtf(Tag, msg);
		return 0;
	}
		
	static public int w(String Tag, String msg) {
		if ( _show(5) ) return wtf(Tag, msg);
		return 0;
	}
	
	static public int wtf(String Tag, String msg) {
		System.out.println(" " + Tag + "  " + msg);
		return 0;
	}

}
