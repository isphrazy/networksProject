package edu.uw.cs.cse461.sp12.OS;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

public class DDNSResolverService extends RPCCallable{

    private String rootName;
    private String rootPort;
    private String password;
    private String hostName;
    
    public DDNSResolverService() {
        loadConfig();
    }
    
	private void loadConfig() {
	    rootName = OS.config().getProperty("root.name");
	    rootPort = OS.config().getProperty("root.port");
	    password = OS.config().getProperty("root.password");
	    hostName = OS.config().getProperty("host.name");
    }

    @Override
	public String servicename() {
		return "ddnsresolver";
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
	}
	
	public void register(DDNSFullName hostname, int port) {
	    
	    try {
            RPCCallerSocket callerSocket = new RPCCallerSocket(rootName, rootName, rootPort);
            JSONObject response = callerSocket.invoke("ddns", "resolve", generateRegisterJson(hostname, port));
            System.out.println(response);
	    } catch (IOException excp) {
            excp.printStackTrace();
        }
	}
	
	/*
	 * generate register json string and return it
	 */
	private JSONObject generateRegisterJson(DDNSFullName hostname, int port) {
	    JSONObject registerJ = new JSONObject();
	    try {
            registerJ.put("port", port);
            registerJ.put("name", hostname.hostname);
            registerJ.put("password", 12358);
            registerJ.put("ip", IPFinder.getCurrentIp());
        } catch (JSONException excp) {
            // TODO Auto-generated catch block
            excp.printStackTrace();
        }
        return registerJ;
    }

    public void unregister(DDNSFullName hostname) {
		
	}
	
	public DDNSRRecord resolve(String target) {
		return null;
	}
}
