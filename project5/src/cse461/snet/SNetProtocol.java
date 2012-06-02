package cse461.snet;

import java.io.File;
import org.json.*;

import android.os.Environment;

import edu.uw.cs.cse461.sp12.util.DB461.RecordSet;
import edu.uw.cs.cse461.sp12.util.*;
import edu.uw.cs.cse461.sp12.util.DB461.DB461Exception;
import edu.uw.cs.cse461.sp12.util.SNetDB461.CommunityRecord;
import edu.uw.cs.cse461.sp12.util.SNetDB461.PhotoRecord;
import android.widget.Toast;


public class SNetProtocol {
	
	public static final String PICTURE_FOLDER = "snet_pics/";
	public static final File sdCard = Environment.getExternalStorageDirectory();
    public static final File dir = new File (sdCard.getAbsolutePath() + "/" + PICTURE_FOLDER);
	
	private SNetDB461 db;
	    
    public SNetProtocol(SNetDB461 db){
    	this.db = db;
    }
    
    public JSONObject fetchUpdates() throws DB461Exception, JSONException {
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
	    	fetchUpdatesMessage.put("community", communityUpdates);
	    	
	    	RecordSet<PhotoRecord> photoRecords = db.PHOTOTABLE.readAll();
			JSONArray needphotos = new JSONArray();
			for (PhotoRecord photoRecord: photoRecords) {
				if (photoRecord.file == null)
					needphotos.put(photoRecord.hash);
			}
	    	fetchUpdatesMessage.put("needphotos", needphotos);
		} catch (DB461Exception e) {
			throw new DB461Exception("An error occurred when creating a fetchUpdate message");
		} catch (JSONException e) {
			throw new JSONException("An error occurred when creating a JSONObject for fetchUpdate");
		}
    	return fetchUpdatesMessage;
    }
    
    public JSONObject fetchPhotos(int photohash) throws JSONException {
    	JSONObject fetchPhotosMessage = new JSONObject();
    	try {
			fetchPhotosMessage.put("photohash", photohash);
		} catch (JSONException e) {
			throw new JSONException("An error occurred when creating a JSONObject for fetchPhoto");
		}
    	return fetchPhotosMessage;
    }
    
    public boolean isValidCommunityProtocol(JSONObject response, String community, String photo) {
    	return response.has(community) && response.has(photo);
    }
    
    public boolean isValidMemberField(JSONObject record) {
    	return record.has("generation") && record.has("myphotohash") && record.has("chosenphotohash");
    }
    
    public boolean isValidPhotoProtocol(JSONObject response, String photo) {
    	return response.has(photo);
    }
    
    public boolean isValidPhotoResponse(int hash, JSONObject response) {
    	try {
			return response.has("photohash") && response.has("photodata") && response.getInt("photohash") == hash;
		} catch (JSONException e) {
			return false;
		}
    }
}
