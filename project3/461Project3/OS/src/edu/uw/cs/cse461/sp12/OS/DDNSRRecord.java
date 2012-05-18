package edu.uw.cs.cse461.sp12.OS;

import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

public class DDNSRRecord {
	private boolean isAlive;
	private String ip;
	private int port;
	protected Timer timer;
	protected String name;
	protected String DDNSRecordType;
	private boolean done;
	
    public DDNSRRecord(){}
	
	public DDNSRRecord(String DDNSRecordType, String name) {
		this.name = name;
		this.isAlive = false;
		this.DDNSRecordType = DDNSRecordType;
		this.ip = "";
		this.port = -1;
	}
	
	public DDNSRRecord(String DDNSRecordType, String name, String ip, int port) {
		this.name = name;
		this.isAlive = false;
		this.DDNSRecordType = DDNSRecordType;
		this.ip = ip;
		this.port = port;
	}
	
	public void terminateTimers() {
		this.timer.cancel();
	}
	
    public boolean isDone() {
        return done;
    }

    public void setDone(boolean success) {
        this.done = success;
    }
	
	public boolean isAlive() {
		return this.isAlive;
	}
	
	public void setIsAlive(boolean boo) {
		this.isAlive = boo;
	}
	
	public String getIp() {
		return ip;
	}
	
	public void setIp(String ip){
	    this.ip = ip;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name){
	    this.name = name;
	}
	
	public void setPort(int port){
	    this.port = port;
	}
	
	public int getPort() {
		return port;
	}
	
	public String getDDNSRecordType() {
		return DDNSRecordType;
	}
	
	public void setDDNSRecordType(String type){
	    DDNSRecordType = type;
	}
	
	public void unregisterDDNSRecord() {
		this.timer.cancel();
		this.isAlive = false;
	}
	
	public JSONObject toJSON() {
		JSONObject nodeMsg = new JSONObject();
		try {
			nodeMsg.put("name", this.name);
			nodeMsg.put("type", this.DDNSRecordType);
			nodeMsg.put("ip", this.ip);
			nodeMsg.put("port", this.port);
		} catch (JSONException e) {
			e.printStackTrace();
		}
    	return nodeMsg;
	}
	
	public void schedule(int ttl, String IP, int Port) {
		if (this.timer != null)
			this.timer.cancel();
		timer = new Timer();
		this.isAlive = true;
		this.ip = IP;
		this.port = Port;
		this.timer.schedule(new TimerTask() {
			public void run() {
				isAlive = false;
				ip = "";
				port = -1;
			}
		}, (long) ttl * 1000);
	}
	
	public String toString(){
	    StringBuilder sb = new StringBuilder();
	    sb.append("name: " + name);
	    sb.append(", ip: " + ip);
	    sb.append(", port: " + port);
	    sb.append(", type: " + DDNSRecordType);
	    
	    return sb.toString();
	}
}

class CNAME extends DDNSRRecord {
	private String alias;

	public CNAME(String DDNSRecordType, String name, String alias) {
		super(DDNSRecordType, name);
		this.alias = alias;
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject nodeMsg = new JSONObject();
		try {
			nodeMsg.put("alias", this.alias);
			nodeMsg.put("name", this.name);
			nodeMsg.put("type", this.DDNSRecordType);
		} catch (JSONException e) {
			e.printStackTrace();
		}
    	return nodeMsg;
	}

}
