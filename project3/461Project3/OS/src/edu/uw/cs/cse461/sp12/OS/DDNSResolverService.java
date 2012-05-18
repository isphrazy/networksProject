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
        
        initVars();
    }
    
    /*
     * initiate variables
     */
    private void initVars() {
        timer = new Timer();
        cacheRecords = new HashMap<String, DDNSRRecord>();
        timerStarted = false;
    }


    /*
     * load configuration file and set up varialbes
     */
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
	    timer.cancel();
	    timer.purge();
	    unregister(new DDNSFullName(hostName));
	}
	
	/*
	 * find the ip and port tyring 
	 */
	private DDNSRRecord getAdminHost(DDNSFullName hostname){
	    DDNSRRecord record = new DDNSRRecord();
	    String currentHostname = hostname.hostname;
        int upperNamePos = currentHostname.indexOf('.');
        int upperUpperNamePos = currentHostname.indexOf('.', upperNamePos + 1);
        record.setDone(false);
        if(upperUpperNamePos >= 0){
            record.setDone(true);
            //if the given hostname is directly under cse461.
            if(currentHostname.substring(upperNamePos + 1, upperUpperNamePos).equals("cse461")){
                record.setIp(rootName);
                record.setPort(Integer.parseInt(rootPort));
            }else{
                record = resolve(currentHostname.substring(currentHostname.indexOf('.') + 1, currentHostname.length()));
            }
        }
	    return record;
	}
	
	/**
	 * register the given hostname to corresponding soa
	 * @param hostname is the hostname that will be registered
	 * @param myPort is the port service will be using
	 * @return the regiter result
	 */
	public DDNSRRecord register(DDNSFullName hostname, int myPort) {
	    
	    DDNSRRecord record = getAdminHost(hostname);
	    DDNSRRecord result = new DDNSRRecord();
	    if(record.isDone()){
	        
	        this.myPort = myPort;
	        try {
	            
	            RPCCallerSocket callerSocket = new RPCCallerSocket(record.getIp(), record.getIp(), "" + record.getPort());
	            JSONObject response = callerSocket.invoke("ddns", "register", generateRegisterJson(hostname, myPort));
	            if(response.length() < 3 || response.getString("resulttype").equals("ddnsexception")){
	                result.setDone(false);
	                return result;
	            }
	            JSONObject node = response.getJSONObject("node");
	            //setupt record
	            result.setIp(node.getString("ip"));
	            result.setPort(node.getInt("port"));
	            result.setName(node.getString("name"));
	            result.setDDNSRecordType(node.getString("type"));
	            
	            //if the timer is not started, start the timer
	            if(!timerStarted){
	                timerStarted = true;
	                long delay = (long)(response.getLong("lifetime") * 0.8 * 1000);
	                startTimer(delay, hostname, myPort);
	            }
	            
	        } catch (Exception excp) {
	            result.setDone(false);
	        } 
	        
	    }
	    return result;
	}
	
	
	/*
	 * start timer up
	 */
	private void startTimer(long delay, DDNSFullName hostname, int myPort){
        MTimerTask task = new MTimerTask();
        task.hostname = hostname;
        task.myPort = myPort;
        timer.scheduleAtFixedRate(task, delay, delay);
	}
	
	/*
	 * Timer class
	 */
	private class MTimerTask extends TimerTask{

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
            registerJ.put("ip", IPFinder.getIp());
        } catch (JSONException excp) {
        }
        return registerJ;
    }

	/**
	 * 
	 * @param hostname is the hostname that will be unregistered
	 * @return
	 */
    public DDNSRRecord unregister(DDNSFullName hostname) {
        timerStarted = false;
        DDNSRRecord record = getAdminHost(hostname);
        DDNSRRecord result = new DDNSRRecord();
        result.setDone(true);
        try {
            RPCCallerSocket callerSocket = new RPCCallerSocket(record.getIp(), record.getIp(), "" + record.getPort());
            JSONObject response = callerSocket.invoke("ddns", "unregister", generateUnregisterJson(hostname, myPort));
        } catch (IOException excp) {
            result.setDone(false);
        }
        return result;
	}
    
    
    /*
     * generate unregister json
     */
    private JSONObject generateUnregisterJson(DDNSFullName hostname, int port){
        JSONObject registerJ = new JSONObject();
        try {
            registerJ.put("name", hostname.hostname.trim());
            registerJ.put("password", password);
        } catch (JSONException excp) {}
        return registerJ;
    }
	
    
    /**
     * resolve a given string to ip/port
     * @param target
     * @return
     */
	public DDNSRRecord resolve(String target) {
        RPCCallerSocket callerSocket;
        JSONObject response = null;
//        DDNSRRecord record = new DDNSRRecord();
        JSONObject node = null;
        String remoteName = null;
        String remotePort = null;
        JSONObject request = generateResolveJson(target);
        DDNSRRecord cacheRecord = cacheRecords.get(target);
        
        
        if(cacheRecord != null && cacheRecord.isDone()){//cache exist
            remoteName = cacheRecord.getIp();
            remotePort = "" + cacheRecord.getPort();
            try {
                callerSocket = new RPCCallerSocket(remoteName, remoteName, remotePort);
                response = callerSocket.invoke("ddns", "resolve", request);
                if(response.has("done") && response.getBoolean("done"))
                    return cacheRecord;
                else if(response.has("exceptionnum") && response.getInt("exceptionnum") == 2){
                    cacheRecord.setDone(false);
                    return cacheRecord;
                }
            } catch (Exception e) {
                cacheRecord.setDone(false);
                return cacheRecord;
            } 
        }else{
            cacheRecords.put(target, new DDNSRRecord());
            cacheRecord = cacheRecords.get(target);
        }
        
        remoteName = rootName;
        remotePort = rootPort;
        
        try {
            int maxResolveNumber = 50;
            do{
                callerSocket = new RPCCallerSocket(remoteName, remoteName, remotePort);
                response = callerSocket.invoke("ddns", "resolve", request);
                //failed to resolve
                if(response.length() < 3 || response.getString("resulttype").equals("ddnsexception")){
                    cacheRecord.setDone(false);
                    return cacheRecord;
                }
                node = response.getJSONObject("node");
                if(node.getString("type").equals("CNAME")){
                    cacheRecords.remove(target);
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
                cacheRecord.setDone(false);
                return cacheRecord;
            }
            cacheRecord.setDone(true);
            cacheRecord.setDDNSRecordType(node.getString("type"));
            cacheRecord.setName(node.getString("name"));
            cacheRecord.setIp(node.getString("ip"));
            cacheRecord.setPort(node.getInt("port"));
        } catch (Exception e) {
            cacheRecord.setDone(false);
            
        } 
		return cacheRecord;
	}
	
	/*
	 * 
	 */
	private JSONObject generateResolveJson(String target){
	    JSONObject resolveJ = new JSONObject();
	    try {
            resolveJ.put("name", target);
        } catch (JSONException e) {
        }
        return resolveJ;
	    
	}
}
