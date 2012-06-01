package cse461.snet;

import org.json.*;

import edu.uw.cs.cse461.sp12.util.DB461.RecordSet;
import edu.uw.cs.cse461.sp12.util.*;
import edu.uw.cs.cse461.sp12.util.DB461.DB461Exception;
import edu.uw.cs.cse461.sp12.util.SNetDB461.CommunityRecord;
import edu.uw.cs.cse461.sp12.util.SNetDB461.PhotoRecord;

public class SNetProtocol {
	
	private SNetDB461 db;
	    
    public SNetProtocol(SNetDB461 db){
    	this.db = db;
    }
    
    public JSONObject fetchUpdates() {
    	JSONObject fetchUpdatesMessage = new JSONObject();
    	try {
			RecordSet<CommunityRecord> records = db.COMMUNITYTABLE.readAll();
			JSONObject communityUpdates = new JSONObject();
	    	for (CommunityRecord record : records) {
	    		JSONObject value = new JSONObject();
	    		value.put("generation", record.generation);
	    		value.put("myphotohash", record.myPhotoHash);
	    		value.put("chosenphotohash", record.chosenPhotoHash);
	    		communityUpdates.put(record.name, value);
	    	}
	    	fetchUpdatesMessage.put("communityupdates", communityUpdates);
	    	
	    	RecordSet<PhotoRecord> photoRecords = db.PHOTOTABLE.readAll();
			JSONArray needphotos = new JSONArray();
			for (PhotoRecord photoRecord: photoRecords) {
				if (photoRecord.file.equals("null")) {
					needphotos.put(photoRecord.hash);
				}
			}
	    	fetchUpdatesMessage.put("needphotos", needphotos);
		} catch (DB461Exception e) {
			System.out.println("An error occurred when creating a fetchUpdate message");
//			e.printStackTrace();
		} catch (JSONException e) {
			System.out.println("An error occurred when creating a JSONObject for fetchUpdate");
//			e.printStackTrace();
		}
    	return fetchUpdatesMessage;
    }
    
    public JSONObject fetchPhotos(int photohash) {
    	JSONObject fetchPhotosMessage = new JSONObject();
    	try {
			fetchPhotosMessage.put("photohash", photohash);
		} catch (JSONException e) {
			System.out.println("An error occurred when creating a JSONObject for fetchPhotos");
//			e.printStackTrace();
		}
    	return fetchPhotosMessage;
    }
}