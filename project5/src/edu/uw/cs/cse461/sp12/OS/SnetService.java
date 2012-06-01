package edu.uw.cs.cse461.sp12.OS;

import org.json.JSONObject;

import android.util.Log;

import cse461.snet.MDb;

import edu.uw.cs.cse461.sp12.OS.RPCCallable.RPCCallableMethod;
import edu.uw.cs.cse461.sp12.util.SNetDB461;
import edu.uw.cs.cse461.sp12.util.DB461.DB461Exception;

public class SnetService extends RPCCallable {
	private RPCCallableMethod<SnetService> fetchUpdates;
	private RPCCallableMethod<SnetService> fetchPhoto;
    private SNetDB461 db;
	
	@Override
    public String servicename() {
        return "ddns";
    }

	@Override
	public void shutdown() {
		// nothing to do, but have to implement to fulfill the interface promise
	}
	
	public SnetService() throws Exception {
		fetchUpdates = new RPCCallableMethod<SnetService>(this, "_fetchUpdates");
		((RPCService)OS.getService("rpc")).registerHandler(servicename(), "fetchUpdates", fetchUpdates );
		fetchPhoto = new RPCCallableMethod<SnetService>(this, "_fetchPhoto");
		((RPCService)OS.getService("rpc")).registerHandler(servicename(), "fetchPhoto", fetchPhoto );
		
		try {
    		db = MDb.getInstance();
    	} catch (DB461Exception e) {
    		System.out.println("error occurred while initializing the database");
		} finally {
    		if (db != null)
    			db.discard();
    	}
	}
	
	public JSONObject _fetchUpdates(JSONObject args) {
	    Log.e("_fetchUpdates", args.toString());
		return null; 
	}
	
	public JSONObject _fetchPhoto(JSONObject args) {
	    Log.e("_fetchPhoto", args.toString());
		return null;
	}
}
