package cse461.snet;

import java.util.Properties;

import cse461.snet.R;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;

public class UpdatePicPage extends Activity {
    
    Spinner spinner;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_layout);
        
        initVars();
    }
    
    
    private void initVars() {
        spinner = (Spinner) findViewById(R.id.spinner);
//        spinner.setSelection(0);
    }


    public void beFriend(View view){
        
    }
    
    
    public void unFriend(View view){
        
    }
    
    
    public void contact(View view){
        
    }
    
    
    public void fixDb(View view){
        
    }
    
    
}
