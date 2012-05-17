package edu.uw.cs.cse461.sp12.OS;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

public class DDNSResolverService extends RPCCallable{

    private String rootName;
    private String rootPort;
    private String password;
    private String hostName;
    private int myPort;
    private Map<String, DDNSRRecord> cacheRecords;
    private Timer timer;
    private boolean timerStarted;
    
    public DDNSResolverService() {
        loadConfig();
        timer = new Timer();
        cacheRecords = new HashMap<String, DDNSRRecord>();
        timerStarted = false;
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
	
	public DDNSRRecord register(DDNSFullName hostname, int myPort) {
	    String currentHostname = hostname.hostname;
//	    DDNSRRecord result = new DDNSRRecord();
	    int upperNamePos = currentHostname.indexOf('.');
	    int upperUpperNamePos = currentHostname.indexOf('.', upperNamePos + 1);
	    DDNSRRecord record = new DDNSRRecord();
	    if(upperUpperNamePos >= 0){
	        
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
	            JSONObject node = response.getJSONObject("node");
	            record.ip = node.getString("ip");
	            record.port = node.getInt("port");
	            record.name = node.getString("name");
	            
	            if(!timerStarted){
	                timerStarted = true;
	                long delay = (long)(response.getLong("lifetime") * 0.8 * 1000);
	                startTimer(delay, hostname, myPort);
	            }
	            
	        } catch (IOException excp) {
	            excp.printStackTrace();
	        } catch (JSONException e) {
                e.printStackTrace();
            }
	        
	    }
	    return record;
	}
	
	private void startTimer(long delay, DDNSFullName hostname, int myPort){
        MTimerTask task = new MTimerTask();
        task.hostname = hostname;
        task.myPort = myPort;
        timer = new Timer();
        timer.scheduleAtFixedRate(task, delay, delay);
	}
	
	public class MTimerTask extends TimerTask{

	    DDNSFullName hostname;
	    int myPort;
	    
        @Override
        public void run() {
            if(timerStarted)
                register(hostname, myPort);
            else
                timer.cancel();
        }
	    
	}
	
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
            System.out.println("generateRegisterJson: " + registerJ.toString());
        } catch (JSONException excp) {
            excp.printStackTrace();
        }
        return registerJ;
    }

    public void unregister(DDNSFullName hostname) {
        timerStarted = false;
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
        DDNSRRecord cacheRecord;
        if(cacheRecords.containsKey(target)){
            cacheRecord = cacheRecords.get(target);
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
        }else{
            cacheRecords.put(target, new DDNSRRecord());
            cacheRecord = cacheRecords.get(target);
            
        }
        
        remoteName = rootName;
        remotePort = rootPort;
        
        try {
            int maxResolveNumber = 8;
            do{
                System.out.println("resolve to name: " + remoteName + " port: " + remotePort);
                callerSocket = new RPCCallerSocket(remoteName, remoteName, remotePort);
                response = callerSocket.invoke("ddns", "resolve", request);
                System.out.println("resolve result: " + response.toString());
                node = response.getJSONObject("node");
                if(node.getString("type").equals("CNAME")){
                    request = generateResolveJson(node.getString("alias"));
                    remoteName = rootName;
                    remotePort = rootPort;
                }else{
                    remoteName = node.getString("ip");
                    remotePort = "" + node.getInt("port");
                }
                maxResolveNumber--;
            }while(!response.getBoolean("done") && maxResolveNumber > 0);
            if(maxResolveNumber <= 0){
                cacheRecord.success = false;
                return cacheRecord;
            }
            cacheRecord.success = false;
            cacheRecord.type = node.getString("type");
            cacheRecord.ip = node.getString("ip");
            cacheRecord.port = node.getInt("port");
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
