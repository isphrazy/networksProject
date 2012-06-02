package edu.uw.cs.cse461.sp12.OS;

import java.io.IOException;
import java.util.Iterator;

import org.json.*;

import android.util.Log;

import cse461.snet.MDb;
import cse461.snet.SNetProtocol;

import edu.uw.cs.cse461.sp12.OS.RPCCallable.RPCCallableMethod;
import edu.uw.cs.cse461.sp12.util.Base64;
import edu.uw.cs.cse461.sp12.util.DB461.RecordSet;
import edu.uw.cs.cse461.sp12.util.SNetDB461;
import edu.uw.cs.cse461.sp12.util.DB461.DB461Exception;
import edu.uw.cs.cse461.sp12.util.SNetDB461.*;

public class SnetService extends RPCCallable {
	private RPCCallableMethod<SnetService> fetchUpdates;
	private RPCCallableMethod<SnetService> fetchPhoto;
    private SNetDB461 db;
    private SNetProtocol snet;
	
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
		snet = new SNetProtocol(db);
	}
	
	//TODO throw exception for error cases
	
	public JSONObject _fetchUpdates(JSONObject args) {
	    Log.e("_fetchUpdates", args.toString());
	    try {
		    if (snet.isValidCommunityProtocol(args, "community", "needphotos")) {
		    	JSONObject msg = new JSONObject();
		    	JSONObject community = args.getJSONObject("community");
		    	JSONObject memberField = new JSONObject();
		    	
		    	Iterator<String> it = community.keys();
	        	while (it.hasNext()) {
	        		String name = it.next().toString();
	        		JSONObject member = community.getJSONObject(name);
	        		
	        		//TODO
	        		if (!snet.isValidMemberField(member)) {
	        			break;
	        		}
	        		
	        		CommunityRecord cRecord = db.COMMUNITYTABLE.readOne(name);
	        		if (cRecord.generation > member.getInt("generation")) {
	        			JSONObject newerMember = new JSONObject();
	        			newerMember.put("generation", cRecord.generation);
	        			newerMember.put("myphotohash", cRecord.myPhotoHash);
	        			newerMember.put("chosenphotohash", cRecord.chosenPhotoHash);
	        			memberField.put(name, newerMember);
	        		} else if (cRecord.generation < member.getInt("generation")) {
	        			//TODO update ref count as well
	        		}
	        	}
	        	
	        	msg.put("communityupdates", memberField);
	        	
	        	JSONArray needPhotos = community.getJSONArray("needphotos");
	        	JSONArray photoUpdates = new JSONArray();
	        	
	        	RecordSet<PhotoRecord> photoRecords = db.PHOTOTABLE.readAll();
	        	for (int i = 0; i < needPhotos.length(); i++) {
	        		int photoName = needPhotos.getInt(i);
	        		if (photoRecords.contains(photoName))
	        			photoUpdates.put(photoName);
	        	}
	        	
	        	msg.put("photoupdates", photoUpdates);
	        	
	        	return msg;
		    }
	    } catch (JSONException e)  {
	    	
	    } catch (DB461Exception e) {
//			e.printStackTrace();
		}
		return null; 
	}
	
	public JSONObject _fetchPhoto(JSONObject args) {
	    Log.e("_fetchPhoto", args.toString());
	    try {
	    	if (snet.isValidPhotoProtocol(args, "photohash")) {
				PhotoRecord photoHash = db.PHOTOTABLE.readOne(args.getInt("photohash"));
				if (photoHash == null) {
					// throw exception
				}
				
				if (photoHash.file == null) {
					// throw exception
				}
				
				String encodedData = Base64.encodeFromFile("");
			}
	    } catch (DB461Exception e) {
			e.printStackTrace();
		} catch (JSONException e) {
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
