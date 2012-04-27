package edu.uw.cs.cse461.sp12.OSConsoleApps;

import edu.uw.cs.cse461.sp12.OS.OS;
import edu.uw.cs.cse461.sp12.OS.RPCService;
import edu.uw.cs.cse461.sp12.util.Log;

/**
 * A ConsoleApp that prints the IP/port of the RPC service on the local machine.
 * 
 * @author zahorjan
 *
 */
public class WhoAmI implements OSConsoleApp {
	// I'm using the android-like Log.x() functions for debugging output, even for console apps
	private static final String TAG="WhoAmI";
	
	/**
	 * OSConsoleApp's must have a constructor taking no arguments.  The constructor can initialize,
	 * but shouldn't perform any of the function of the app.  That's done in the run() method.
	 */
	public WhoAmI() {
	}

	/**
	 * A OSConsoleApp must have an appname() method.  (The name doesn't have to match the class name,
	 * but does have to be unique among all apps.)
	 */
	public String appname() {
		return "whoami";
	}

	/**
	 * This method will be called each time the app is invoked (by the AppManager).
	 */
	public void run() {
		try {
			RPCService rpcService = (RPCService)OS.getService("rpc");
			System.out.println("IP: " + rpcService.localIP() + "  Port: " + rpcService.localPort());
		} catch (Exception e) {
			Log.e(TAG, "Caught exception: " + e.getMessage());
		}
	}

	/**
	 * Required to be an OSConsoleApp.  This is called when the entire system is going down (i.e., not
	 * at the end of each execution).  In the case of WhoAmI, there's nothing that needs doing.
	 */
	public void shutdown() {
	}
}
