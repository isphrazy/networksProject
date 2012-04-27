package edu.uw.cs.cse461.sp12.OSConsoleApps;

/**
 * All console (non-Android) apps must implement this interface.
 * (The AppManager relies on this interface.)
 * <p>
 * Additionally, each class implementing this interface must provide
 * a constructor that takes no arguments.
 * 
 * @author zahorjan
 *
 */
public interface OSConsoleApp {
	/**
	 * Returns a unique, short, string name for the application (e.g., "echo").
	 * @return
	 */
	public String appname();
	/**
	 * This method is called each time the app is invoked via the AppManager. 
	 * @throws Exception
	 */
	public void run() throws Exception;
	/**
	 * This method is called when the entire system is coming down.  If the app
	 * has started any background threads, they should be terminated.
	 * 
	 * @throws Exception
	 */
	public void shutdown() throws Exception;
}
