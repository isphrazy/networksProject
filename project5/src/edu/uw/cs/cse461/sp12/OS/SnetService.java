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
        return "snet";
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
    	    Log.e("_fetchUpdates", "error occurred while initializing the database");
		}
		
		snet = new SNetProtocol(db);
	}
		
	public JSONObject _fetchUpdates(JSONObject args) throws IllegalArgumentException {
	    Log.e("_fetchUpdates", args.toString());
	    
    	JSONObject msg = new JSONObject();
	    try {
		    if (snet.isValidCommunityProtocol(args, "community", "needphotos")) {
		    	JSONObject community = args.getJSONObject("community");
		    	JSONObject memberField = new JSONObject();
		    	
		    	Iterator<String> it = community.keys();
	        	while (it.hasNext()) {
	        		String name = it.next().toString();
	        		JSONObject member = community.getJSONObject(name);
	        		
	        		if (!snet.isValidMemberField(member)) {
	        			throw new IllegalArgumentException("memberField in the args does not follow the SNet protocol");
	        		}
	        		
	        		CommunityRecord cRecord = db.COMMUNITYTABLE.readOne(name);
	        		if (cRecord.generation > member.getInt("generation")) {
	        			JSONObject newerMember = new JSONObject();
	        			newerMember.put("generation", cRecord.generation);
	        			newerMember.put("myphotohash", cRecord.myPhotoHash);
	        			newerMember.put("chosenphotohash", cRecord.chosenPhotoHash);
	        			memberField.put(name, newerMember);
	        		} else if (cRecord.generation < member.getInt("generation")) {
	        			cRecord.generation = member.getInt("generation");
	        			cRecord.myPhotoHash = member.getInt("myphotohash");
	        			cRecord.chosenPhotoHash = member.getInt("chosenphotohash");
	        			db.COMMUNITYTABLE.write(cRecord);
	        		}
	        	}
	        	
	        	msg.put("communityupdates", memberField);
	        	
	        	JSONArray needPhotos = args.getJSONArray("needphotos");
	        	JSONArray photoUpdates = new JSONArray();
	        	
	        	for (int i = 0; i < needPhotos.length(); i++) {
	        		int photoName = needPhotos.getInt(i);
	        		PhotoRecord photoRecord = db.PHOTOTABLE.readOne(photoName);
	        		if (photoRecord != null)
	        			photoUpdates.put(photoName);
	        	}
	        	
	        	msg.put("photoupdates", photoUpdates);
	        	
		    } else {
    			throw new IllegalArgumentException("fetchUpdate message does not follow the SNet protocol");
		    }
	    } catch (JSONException e)  {
		    Log.e("_fetchUpdates", "JSONException: " + e.getMessage());
			throw new IllegalArgumentException("Sorry, an error has occurred at my part.");
	    } catch (DB461Exception e) {
		    Log.e("_fetchUpdates", "DB461Exception: " + e.getMessage());
			throw new IllegalArgumentException("Sorry, an error has occurred at my part.");
	    } catch (Exception e) {
		    Log.e("_fetchUpdates", "Exception: " + e.getMessage());
			throw new IllegalArgumentException("Sorry, an error has occurred at my part.");
		}
	    Log.e("_fetchUpdates", "msg: " + msg);
    	return msg;
	}
	
	public JSONObject _fetchPhoto(JSONObject args) throws IllegalArgumentException {
	    Log.e("_fetchPhoto", args.toString());
    	JSONObject msg = new JSONObject();
	    try {
	    	if (snet.isValidPhotoProtocol(args, "photohash")) {
				PhotoRecord photoHash = db.PHOTOTABLE.readOne(args.getInt("photohash"));
				
				if (photoHash == null)
        			throw new IllegalArgumentException("Sorry, I do not have the photo you requested");
				
				if (photoHash.file == null)
        			throw new IllegalArgumentException("Sorry, I do not have the photo you requested");
				
				String encodedData = Base64.encodeFromFile(photoHash.file.getAbsolutePath());
		        Log.e("_fetchPhoto", photoHash.file.getAbsolutePath());

				msg.put("photohash", photoHash.hash);
				msg.put("photodata", encodedData);
			} else {
    			throw new IllegalArgumentException("Your fetchPhoto args does not follow the SNet protocol");
			}
	    } catch (DB461Exception e) {
	    	Log.e("_fetchUpdates", e.getMessage());
			throw new IllegalArgumentException("Sorry, an error has occurred at my part.");		
		} catch (JSONException e) {
			Log.e("_fetchUpdates", e.getMessage());
			throw new IllegalArgumentException("Sorry, an error has occurred at my part.");
		} catch (IOException e) {
			Log.e("_fetchUpdates", e.getMessage());
			throw new IllegalArgumentException("Sorry, an error has occurred at my part.");		
		}
		return msg;
	}
}
