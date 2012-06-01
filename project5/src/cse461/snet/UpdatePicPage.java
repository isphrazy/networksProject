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
    
	private static final String PICTURE_FOLDER = "snet_pics/";
	private static final File sdCard = Environment.getExternalStorageDirectory();
    private static final File dir = new File (sdCard.getAbsolutePath() + "/" + PICTURE_FOLDER);
	
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
    		System.out.println("error occurred while initializing the database");
		} 
    	
        spinner = (Spinner) findViewById(R.id.spinner);
        try {
			RecordSet<CommunityRecord> allRecords = db.COMMUNITYTABLE.readAll();
			adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item);
			for (CommunityRecord record: allRecords) {
				adapter.add(record.name);
			}
		} catch (DB461Exception e) {
			System.out.println("An error occurred while putting names into the spinner");
		}
        
        spinner.setAdapter(adapter);
        snet = new SNetProtocol(db);
        resolver = ((DDNSResolverService)OS.getService("ddnsresolver"));
    }


    public void beFriend(View view){
        String friend = spinner.getSelectedItem().toString();
        try {
			CommunityRecord record = db.COMMUNITYTABLE.readOne(friend);
			if (record.isFriend == false) {
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
			}
			DDNSRRecord ddnsRecord = resolver.resolve(friend);
			RPCCallerSocket callerSocket = new RPCCallerSocket(ddnsRecord.getIp(), ddnsRecord.getIp(), "" + ddnsRecord.getPort());
	        JSONObject fetchUpdate = snet.fetchUpdates();
	        JSONObject response = callerSocket.invoke("snet", "fetchUpdates", fetchUpdate);
	        
	        fetchUpdates(response);
	        
	        fetchPhotos(response, callerSocket);
	        
        } catch (DB461Exception e) {
			e.printStackTrace();
		} catch (DDNSNoAddressException e) {
			e.printStackTrace();
		} catch (DDNSNoSuchNameException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
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
//			e.printStackTrace();
		}
    }
    
    public void contact(View view){
        String contact = spinner.getSelectedItem().toString();
        try {
			DDNSRRecord record = resolver.resolve(contact);
	        RPCCallerSocket callerSocket = new RPCCallerSocket(record.getIp(), record.getIp(), "" + record.getPort());
	        JSONObject fetchUpdate = snet.fetchUpdates();
	        JSONObject response = callerSocket.invoke("snet", "fetchUpdates", fetchUpdate);
	        Log.e("contact", "response: " + response);
	        
	        fetchUpdates(response);
	        Log.e("contact", "community db: " + db.COMMUNITYTABLE);
	        adapter.clear();
	        for (CommunityRecord communityRecord: db.COMMUNITYTABLE.readAll()) {
	            adapter.add(communityRecord.name);
	        }
	        
	        fetchPhotos(response, callerSocket);
	        
		} catch (DDNSNoAddressException e) {
	        Toast.makeText(this, contact + " is currently offline", Toast.LENGTH_SHORT).show();
//			e.printStackTrace();
		} catch (DDNSNoSuchNameException e) {
	        Toast.makeText(this, contact + " is not in the community", Toast.LENGTH_SHORT).show();
//			e.printStackTrace();
		} catch (IOException e) {
	        Toast.makeText(this, "Problem occurred while connecting to " + contact , Toast.LENGTH_SHORT).show();
//			e.printStackTrace();
		} catch (JSONException e) {
	        Toast.makeText(this, "Problem occurred while reading the fetchupdate response from " + contact,
	        		Toast.LENGTH_SHORT).show();
//			e.printStackTrace();
		} catch (DB461Exception e) {
	        Toast.makeText(this, "Problem occurred while reading the db in contact", Toast.LENGTH_SHORT).show();
//			e.printStackTrace();
		}
    }
    
    public void fixDb(View view){
        try {
			db.checkAndFixDB(dir);
		} catch (DB461Exception e) {
	        Toast.makeText(this, "Problem occurred when fixDB", Toast.LENGTH_SHORT).show();
//			e.printStackTrace();
		}
    }
    
    private boolean isValidCommunityProtocol(JSONObject response) {
    	return response.has("communityupdates") && response.has("photoupdates");
    }
    
    private boolean isValidMemberField(JSONObject record) {
    	return record.has("generation") && record.has("myphotohash") && record.has("chosenphotohash");
    }
    
    private boolean isValidPhotoProtocol(JSONObject response) {
    	return response.has("photoupdates");
    }
    
    private boolean isValidPhotoResponse(int hash, JSONObject response) {
    	try {
			return response.has("photohash") && response.has("photodata") && response.getInt("photohash") == hash;
		} catch (JSONException e) {
			return false;
//			e.printStackTrace();
		}
    }
    
    private void fetchUpdates(JSONObject response) throws JSONException, DB461Exception {
    	if (isValidCommunityProtocol(response)) {
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
        		
        		if (!isValidMemberField(receivedCommunityRecord))
        			break;
        		
        		if (receivedCommunityRecord.getInt("generation") >= communityRecord.generation) {
        			communityRecord.generation = receivedCommunityRecord.getInt("generation");
        			communityRecord.chosenPhotoHash = receivedCommunityRecord.getInt("chosenphotohash");
        			communityRecord.myPhotoHash = receivedCommunityRecord.getInt("myphotohash");
        			db.COMMUNITYTABLE.write(communityRecord);
        		}
        	}
        }
    }
    
    private void fetchPhotos(JSONObject response, RPCCallerSocket callerSocket) 
    		throws IOException, JSONException, DB461Exception {
        if (isValidPhotoProtocol(response)) {
	        JSONArray array = response.getJSONArray("photoupdates");
	        for (int i = 0; i < array.length(); i++) {
	        	int photo = array.getInt(i);
	        	PhotoRecord photoRecord = db.PHOTOTABLE.readOne(photo);
	        	if (photoRecord == null || photoRecord.file == null) {
	        		JSONObject photoRequest = snet.fetchPhotos(photo);
	        		JSONObject photoResponse = callerSocket.invoke("snet", "fetchPhoto", photoRequest);
	        		if (isValidPhotoResponse(photo, photoResponse)) {
	        			String photoPath = dir.getPath() + "/" + Integer.toString(photo) +".jpg";
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
	        		}
	        	}
	        }
        }
    }
}
