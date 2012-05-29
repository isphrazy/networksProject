package cse461.snet;

import java.util.Properties;

import cse461.snet.R;
import edu.uw.cs.cse461.sp12.OS.ContextManager;
import edu.uw.cs.cse461.sp12.OS.IPFinder;
import edu.uw.cs.cse461.sp12.OS.OS;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager.OnActivityResultListener;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class SNetDroidActivity extends Activity {
    
    private Properties config;
    private int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ContextManager.setContext(getApplicationContext());
        config = new Properties();
    }
    
    /**
     * will be called when take picture button is clicked
     * @param view
     */
    public void takePic(View view){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        Bitmap takedPhoto = (Bitmap) data.getExtras().get("data");
    }
    
    /**
     * will be called when choose picture button is clicked
     * @param view
     */
    public void choosePic(View view){
        
    }
    
    /**
     * will be called when update picture button is clicked
     * @param view
     */
    public void updatePic(View view){
        Intent i = new Intent();
        i.setClass(this, UpdatePicPage.class);
        startActivity(i);
    }
    
    /**
     * start OS
     */
    public void onStart(){
        super.onStart();
        Toast.makeText(this, "start os", Toast.LENGTH_SHORT).show();
        String ip = getIp();
        ((TextView) findViewById(R.id.ip_tv)).setText(ip);
        
        IPFinder.getInstance().ip = ip;
        try {
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
    
    
    /*
     * get current ip
     */
    private String getIp() {
        WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int wifiIPInt = wifiInfo.getIpAddress();
        String wifiIP = Formatter.formatIpAddress(wifiIPInt);
        
        return wifiIP;
    }
}