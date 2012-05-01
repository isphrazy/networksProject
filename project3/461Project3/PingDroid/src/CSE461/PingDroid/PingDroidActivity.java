package CSE461.PingDroid;

import java.io.IOException;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.sp12.OS.OS;
import edu.uw.cs.cse461.sp12.OS.RPCCallerSocket;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class PingDroidActivity extends Activity {
	
	private Properties config;
	
	private EditText ip_et;
	private EditText port_et;
	private Button ping_b;
	private TextView ping_result_tv;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getLayouts();
        config = new Properties();
        
		try {
//			config.load(getAssets().open("jz.cse461.config.ini"));
			config.load(getAssets().open("foo.bar.config.ini"));
			OS.boot(config);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			OS.boot(config);
		} catch (Exception e) {
			e.printStackTrace();
		}
		OS.startServices(OS.rpcServiceClasses);
    }
    

	public void onClick(View view){
    	if(view == ping_b){
    		Log.e("onclick", "Ping!!");
    		String ip = ip_et.getText().toString().trim();
    		String port = port_et.getText().toString().trim();
			RPCCallerSocket socket = null;
			try {
				long start = System.currentTimeMillis();
				socket = new RPCCallerSocket(ip, ip, port);
				socket.invoke("echo", "echo", new JSONObject().put("msg", "") );
				long end = System.currentTimeMillis();
				ping_result_tv.setText("time used: " + (end - start) + " ms");
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}finally{
				socket.close();
			}
    	}
    }

	private void getLayouts() {
		ip_et = (EditText) findViewById(R.id.ip_et);
		port_et = (EditText) findViewById(R.id.port_et);
		ping_b = (Button) findViewById(R.id.ping_b);
		ping_result_tv = (TextView) findViewById(R.id.ping_result_tv);
	}
}