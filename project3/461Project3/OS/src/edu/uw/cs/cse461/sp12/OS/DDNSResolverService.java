package edu.uw.cs.cse461.sp12.OS;

import java.io.IOException;
import java.util.Timer;

import org.json.JSONException;
import org.json.JSONObject;

public class DDNSResolverService extends RPCCallable{

    private String rootName;
    private String rootPort;
    private String password;
    private String hostName;
    private int myPort;
    private DDNSRRecord cacheRecord;
    
    public DDNSResolverService() {
        loadConfig();
        
        cacheRecord = new DDNSRRecord();
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
	
	public void register(DDNSFullName hostname, int myPort) {
	    String currentHostname = hostname.hostname;
	    int upperNamePos = currentHostname.indexOf('.');
	    int upperUpperNamePos = currentHostname.indexOf('.', upperNamePos + 1);
	    if(upperUpperNamePos >= 0){
	        DDNSRRecord record = new DDNSRRecord();
	        
	        //if the domain is in cse461
	        if(currentHostname.substring(upperNamePos + 1, upperUpperNamePos).equals("cse461")){
	            record.ip = rootName;
	            record.port = Integer.parseInt(rootPort);
	        }else{
	            record = resolve(currentHostname.substring(currentHostname.indexOf('.') + 1, currentHostname.length()));
	        }
	        
	        this.myPort = myPort;
	        try {
	            RPCCallerSocket callerSocket = new RPCCallerSocket(record.ip, record.ip, "" + record.port);
	            JSONObject response = callerSocket.invoke("ddns", "register", generateRegisterJson(hostname, myPort));
	            System.out.println("register reponse: " + response);
	            Timer timer = new Timer();
//	            timer.scheduleAtFixedRate(task, firstTime, period)
	        } catch (IOException excp) {
	            excp.printStackTrace();
	        }
	    }
	}
	
	
	
//	private void admin(hostname)
	
	/*
	 * generate register json string and return it
	 */
	private JSONObject generateRegisterJson(DDNSFullName hostname, int port) {
	    JSONObject registerJ = new JSONObject();
	    try {
            registerJ.put("port", port);
            registerJ.put("name", hostname.hostname.trim());
            registerJ.put("password", password);
            registerJ.put("ip", IPFinder.getCurrentIp());
            System.out.println(registerJ.toString());
        } catch (JSONException excp) {
            excp.printStackTrace();
        }
        return registerJ;
    }

    public void unregister(DDNSFullName hostname) {
//        DDNSRRecord record = resolve(hostname.hostname);
        DDNSRRecord record = new DDNSRRecord();
        record.ip = rootName;
        record.port = Integer.parseInt(rootPort);
        try {
            RPCCallerSocket callerSocket = new RPCCallerSocket(record.ip, record.ip, "" + record.port);
            JSONObject response = callerSocket.invoke("ddns", "unregister", generateUnregisterJson(hostname, myPort));
            System.out.println("unregister json response: " + response);
        } catch (IOException excp) {
            excp.printStackTrace();
        }
	}
    
    public JSONObject generateUnregisterJson(DDNSFullName hostname, int port){
        JSONObject registerJ = new JSONObject();
        try {
//            registerJ.put("port", port);
            registerJ.put("name", hostname.hostname.trim());
            registerJ.put("password", password);
//            registerJ.put("ip", IPFinder.getCurrentIp());
            System.out.println("unregister json: " + registerJ.toString());
        } catch (JSONException excp) {
            excp.printStackTrace();
        }
        return registerJ;
    }
	
	public DDNSRRecord resolve(String target) {
        RPCCallerSocket callerSocket;
        JSONObject response = null;
//        DDNSRRecord record = new DDNSRRecord();
        JSONObject node = null;
        String remoteName = null;
        String remotePort = null;
        JSONObject request = generateResolveJson(target);

        if(cacheRecord.initialized){
            remoteName = cacheRecord.ip;
            remoteName = "" + cacheRecord.port;
            try {
                callerSocket = new RPCCallerSocket(remoteName, remoteName, remotePort);
                response = callerSocket.invoke("ddns", "resolve", request);
                System.out.println("cached record resolve result: " + response.toString());
                if(response.getBoolean("done"))
                    return cacheRecord;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        remoteName = rootName;
        remotePort = rootPort;
        
        try {
            do{
                callerSocket = new RPCCallerSocket(remoteName, remoteName, remotePort);
                response = callerSocket.invoke("ddns", "resolve", request);
                System.out.println("resolve result: " + response.toString());
                node = response.getJSONObject("node");
                remoteName = node.getString("ip");
                remotePort = "" + node.getInt("port");
                
            }while(!response.getBoolean("done"));
            cacheRecord.type = node.getString("type");
            cacheRecord.ip = node.getString("ip");
            cacheRecord.port = node.getInt("port");
            cacheRecord.initialized = true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
		return cacheRecord;
	}
	
	private JSONObject generateResolveJson(String target){
	    JSONObject resolveJ = new JSONObject();
	    try {
            resolveJ.put("name", target);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return resolveJ;
	    
	}
}
