package edu.uw.cs.cse461.sp12.OSConsoleApps;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import edu.uw.cs.cse461.sp12.OS.DDNSException;
import edu.uw.cs.cse461.sp12.OS.DDNSException.DDNSNoAddressException;
import edu.uw.cs.cse461.sp12.OS.DDNSException.DDNSNoSuchNameException;
import edu.uw.cs.cse461.sp12.OS.DDNSRRecord;
import edu.uw.cs.cse461.sp12.OS.DDNSResolverService;
import edu.uw.cs.cse461.sp12.OS.OS;

/**
 * This application resolves a name and displays the resulting resource record.
 * It will require a bit of porting to get to work, because your DDNS application
 * api is sure not to be the same as mine.
 * <p>
 * Among the less expected changes is that I've modified the OS implementation.
 * I now require "implements OSLoadableApp" rather than the "implements OSConsoleApp"
 * that was used in the Project 3 distribution.
 * 
 * @author zahorjan
 *
 */
public class nslookup implements OSConsoleApp {
	
	/**
	 * Method required by OSLoadableApp interface
	 */
	@Override
	public String appname() { return "nslookup"; }
	
	/**
	 * Constructor required by OSLoadableApp interface.  There's nothing to do for this app.
	 */
	public nslookup() {
	}

	/**
	 * Method required by OSLoadableApp interface.  Again, nothing to do.
	 */
	@Override
	public void shutdown() {}
	
	/**
	 * Fetches the resource record for a name, using the locally running DDNS name resolver service.
	 * (All systems are required to run a name resolver.)
	 */
	public void run() {
		try {
			// Eclipse doesn't support System.console()
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			while ( true ) {
				String targetStr = null;
				DDNSRRecord record = null;
				try {
					System.out.print("Enter a host name, or exit to exit: ");
					targetStr = console.readLine();
					if ( targetStr == null ) targetStr = "";
					else if ( targetStr.equals("exit")) break;
					record = ((DDNSResolverService)OS.getService("ddnsresolver")).resolve(targetStr);
					System.out.println( targetStr + ":  [" + record.toString() + "]");
//				} catch (DDNSNoAddressException nae) {
//					System.out.println("No address is currently assoicated with that name");
//				} catch (DDNSNoSuchNameException nsne) {
//					System.out.println("No such name: " + targetStr);
				} catch (Exception e) {
				    e.printStackTrace();
					System.out.println("Exception: " + e.getMessage());
				}
			}
		} catch (Exception e) {
		    e.printStackTrace();
			System.out.println("EchoConsole.run() caught exception: " + e.getMessage());
		}	
	}
}
