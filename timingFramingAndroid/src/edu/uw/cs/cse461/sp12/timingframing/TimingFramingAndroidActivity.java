package edu.uw.cs.cse461.sp12.timingframing;

import java.io.IOException;

import org.json.JSONException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class TimingFramingAndroidActivity extends Activity implements Client.ClientListener {
	public static final String TAG = "TimingFramingAndroidActivity";
    public static final String PREFS_NAME = "CSE461";

    private Client mClient;
    private Thread mClientThread;
	private String mServerHost;
	private int mServerPort;
	private int mServerInterSymbolTime;
	
	private EditText portET;
	private EditText interSymbolET;
	
	private boolean customInterval;
	
	/** Called when the activity is first created.  Establishes the UI.  Reads state information saved by previous runs. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.e("oncreate", "in");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mClient = new Client();
        mClient.addListener(this);
        
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);
        mServerHost = settings.getString("serverhost", Properties.SERVER_HOST);
        int defaultPort = Properties.SERVER_PORT_NEGOTIATE + Properties.SERVER_PORT_INTERSYMBOL_TIME_VEC.length;
        mServerPort = settings.getInt("serverport", defaultPort );
        
        
        portET = (EditText)findViewById(R.id.portText);
        ((TextView)findViewById(R.id.hostText)).setText(mServerHost);
        ((TextView)findViewById(R.id.portText)).setText(new Integer(mServerPort).toString());
        
        setLayout();
        
    }
    CheckBox isCheck;
    CheckBox portCheck;
    private void setLayout() {
    	portET = (EditText)findViewById(R.id.portText);
        interSymbolET = (EditText)findViewById(R.id.isET);
        
        isCheck = (CheckBox)findViewById(R.id.isCheck);
        portCheck = (CheckBox)findViewById(R.id.portCheck);
        
        
        isCheck.setOnClickListener(new OnClickListener() {
        	 
      	  @Override
      	  public void onClick(View v) {
                      //is chkIos checked?
      		if (((CheckBox) v).isChecked()) {
      			portCheck.setChecked(false);
      		}
      	  }
      	});
        
        portCheck.setOnClickListener(new OnClickListener() {
       	 
        	  @Override
        	  public void onClick(View v) {
                        //is chkIos checked?
        		if (((CheckBox) v).isChecked()) {
        			isCheck.setChecked(false);
        		}
        	  }
        	});
        
	}

	/**
     * Called when activity is stopped.  Save user preferences for next execution.
     */
    @Override
    protected void onStop() {
    	super.onStop();
    	
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString("serverhost", mServerHost);
    	editor.putInt("serverport", mServerPort);
    	editor.commit();
    }
    
    /**
     * Fetch the data entered through the UI by the user.
     * @return
     */
    private boolean readUserInputs() {
        mServerHost = ((TextView)findViewById(R.id.hostText)).getText().toString();
        mServerPort = Integer.parseInt(((TextView)findViewById(R.id.portText)).getText().toString());
        try {
        	mServerInterSymbolTime = mClient.portToIntersymbolTime(mServerPort, Properties.SERVER_INTER_SYMBOL_TIME);
        } catch (Exception e) {
        	// display a fleeting error message
    		Toast toast = Toast.makeText(getApplicationContext(), "Valid port numbers are " + Properties.SERVER_PORT_NEGOTIATE + "-" + 
    							(Properties.SERVER_PORT_NEGOTIATE + Properties.SERVER_PORT_INTERSYMBOL_TIME_VEC.length), Toast.LENGTH_LONG);
    		toast.show();
    		return false;
    	}
    	return true;
    }
    
    /**
     * Start/stop toggle button click handler.  (The association of a button click with the 
     * invocation of this routine is made in the layout object.)
     */
    public void onToggleClicked(View v) {
    	if (((ToggleButton)v).isChecked()) {
			((TextView)findViewById(R.id.asyncText)).setText("");
			((TextView)findViewById(R.id.syncedText)).setText("");
			if ( !readUserInputs() ) {
				((ToggleButton)v).setChecked(false);
				return;
			}
    		// Reading chars sent by server is done in a background thread, so that the UI remains responsive.
    		mClientThread = new Thread() {
    			public void run() {
    				try {
    					if(portCheck.isChecked()){
    						mServerPort = Integer.parseInt(portET.getText().toString());
    						mServerInterSymbolTime = Properties.SERVER_PORT_INTERSYMBOL_TIME_VEC[mServerPort - Properties.SERVER_PORT_NEGOTIATE - 1];
    						
    					}else{
    						mServerPort = Properties.SERVER_PORT_NEGOTIATE;
    						mServerInterSymbolTime = Integer.parseInt(interSymbolET.getText().toString());
    					}
    					
    						
        				mClient.reset();
    					mClient.connect(mServerHost, mServerPort, mServerPort == Properties.SERVER_PORT_NEGOTIATE, mServerInterSymbolTime);
    					   
    					runOnUiThread(new Runnable() {
    		    			public void run() {
    		    				ToggleButton startstopToggle = (ToggleButton)findViewById(R.id.toggleStartStop);
    		    				startstopToggle.setChecked(false);
    		    			}
    		    		});
    				} catch (IOException e) {
    					runOnUiThread( new Runnable() {
    						public void run() {
    	    					Toast toast = Toast.makeText(getApplicationContext(), "Can't connect to " + mServerHost + ":" + mServerPort, Toast.LENGTH_SHORT);
    	    					toast.show();
    						}
    					});
    				} catch (JSONException e) {
						e.printStackTrace();
					} catch (NumberFormatException fe){
						Toast.makeText(TimingFramingAndroidActivity.this, "input data is invalid, please input number", Toast.LENGTH_SHORT).show();
					}
    			}
    		};
    		mClientThread.start();
    	} else {
    		if ( mClient != null ) mClient.stop();
    	}
    }

    /**
     * Helper class to get onChar activity onto UI thread
     * <p>
     * The UI can be updated only by the UI (main) thread.  Characters are read by 
     * a background thread.  The background thread needs to get data to the UI thread
     * to update the display when characters arrive.  This class is useful for establishing
     * that inter-thread communication.
     */
    private class OnCharClass implements Runnable {
    	private static final int MAXCHARS = 25;
    	int type;
    	char c;
    	Client mClient;
    	public OnCharClass(int t, char ch, Client client) {
    		type = t;
    		c = ch;
    		mClient = client;
    	}
		public void run() {
			int textviewId = 0;
			if ( type == Client.TYPE_SYNC ) textviewId = R.id.syncedText;
			else if ( type == Client.TYPE_ASYNC ) textviewId = R.id.asyncText;
			else throw new RuntimeException("Unknown type in ConsoleClient.onChar: " + type);
			
			TextView textView = (TextView)findViewById(textviewId);
			//int textLength = textView.getText().length();
			int textLength = textView.length();
			int start = textLength > MAXCHARS ? textLength-MAXCHARS : 0;
			String text = textView.getText().toString().substring(start) + c;
			textView.setText(text);
			
			textView = (TextView)findViewById(R.id.charsreadText);
			textView.setText(new Integer(mClient.getNumMatchingChars()).toString());
		}
    }
    
    /**
     * Callback from Client object when a character is read, synchronously or asynchronously.
     */
    @Override
	public void onChar(int type, char c) {
		runOnUiThread( new OnCharClass(type, c, this.mClient) );
	}
    
    
    
}