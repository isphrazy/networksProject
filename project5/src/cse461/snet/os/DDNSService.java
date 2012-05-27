package cse461.snet.os;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import org.json.JSONException;
import org.json.JSONObject;
import cse461.snet.os.DDNSException;

/**
 * DDNSService maintains the ddns tree. If it is a SOA, it will keep track
 * of the instances below this. It also provides a resolve service to others
 * to resolve the ip address and port number of this and the instances below
 * this.
 * 
 * @author Cheng Hao Chuang
 * @author Pingyang He
 * @project CSE461 12sp project 4 
 */
public class DDNSService extends RPCCallable{
	public static final String[] ddnsHostNames = {
		"htc.null.cse461."
	};
	
	private static final int TTL = 60;
	
	// A variable capable of describing a method that can be invoked by RPC.
	private RPCCallableMethod<DDNSService> ddns;
	private RPCCallableMethod<DDNSService> resolve;
	private RPCCallableMethod<DDNSService> unregister;

	private Map<String, DDNSRRecord> ddnsMap;
	private Map<String, String> ddnsHostAndPassword;
	private String hostName;
	private DDNSRRecord ddnsRecordType;
	
    @Override
    public String servicename() {
        return "ddns";
    }

    @Override
    public void shutdown() {
    	this.ddnsRecordType.terminateTimers();
    	for (String key: ddnsMap.keySet()) {
    		ddnsMap.get(key).terminateTimers();
    	}
    	((DDNSResolverService)OS.getService("ddnsresolver")).unregister(new DDNSFullName(this.hostName));
    }
    
    /**
     * Constructs a ddnsService object. It registers register, resolve and
     * unregister methods to rpc. 
     */
    public DDNSService() throws Exception{
    	// Set up the method descriptor variable to refer to this->_register()
		ddns = new RPCCallableMethod<DDNSService>(this, "_register");
		// Register the method with the RPC service as externally invocable method "register"
		((RPCService)OS.getService("rpc")).registerHandler(servicename(), "register", ddns );
		
		resolve = new RPCCallableMethod<DDNSService>(this, "_resolve");
		((RPCService)OS.getService("rpc")).registerHandler(servicename(), "resolve", resolve );
		
		unregister = new RPCCallableMethod<DDNSService>(this, "_unregister");
		((RPCService)OS.getService("rpc")).registerHandler(servicename(), "unregister", unregister );

		int serverport = Integer.parseInt(OS.config().getProperty("rpc.serverport"));
    	this.hostName = OS.config().getProperty("host.name");
		String ip =	IPFinder.getInstance().getIp();
		try{
		    ((DDNSResolverService)OS.getService("ddnsresolver")).register(new DDNSFullName(this.hostName), serverport);
		}catch (Exception e){
		    System.out.println("failed to register");
		    return;
		}
    	
    	ddnsMap = new HashMap<String, DDNSRRecord>();
    	ddnsHostAndPassword = new HashMap<String, String>();
    	
		String ddnsRecordType = OS.config().getProperty("ddnsrecordtype");
		this.ddnsRecordType = new DDNSRRecord(ddnsRecordType, hostName, ip, serverport);
    	setupddns();
    	
    	// For grading purposes
    	setupAandB();
    }
    
    /**
	 * This method is callable by RPC (because of the actions taken by the constructor).
	 * <p>
	 * All RPC-callable methods take a JSONObject as their single parameter, and return
	 * a JSONObject.  (The return value can be null.)  This particular method register 
	 * the name, ip and port in args.
	 * @return DDNSNoSuchNameException if name is not under this instance
	 * @return DDNSAuthorizationException if the password is not associated
	 * 		   with name
	 * @return DDNSRuntimeException if all other unexpected things happened
	 */
    public JSONObject _register(JSONObject args) throws JSONException, IOException {
    	try {
    		String name = args.getString("name");
    		String password = args.getString("password");
    		String ip = args.getString("ip");
    		int port = args.getInt("port");
    		
    		if (!ddnsMap.containsKey(name) || !ddnsHostAndPassword.containsKey(name))
    			return exceptionMsg("DDNSNoSuchNameException", "1", "No such name exists for name ",name);
    		
    		if (!ddnsHostAndPassword.get(name).equals(password))
    			return exceptionMsg("DDNSAuthorizationException", "3", "Bad password for ",name);
    		
    		DDNSRRecord temp = ddnsMap.get(name);
    		temp.schedule(TTL, ip, port);
    		return successMsg(temp, "registerresult", true);
    	} catch (JSONException e) {
    		return exceptionMsg("DDNSRuntimeException", "4", "Sorry, runtimeException. I don't know what's wrong","");
    	// If the value of a filed is not following the protocol
    	} catch (Exception e) {
    		return exceptionMsg("DDNSRuntimeException", "4", "Sorry, runtimeException. I don't know what's wrong","");
    	}
    }
    
    /**
	 * This method is callable by RPC (because of the actions taken by the constructor).
	 * <p>
	 * All RPC-callable methods take a JSONObject as their single parameter, and return
	 * a JSONObject.  (The return value can be null.)  This particular method unregisters
	 * the name.
	 * 
	 * @return DDNSNoSuchNameException if name is not under this instance
	 * @return DDNSAuthorizationException if the password is not associated
	 * 		   with name
	 * @return DDNSRuntimeException if all other unexpected things happened
	 */
    public JSONObject _unregister(JSONObject args) throws JSONException, IOException {
    	try {
    		String name = args.getString("name");
    		String password = args.getString("password");
    		
    		if (!ddnsMap.containsKey(name))
    			return exceptionMsg("DDNSNoSuchNameException", "1", "No such name exists for name ",name);
    		
    		if (!ddnsHostAndPassword.get(name).equals(password))
    			return exceptionMsg("DDNSAuthorizationException", "3", "Bad password for ",name);
    		
    		DDNSRRecord temp = ddnsMap.get(name);
    		temp.unregisterDDNSRecord();
    		
    		return successMsg(temp, "unregisterresult", false);
    	} catch (JSONException e) {
    		return exceptionMsg("DDNSRuntimeException", "4", "Sorry, runtimeException. I don't know what's wrong","");
    	// If the value of a filed is not following the protocol
    	} catch (Exception e) {
    		return exceptionMsg("DDNSRuntimeException", "4", "Sorry, runtimeException. I don't know what's wrong","");
    	}
    }
    
    /**
	 * This method is callable by RPC (because of the actions taken by the constructor).
	 * <p>
	 * All RPC-callable methods take a JSONObject as their single parameter, and return
	 * a JSONObject.  (The return value can be null.)  This particular method resolve
	 * the name in args
	 * @return DDNSNoSuchNameException if name is not under this instance
	 * @return DDNSNoAddressException if there is current no address
	 * 		   associated with this name.
	 * @return DDNSRuntimeException if all other unexpected things happened
	 */
    public JSONObject _resolve(JSONObject args) throws JSONException, IOException {
    	String name = args.getString("name");
    	String newName = name;
    	if (!name.endsWith("."))
    		newName += ".";
    	if (this.ddnsRecordType.getName().equals(newName)) {
    		return successMsg(ddnsRecordType, "resolveresult", false);
    	}
    	
    	if (this.ddnsMap.containsKey(newName)) {
    		if (!this.ddnsMap.get(newName).isAlive())
    			return exceptionMsg("DDNSNoAddressException", "2", "No such address exists for name ",name);
    		return successMsg(ddnsMap.get(newName), "resolveresult", false);
    	}

    	for (String hostName: ddnsMap.keySet()) {
    		if (name.endsWith(hostName)) {
    			return successMsg(ddnsMap.get(newName), "resolveresult", false);
    		}
    	}
    	return exceptionMsg("DDNSRuntimeException", "4", "Sorry, runtimeException. I don't know what's wrong","");
    }
    
    /* Constructs and returns a success json message */
    private JSONObject successMsg(DDNSRRecord record, String resulttype, boolean ttl) {
    	JSONObject successMsg = new JSONObject();
		try {
			successMsg.put("node", record.toJSON());
			if (ttl)
				successMsg.put("lifetime",TTL);
			successMsg.put("resulttype", resulttype);
			if (record.getDDNSRecordType().equals("NS") || record.getDDNSRecordType().equals("CNAME"))
				successMsg.put("done", false);
			else
				successMsg.put("done", true);
		} catch (JSONException e) {
			e.printStackTrace();
		}
    	return successMsg;
    }
    
    /* Constructs and returns a exception json message */
    private JSONObject exceptionMsg(String exception, String exceptionnum, String message, String name) {
    	JSONObject exceptionMsg = new JSONObject();
		try {
			exceptionMsg.put("resulttype", "ddnsexception");
			exceptionMsg.put("exceptionnum", exceptionnum);
			exceptionMsg.put("message", message+name);
			if (!exception.equals("DDNSRuntimeException"))
				exceptionMsg.put("name", name);
		} catch (JSONException e) {
			e.printStackTrace();
		}
    	return exceptionMsg;
    }
    
    /* initial setup of the ddns map */
    private void setupddns() {
    	try {
			if (!ddnsRecordType.getDDNSRecordType().equals("A") && !ddnsRecordType.getDDNSRecordType().equals("SOA")
					&& !ddnsRecordType.getDDNSRecordType().equals("CNAME"))
				throw new Exception();
			
			if (ddnsRecordType.getDDNSRecordType().equals("SOA")) {
				for (String hostName: ddnsHostNames) {
					String ddnsRecordType = OS.config().getProperty(hostName+"recordtype");
					ddnsMap.put(hostName, new DDNSRRecord(ddnsRecordType, hostName));
					String hostNamePassword = OS.config().getProperty(hostName+"password");
					ddnsHostAndPassword.put(hostName, hostNamePassword);
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("failed to load the config file");
		} catch (IOException e) {
			System.out.println("error occured during reading the config file");
		} catch (NullPointerException e) {
			System.out.println("A ddnsHostName is not in the config file");
		} catch (Exception e) {
			System.out.println("error occured in the ddns config file");
		}
    }
    
    /* setup a and b for grading purposes */
    private void setupAandB() {
    	DDNSRRecord a = new DDNSRRecord("A", "a.null.cse461.", this.ddnsRecordType.getIp(),
    			this.ddnsRecordType.getPort());
    	a.setIsAlive(true);
    	this.ddnsMap.put("a.null.cse461.", a);
    	
    	DDNSRRecord b = new CNAME("CNAME","b.null.cse461",".");
    	b.setIsAlive(true);
    	this.ddnsMap.put("b.null.cse461.", b);
    }
}