package cse461.snet;

import java.io.IOException;

import org.json.JSONObject;

import cse461.snet.R;
import edu.uw.cs.cse461.sp12.OS.DDNSException.DDNSNoAddressException;
import edu.uw.cs.cse461.sp12.OS.DDNSException.DDNSNoSuchNameException;
import edu.uw.cs.cse461.sp12.OS.DDNSRRecord;
import edu.uw.cs.cse461.sp12.OS.DDNSResolverService;
import edu.uw.cs.cse461.sp12.OS.OS;
import edu.uw.cs.cse461.sp12.OS.RPCCallerSocket;
import edu.uw.cs.cse461.sp12.util.*;
import edu.uw.cs.cse461.sp12.util.DB461.*;
import edu.uw.cs.cse461.sp12.util.SNetDB461.*;
import android.app.Activity;
import android.os.Bundle;
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
    		System.out.println("error occurred while initializing the database");
		} finally {
    		if (db != null)
    			db.discard();
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
			}
			// TODO: FetchUpdate and get photos
        } catch (DB461Exception e) {
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
        } catch (DB461Exception e) {
			e.printStackTrace();
		}
    }
    
    public void contact(View view){
        String contact = spinner.getSelectedItem().toString();
        try {
			DDNSRRecord record = resolver.resolve(contact);
	        RPCCallerSocket callerSocket = new RPCCallerSocket(record.getIp(), record.getIp(), "" + record.getPort());
	        JSONObject fetchUpdate = snet.fetchUpdates();
	        callerSocket.invoke("snet", "fetchUpdates", fetchUpdate);
	        // TODO Update stuff
	        
	        
	        // TODO Request photos
	        
	        
		} catch (DDNSNoAddressException e) {
	        Toast.makeText(this, contact + " is currently offline", Toast.LENGTH_SHORT).show();
//			e.printStackTrace();
		} catch (DDNSNoSuchNameException e) {
	        Toast.makeText(this, contact + " is not in the community", Toast.LENGTH_SHORT).show();
//			e.printStackTrace();
		} catch (IOException e) {
	        Toast.makeText(this, "Problem occurred while connecting to " + contact , Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
    }
    
    public void fixDb(View view){
        
    }
}
