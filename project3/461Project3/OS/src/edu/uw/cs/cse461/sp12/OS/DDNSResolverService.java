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
	    timerStarted = false;
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
	    if(record.isDone()){
	        
	        this.myPort = myPort;
	        try {
	            RPCCallerSocket callerSocket = new RPCCallerSocket(record.getIp(), record.getIp(), "" + record.getPort());
	            JSONObject response = callerSocket.invoke("ddns", "register", generateRegisterJson(hostname, myPort));
	            System.out.println("register reponse: " + response);
	            if(response.length() < 3 || response.getString("resulttype").equals("ddnsexception")){
	                record.setDone(false);
	                return record;
	            }
	            JSONObject node = response.getJSONObject("node");
	            //setupt record
	            record.setIp(node.getString("ip"));
	            record.setPort(node.getInt("port"));
	            record.setName(node.getString("name"));
	            record.setDDNSRecordType(node.getString("type"));
	            
	            //if the timer is not started, start the timer
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
	
	
	/*
	 * start timer up
	 */
	private void startTimer(long delay, DDNSFullName hostname, int myPort){
        MTimerTask task = new MTimerTask();
        task.hostname = hostname;
        task.myPort = myPort;
        timer = new Timer();
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
            registerJ.put("ip", IPFinder.getCurrentIp());
            System.out.println("generateRegisterJson: " + registerJ.toString());
        } catch (JSONException excp) {
            excp.printStackTrace();
        }
        return registerJ;
    }

	/**
	 * 
	 * @param hostname
	 * @return
	 */
    public DDNSRRecord unregister(DDNSFullName hostname) {
        timerStarted = false;
//        DDNSRRecord record = resolve(hostname.hostname);
        DDNSRRecord record = getAdminHost(hostname);
//        DDNSRRecord record = new DDNSRRecord();
        try {
            RPCCallerSocket callerSocket = new RPCCallerSocket(record.getIp(), record.getIp(), "" + record.getPort());
            JSONObject response = callerSocket.invoke("ddns", "unregister", generateUnregisterJson(hostname, myPort));
            System.out.println("unregister json response: " + response);
        } catch (IOException excp) {
            excp.printStackTrace();
        }
        return record;
	}
    
    
    /*
     * 
     */
    private JSONObject generateUnregisterJson(DDNSFullName hostname, int port){
        JSONObject registerJ = new JSONObject();
        try {
            registerJ.put("name", hostname.hostname.trim());
            registerJ.put("password", password);
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
            remoteName = cacheRecord.getIp();
            remotePort = "" + cacheRecord.getPort();
            System.out.println("get cache with name: " + remoteName + " port: " + remotePort);
            try {
                callerSocket = new RPCCallerSocket(remoteName, remoteName, remotePort);
                response = callerSocket.invoke("ddns", "resolve", request);
                System.out.println("cached record resolve result: " + response.toString());
                if(response.has("done") && response.getBoolean("done"))
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
            int maxResolveNumber = 50;
            do{
                System.out.println("resolve to name: " + remoteName + " port: " + remotePort);
                callerSocket = new RPCCallerSocket(remoteName, remoteName, remotePort);
                response = callerSocket.invoke("ddns", "resolve", request);
                System.out.println("resolve result: " + response.toString());
                //failed to resolve
                if(response.length() < 3 || response.getString("resulttype").equals("ddnsexception")){
                    cacheRecord.setDone(false);
                    return cacheRecord;
                }
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
                cacheRecord.setDone(false);
                return cacheRecord;
            }
            cacheRecord.setDone(true);
            cacheRecord.setDDNSRecordType(node.getString("type"));
            cacheRecord.setName(node.getString("name"));
            cacheRecord.setIp(node.getString("ip"));
            cacheRecord.setPort(node.getInt("port"));
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
