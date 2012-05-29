package cse461.snet;

import java.util.Properties;

import cse461.snet.R;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class UpdatePicPage extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_layout);
    }
    
    
    public void beFriend(View view){
        Log.e("befiend", "befriend");
    }
    
    
    public void unFriend(View view){
        
    }
    
    
    public void contact(View view){
        
    }
    
    
    public void fixDb(View view){
        
    }
    
    
}
