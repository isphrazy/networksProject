package edu.uw.cs.cse461.sp12.OS;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.sp12.OS.RPCCallable.RPCCallableMethod;

class DDNSService extends RPCCallable{
	
	// A variable capable of describing a method that can be invoked by RPC.
	private RPCCallableMethod<DDNSService> ddns;
	
    @Override
    public String servicename() {
        return "ddns";
    }

    @Override
    public void shutdown() {
		// nothing to do, but have to implement to fulfill the interface promise
    }
    
    public DDNSService() throws Exception{
    	// Set up the method descriptor variable to refer to this->_register()
		//ddns = new RPCCallableMethod<DDNSService>(this, "_register");
		// Register the method with the RPC service as externally invocable method "register"
		//((RPCService)OS.getService("rpc")).registerHandler(servicename(), "_register", ddns );
		int serverport = Integer.parseInt(OS.config().getProperty("rpc.serverport"));
    	String hostName = OS.config().getProperty("host.name");
		
    	((DDNSResolverService)OS.getService("ddnsresolver")).register(new DDNSFullName(hostName), serverport );
    	
    }
    
    public void unregister(DDNSFullName hostname) {
        
    }
}
