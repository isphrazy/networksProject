package edu.uw.cs.cse461.sp12.OS;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * singleton, return the ip address
 * @author Pingyang He
 *
 */
public class IPFinder {
	private static IPFinder ipFinder;
	public static String ip;
	
	/**
	 * 
	 * @return the instance of this class
	 */
	public static IPFinder getInstance(){
		if (ipFinder == null){
			ipFinder = new IPFinder();
		}
		return ipFinder;
	}
	
	/**
	 * 
	 * @return get the ip address
	 */
	public static String getIp(){
		if(ip == null){
			try {
				ip = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		return ip;
	}
	
	public static String getCurrentIp(){
	    String currentIp = null;
	    try {
	        currentIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
	    return currentIp;
	}
}
