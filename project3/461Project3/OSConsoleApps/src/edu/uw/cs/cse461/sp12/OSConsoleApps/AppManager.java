package edu.uw.cs.cse461.sp12.OSConsoleApps;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import edu.uw.cs.cse461.sp12.OS.OS;
import edu.uw.cs.cse461.sp12.util.Log;

/**
 * An AppManager acts someting like a shell on a traditional system - it lets
 * you run apps (OSConsoleApps).  Unlike traditional systems, but something
 * like Android, there is only one instance of an app created, no matter how
 * many times it's invoked.  In this system, all apps are loaded when the 
 * OS boots.  An app invocation is merely a call to its run() method.
 * The AppManager   
 * @author zahorjan
 *
 */
public class AppManager {
	private static final String TAG="AppManager";

	private HashMap<String, OSConsoleApp> mAppMap;
	
	protected static final String[] RPCApps = {"edu.uw.cs.cse461.sp12.OSConsoleApps.Echo", 
											   "edu.uw.cs.cse461.sp12.OSConsoleApps.WhoAmI",
											   "edu.uw.cs.cse461.sp12.OSConsoleApps.nslookup",
											   "edu.uw.cs.cse461.sp12.OSConsoleApps.Ping"
											  };
	
	/**
	 * Constructor handles command line arguments, reads the config file, boots the OS, and
	 * loads the RPC service and other services that depend only on it.
	 * 
	 * @param args String[] argument passed to main().
	 * @throws Exception
	 */
	public AppManager(String[] args) throws Exception {
		mAppMap = new HashMap<String, OSConsoleApp>();
		String configFilename = "config.ini";  // default: may not exist

		// This code deals with command line options
		Options options = new Options();
		options.addOption("f", "configfile", true, "Config file name (Default: " + configFilename + ")");
		options.addOption("h", "help", false, "Print this message");

		CommandLineParser parser = new PosixParser();

		CommandLine line = parser.parse(options, args);
		if ( line.hasOption("help") ) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(OS.class.getName(), options );
			return;
		}
		if ( line.hasOption("configfile") ) configFilename = line.getOptionValue("configfile");

		// read config file data
		Properties config = new Properties();
		config.load(new FileInputStream(configFilename));

		String showDebug = config.getProperty("debug.enable");
		Log.setShowLog( showDebug == null || (!showDebug.isEmpty() && !showDebug.equals("0")) );
		String debugLevel = config.getProperty("debug.level");
		if ( debugLevel != null ) {
			try {
				Log.setLevel(Integer.parseInt(debugLevel));
			} catch (Exception e) {
				Log.e(TAG, "debug.level entry in " + configFilename + " has invalid value.  (Should be 0 or 1.)");
			}
		}

		// boot the OS and load RPC services
		OS.boot(config);
		OS.startServices(OS.rpcServiceClasses);
		OS.startServices(OS.ddnsServiceClasses);
	}

	/**
	 * Loads apps.  Apps are loaded at boot time, but don't really run then.  Instead,
	 * the AppManager enters a loop allowing the user to specify an app to run.  At that point,
	 * the app's run() method is invoked.
	 * 
	 * @param appClassList
	 * @throws Exception
	 */
	public synchronized void loadApps(String[] appClassList) throws Exception {
		String startingApp = null;  // for debugging output in catch block
		try {
			for ( String appClassname : appClassList ) {
				Log.v(TAG, "Loading " + appClassname);
				startingApp = appClassname;
				Class<OSConsoleApp> appClass = (Class<OSConsoleApp>)Class.forName(appClassname);
				OSConsoleApp app = appClass.newInstance();
				mAppMap.put(app.appname(), app);
				Log.v(TAG, appClassname + " Loaded");
			}
		} catch (Exception e) {
			throw new Exception( "Exception starting app " + startingApp + ": " + e.getMessage());
		}
	}

	/**
	 * This method implements a very primitive shell.  Apps are "run in the foreground."
	 * @throws Exception
	 */
	public void run() throws Exception {
		// Eclipse doesn't support System.console()
		BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			StringBuilder sb = new StringBuilder().append("Enter app name (");
			for ( String appname : mAppMap.keySet() ) {
				sb.append( appname + "  ");
			}
			sb.append("), or exit: ");
			System.out.print(sb.toString());
			String appName = console.readLine();

			if ( appName == null ) continue;
			if ( appName.equals("exit") ) break;

			OSConsoleApp app = mAppMap.get(appName);
			if ( app == null ) System.out.println("No such app: " + appName);
			else app.run();
		}
	}
	
	public void shutdown() {
		for ( OSConsoleApp app : mAppMap.values() ) {
			try {
				app.shutdown();
			} catch (Exception e) {
				Log.e(TAG, "shutdown caught exception: " + e.getMessage());
			}
		}
		OS.shutdown();
	}
	
	public static void main(String[] args) {
		final String TAG="AppManagerRPC.main";

		AppManager mgr = null;
		try {
			mgr = new AppManager(args);
			mgr.loadApps(RPCApps);
			mgr.run();
		} catch (Exception e) {
			Log.e(TAG, "Caught exception: " + e.getMessage());
		} finally {
			if ( mgr != null ) mgr.shutdown();
		}
	}
		
}
