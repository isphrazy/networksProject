package CSE461.PingDroid;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import edu.uw.cs.cse461.sp12.OS.DDNSRRecord;
import edu.uw.cs.cse461.sp12.OS.DDNSResolverService;
import edu.uw.cs.cse461.sp12.OS.IPFinder;
import edu.uw.cs.cse461.sp12.OS.OS;
import edu.uw.cs.cse461.sp12.OS.RPCCallerSocket;


import android.app.Activity;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * @author Pingyang He
 *
 */
public class PingDroidActivity extends Activity {
	
	private final int SIZE = 1;
	
	private Properties config;
	
	private EditText ip_et;
	private EditText port_et;
	private Button ping_b;
	private TextView ping_result_tvs[];
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getLayouts();
        config = new Properties();
        
        
    }
    
    /**
     * start OS
     */
    public void onStart(){
    	super.onStart();
    	Toast.makeText(this, "start os", Toast.LENGTH_SHORT).show();
    	String ip = getIp();
    	TextView ip_tv = (TextView) findViewById(R.id.ip_tv);
    	ip_tv.setText("my ip is: " + ip);
    	
    	IPFinder.getInstance().ip = ip;
    	try {
//    		config.load(getAssets().open("jz.cse461.config.ini"));
    		config.load(getAssets().open("foo.bar.config.ini"));
    		OS.boot(config);
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	OS.startServices(OS.rpcServiceClasses);
    	OS.startServices(OS.ddnsServiceClasses);
    	
    }
    
    /**
     * shut the OS down
     */
    public void onStop(){
    	super.onStop();
    	Toast.makeText(this, "shut os down", Toast.LENGTH_SHORT).show();
    	OS.shutdown();
    }

	public void onClick(View view){
    	if(view == ping_b){
    		Log.e("onclick", "Ping!!");
    		String ip = ip_et.getText().toString().trim();
    		if(ip.endsWith("."))
    		    ip = ip.substring(0, ip.length() - 1);
    		DDNSResolverService resolver = ((DDNSResolverService)OS.getService("ddnsresolver"));
    		String port = "";
    		if(ip.endsWith("cse461") || ip.endsWith("www")){
    		    DDNSRRecord record = resolver.resolve(ip += '.');
    		    if(!record.isDone()){
    		        Toast.makeText(this, "can not resolve the given address", Toast.LENGTH_SHORT).show();
    		        return;
    		    }
    		    Log.e("onClick", "resolved ip: " + record.getIp());
    		    ip = record.getIp();
    		    port = "" + record.getPort();
    		}else{
    		    port = port_et.getText().toString().trim();
    		    
    		}
    		
			RPCCallerSocket socket = null;
			try {
				for(int i = 0; i < SIZE; i ++){
					long start = System.nanoTime();
					socket = new RPCCallerSocket(ip, ip, port);
					socket.invoke("echo", "echo", new JSONObject().put("msg", "") );
					long end = System.nanoTime();
					ping_result_tvs[i].setText((i + 1) + ". time used: " + (end - start)/1000000 + " ms");
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
    	}
    }

	/*
	 * set up activity layout
	 */
	private void getLayouts() {
		ip_et = (EditText) findViewById(R.id.ip_et);
		port_et = (EditText) findViewById(R.id.port_et);
		ping_b = (Button) findViewById(R.id.ping_b);
		ping_result_tvs = new TextView[SIZE];
		ping_result_tvs[0] = (TextView) findViewById(R.id.ping_result_tv0);
	}
	
	/*
	 * get android phone's ip
	 */
	private String getIp() {
		WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int wifiIPInt = wifiInfo.getIpAddress();
		String wifiIP = Formatter.formatIpAddress(wifiIPInt);
		
		return wifiIP;
	}
}