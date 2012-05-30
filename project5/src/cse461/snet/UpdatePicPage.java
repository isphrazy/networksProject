package cse461.snet;

import cse461.snet.R;
import edu.uw.cs.cse461.sp12.util.*;
import edu.uw.cs.cse461.sp12.util.DB461.DB461Exception;
import edu.uw.cs.cse461.sp12.util.DB461.*;
import edu.uw.cs.cse461.sp12.util.SNetDB461.*;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class UpdatePicPage extends Activity {
    
    Spinner spinner;
    ArrayAdapter<String> adapter;
    SNetDB461 db;
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_layout);
        
        initVars();
    }
    
    private void initVars() {
    	try {
    		db = new SNetDB461();
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
    }


    public void beFriend(View view){
        String friend = spinner.getSelectedItem().toString();
        try {
			CommunityRecord record = db.COMMUNITYTABLE.readOne(friend);
			if (record.isFriend == false) {
				record.isFriend = true;
				db.COMMUNITYTABLE.write(record);
			}
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
        
    }
    
    public void fixDb(View view){
        
    }
}
