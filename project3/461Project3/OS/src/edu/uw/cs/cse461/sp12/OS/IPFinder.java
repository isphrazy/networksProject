package edu.uw.cs.cse461.sp12.OS;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IPFinder {
	private static IPFinder ipFinder;
	public static String ip;
	
	public static IPFinder getInstance(){
		if (ipFinder == null){
			ipFinder = new IPFinder();
		}
		return ipFinder;
	}
	
	
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
}
