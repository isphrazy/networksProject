package cse461.snet;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cse461.snet.R;
import edu.uw.cs.cse461.sp12.OS.DDNSException.DDNSNoAddressException;
import edu.uw.cs.cse461.sp12.OS.DDNSException.DDNSNoSuchNameException;
import edu.uw.cs.cse461.sp12.OS.DDNSRRecord;
import edu.uw.cs.cse461.sp12.OS.DDNSResolverService;
import edu.uw.cs.cse461.sp12.OS.OS;
import edu.uw.cs.cse461.sp12.OS.RPCCallerSocket;
import edu.uw.cs.cse461.sp12.util.*;
import edu.uw.cs.cse461.sp12.util.DB461.DB461Exception;
import edu.uw.cs.cse461.sp12.util.DB461.*;
import edu.uw.cs.cse461.sp12.util.SNetDB461.*;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

public class UpdatePicPage extends Activity {
	
    Spinner spinner;
    ArrayAdapter<String> adapter;
    SNetDB461 db;
    SNetProtocol snet;
    DDNSResolverService resolver;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_layout);
        
        initVars();
    }
    
    private void initVars() {
    	try {
    		db = MDb.getInstance();
    	} catch (DB461Exception e) {
	        Toast.makeText(this, "An error occurred while initializing the database", Toast.LENGTH_SHORT).show();
		} 
    	
        spinner = (Spinner) findViewById(R.id.spinner);
        try {
			RecordSet<CommunityRecord> allRecords = db.COMMUNITYTABLE.readAll();
			adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
			for (CommunityRecord record: allRecords) {
				adapter.add(record.name);
			}
		} catch (DB461Exception e) {
	        Toast.makeText(this, "An error occurred while putting names into the spinner", Toast.LENGTH_SHORT).show();
		}
        
        spinner.setAdapter(adapter);
        snet = new SNetProtocol(db);
        resolver = ((DDNSResolverService)OS.getService("ddnsresolver"));
    }


    public void beFriend(View view) {
        Log.e("beFriend", "in");
        String friend = spinner.getSelectedItem().toString();
        try {
			
			DDNSRRecord ddnsRecord = resolver.resolve(friend);
			RPCCallerSocket callerSocket = new RPCCallerSocket(ddnsRecord.getIp(), ddnsRecord.getIp(), "" + ddnsRecord.getPort());
	        JSONObject fetchUpdate = snet.fetchUpdates();
	        JSONObject response = callerSocket.invoke("snet", "fetchUpdates", fetchUpdate);
	        
	        if (response.has("msg")) {
    			throw new IllegalArgumentException("Received an error msg" + response.getString("msg"));
    		}
	        
	        fetchUpdates(response);
	        
	        CommunityRecord record = db.COMMUNITYTABLE.readOne(friend);
            if (record.isFriend == false)
                record.isFriend = true;
            
            db.COMMUNITYTABLE.write(record);
            if (db.PHOTOTABLE.readOne(record.myPhotoHash) == null) {
                PhotoRecord myPhoto = db.PHOTOTABLE.createRecord();
                myPhoto.file = null;
                myPhoto.hash = record.myPhotoHash;
                myPhoto.refCount = 1;
                db.PHOTOTABLE.write(myPhoto);
            }
            if (db.PHOTOTABLE.readOne(record.chosenPhotoHash) == null) {
                PhotoRecord chosenPhoto = db.PHOTOTABLE.createRecord();
                chosenPhoto.file = null;
                chosenPhoto.hash = record.chosenPhotoHash;
                chosenPhoto.refCount = 1;
                db.PHOTOTABLE.write(chosenPhoto);
            }
            
	        fetchPhotos(response, friend);
	        
		} catch (DDNSNoAddressException e) {
	        Toast.makeText(this, friend + " is currently offline", Toast.LENGTH_SHORT).show();
		} catch (DDNSNoSuchNameException e) {
	        Toast.makeText(this, friend + " is not in the community", Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
	        Toast.makeText(this, "Problem occurred while connecting to " + friend , Toast.LENGTH_SHORT).show();
		} catch (JSONException e) {
			if (e.getMessage().isEmpty()) {
				Toast.makeText(this, "Problem occurred while reading the fetchupdate response from " + friend,
	        		Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		} catch (DB461Exception e) {
			if (e.getMessage().isEmpty()) {
				Toast.makeText(this, "Problem occurred while reading the db in contact", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		} catch (IllegalArgumentException e) {
	        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
		}
    }
    
    public void unFriend(View view){
    	String friend = spinner.getSelectedItem().toString();
        try {
			CommunityRecord record = db.COMMUNITYTABLE.readOne(friend);
			if (record.isFriend == true) {
				record.isFriend = false;
				db.COMMUNITYTABLE.write(record);
			}
			if (db.PHOTOTABLE.readOne(record.myPhotoHash) != null)
				db.PHOTOTABLE.delete(record.myPhotoHash);
			if (db.PHOTOTABLE.readOne(record.chosenPhotoHash) != null)
				db.PHOTOTABLE.delete(record.chosenPhotoHash);
			
        } catch (DB461Exception e) {
	        Toast.makeText(this, "A problem occurred when unfriending " + friend, Toast.LENGTH_SHORT).show();
		}
    }
    
//    private class AdapterComparater extends Comparater<>{
//        
//    }
    
    public void contact(View view){
        String contact = spinner.getSelectedItem().toString();
        try {
			DDNSRRecord record = resolver.resolve(contact);
	        RPCCallerSocket callerSocket = new RPCCallerSocket(record.getIp(), record.getIp(), "" + record.getPort());
	        JSONObject fetchUpdate = snet.fetchUpdates();
	        JSONObject response = callerSocket.invoke("snet", "fetchUpdates", fetchUpdate);
	        
	        if (response.has("msg")) {
    			throw new IllegalArgumentException("Received an error msg" + response.getString("msg"));
    		}
	        
	        Log.e("contact", "response: " + response);
	        
	        fetchUpdates(response);
	        
	        Log.e("contact", "community db: " + db.COMMUNITYTABLE);
	        adapter.clear();
	        for (CommunityRecord communityRecord: db.COMMUNITYTABLE.readAll()) {
	            adapter.add(communityRecord.name);
	        }
//	        adapter.sort(comparator);
	        fetchPhotos(response, contact);
	        
		} catch (DDNSNoAddressException e) {
	        Toast.makeText(this, contact + " is currently offline", Toast.LENGTH_SHORT).show();
		} catch (DDNSNoSuchNameException e) {
	        Toast.makeText(this, contact + " is not in the community", Toast.LENGTH_SHORT).show();
		} catch (IOException e) {
	        Toast.makeText(this, "Problem occurred while connecting to " + contact , Toast.LENGTH_SHORT).show();
		} catch (JSONException e) {
			if (e.getMessage().isEmpty()) {
				Toast.makeText(this, "Problem occurred while reading the fetchupdate response from " + contact,
	        		Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		} catch (DB461Exception e) {
			if (e.getMessage().isEmpty()) {
				Toast.makeText(this, "Problem occurred while reading the db in contact", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		} catch (IllegalArgumentException e) {
	        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
		}
    }
    
    public void fixDb(View view){
        try {
			db.checkAndFixDB(snet.dir);
		} catch (DB461Exception e) {
	        Toast.makeText(this, "Problem occurred when fixDB", Toast.LENGTH_SHORT).show();
		}
    }
    
    private void fetchUpdates(JSONObject response) throws JSONException, DB461Exception, IllegalArgumentException {
    	Log.e("fetch updates", "response: " + response);
        if (snet.isValidCommunityProtocol(response, "communityupdates", "photoupdates")) {
        	JSONObject communityupdates = response.getJSONObject("communityupdates");
			Iterator<String> it = communityupdates.keys();
        	while (it.hasNext()) {
        		String name = it.next().toString();
        		CommunityRecord communityRecord = db.COMMUNITYTABLE.readOne(name);
        		if (communityRecord == null) {
        			communityRecord = db.createCommunityRecord();
        			communityRecord.name = name;
        		}
        		
        		JSONObject receivedCommunityRecord = communityupdates.getJSONObject(name);
        		
        		if (!snet.isValidMemberField(receivedCommunityRecord))
        			throw new IllegalArgumentException("memberField in the received " +
        					"communityupdates does not follow the SNet protocol");
        		
        		if (receivedCommunityRecord.getInt("generation") >= communityRecord.generation) {
        			communityRecord.generation = receivedCommunityRecord.getInt("generation");
        			
        			int chosenphotohash = receivedCommunityRecord.getInt("chosenphotohash");
        			if (communityRecord.chosenPhotoHash != chosenphotohash) {
        				PhotoRecord pRecord = db.PHOTOTABLE.readOne(chosenphotohash);
        				if (pRecord != null ) {
        					pRecord.refCount += 1;
        					db.PHOTOTABLE.write(pRecord);
        				}
        				pRecord = db.PHOTOTABLE.readOne(communityRecord.chosenPhotoHash);
        				if (pRecord != null ) {
        					pRecord.refCount -= 1;
        					if (pRecord.refCount < 0) pRecord.refCount = 0; 
        					db.PHOTOTABLE.write(pRecord);
        				}
        			}
        			communityRecord.chosenPhotoHash = chosenphotohash;
        			
        			int myphotohash = receivedCommunityRecord.getInt("myphotohash");
        			if (communityRecord.myPhotoHash != myphotohash) {
        				PhotoRecord pRecord = db.PHOTOTABLE.readOne(myphotohash);
        				if (pRecord != null ) {
        					pRecord.refCount += 1;
        					db.PHOTOTABLE.write(pRecord);
        				}
        				pRecord = db.PHOTOTABLE.readOne(communityRecord.myPhotoHash);
        				if (pRecord != null ) {
        					pRecord.refCount -= 1;
        					if (pRecord.refCount < 0) pRecord.refCount = 0; 
        					db.PHOTOTABLE.write(pRecord);
        				}
        			}
        			communityRecord.myPhotoHash = myphotohash;
        			db.COMMUNITYTABLE.write(communityRecord);
        		}
        	}
        } else {
        	throw new IllegalArgumentException("received message for fetchUpdates does not follow the SNet protocol");
        }
    }
    
    private void fetchPhotos(JSONObject response, String contact) 
    		throws IOException, JSONException, DB461Exception, IllegalArgumentException, DDNSNoAddressException, DDNSNoSuchNameException {
        if (snet.isValidPhotoProtocol(response, "photoupdates")) {
	        JSONArray array = response.getJSONArray("photoupdates");
	        for (int i = 0; i < array.length(); i++) {
	        	int photo = array.getInt(i);
	        	if (photo == 0) continue;
	        	PhotoRecord photoRecord = db.PHOTOTABLE.readOne(photo);
	        	if (photoRecord == null || photoRecord.file == null) {
	        	    DDNSRRecord record;
                    try {
                        record = resolver.resolve(contact);
                    } catch (DDNSNoAddressException e) {
                        throw e;
                    } catch (DDNSNoSuchNameException e) {
                        throw e;
                    }
	                RPCCallerSocket callerSocket = new RPCCallerSocket(record.getIp(), record.getIp(), "" + record.getPort());
	                
	        		JSONObject photoRequest = snet.fetchPhotos(photo);
	        		JSONObject photoResponse = callerSocket.invoke("snet", "fetchPhoto", photoRequest);
	        		
	        		if (photoResponse.has("msg")) {
	        			throw new IllegalArgumentException("Received an error msg" + photoResponse.getString("msg"));
	        		}
	        		
	        		Log.e("fetchPhotos", "photoResponse: " + photoResponse.toString());
	        		if (snet.isValidPhotoResponse(photo, photoResponse)) {
	        			String photoPath = snet.dir.getAbsolutePath() + "/" + Integer.toString(photo) +".jpg";
	        			
	        			Base64.decodeToFile(photoResponse.getString("photodata"), photoPath);
	        			
        				File photoFile = new File(photoPath);
	        			if (photoRecord == null) {
	        				photoRecord = db.createPhotoRecord();
	        				photoRecord.file = photoFile;
	        				photoRecord.hash = photo;
	        				photoRecord.refCount = 1;
	        			} else if (photoRecord.file == null) {
	        				photoRecord.file = photoFile;
	        				photoRecord.refCount++;
	        			}
	        			db.PHOTOTABLE.write(photoRecord);
	        		} else {
	                	throw new IllegalArgumentException("received message for fetchPhoto does not follow the SNet protocol or the photo received" +
	                			"is not the same as the photo requested");
	        		}
	        	}
	        }
        } else {
        	throw new IllegalArgumentException("received message for fetchPhoto does not follow the SNet protocol");
        }
    }
}
