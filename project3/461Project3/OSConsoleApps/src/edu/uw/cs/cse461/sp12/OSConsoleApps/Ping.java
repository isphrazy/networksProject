package edu.uw.cs.cse461.sp12.OSConsoleApps;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

import org.json.JSONObject;

import edu.uw.cs.cse461.sp12.OS.DDNSRRecord;
import edu.uw.cs.cse461.sp12.OS.DDNSResolverService;
import edu.uw.cs.cse461.sp12.OS.OS;
import edu.uw.cs.cse461.sp12.OS.RPCCallerSocket;
import edu.uw.cs.cse461.sp12.util.Log;

public class Ping implements OSConsoleApp {
	// I'm using the android-like Log.x() functions for debugging output, even for console apps
	private static final String TAG="PingConsole";
	private final int NUM_OF_TRIES = 5;
	
	public Ping() {
	}
	
	public String appname() {
		return "ping";
	}

	public void run() {
		try {
			// Eclipse doesn't support System.console()
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Enter lines like <target> <msg> to have <msg> echoed back");
			while ( true ) {
				try {
					System.out.print("Enter a host name or ip, or exit to exit: ");
					String targetIP = console.readLine();
					if ( targetIP == null ) targetIP = "";
					else if ( targetIP.equals("exit")) break;
					
					String targetPort = "0";
					if(targetIP.endsWith(".")) targetIP = targetIP.substring(0, targetIP.length() - 1);
					if(targetIP.endsWith("cse461") || targetIP.endsWith("www")){
					    DDNSRRecord record = ((DDNSResolverService)OS.getService("ddnsresolver")).resolve(targetIP);
					    if(!record.isDone()){
					        System.out.println("can't resolve given address");
					        continue;
					    }
					    targetIP = record.getIp();
					    targetPort = Integer.toString(record.getPort());
					}else{
					    System.out.print("Enter the RPC port, or empty line to exit: ");
					    targetPort = console.readLine();
					    if ( targetPort == null || targetPort.isEmpty() ) continue;
					}

					
					
					RPCCallerSocket socket = null;
					for (int i = 1; i <= NUM_OF_TRIES ; i++) {
						long startTime = System.nanoTime();
						socket = new RPCCallerSocket(targetIP, targetIP, targetPort);
						socket.invoke("echo", "echo", new JSONObject().put("msg", "") );
						long endTime = System.nanoTime();

						double timeTaken = (double) (endTime - startTime) / 1000000000.0;
						System.out.println("Test " + i + ": IP=" + targetIP + " host=" + targetPort + " time="
								+ timeTaken + " seconds");
					}
				} catch (Exception e) {
				    e.printStackTrace();
					System.out.println("Exception: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "PingConsole.run() caught exception: " +e.getMessage());
		}
	}

	public void shutdown() {
	}

}
