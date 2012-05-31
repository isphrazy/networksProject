package cse461.snet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import cse461.snet.R;
import edu.uw.cs.cse461.sp12.OS.ContextManager;
import edu.uw.cs.cse461.sp12.OS.IPFinder;
import edu.uw.cs.cse461.sp12.OS.OS;
import edu.uw.cs.cse461.sp12.util.DB461.DB461Exception;
import edu.uw.cs.cse461.sp12.util.DB461SQLite;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager.OnActivityResultListener;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SNetDroidActivity extends Activity {
    
    private final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1;
    private final int CHOOSE_PICTURE_ACTIVITY_REQUEST_CODE = 2;
    private final String My_PICTURE_NAME = "my_picture.png";
    private final String PICTURE_FOLDER = "snet_pics/";
    private final String MY_PICTURE_FOLDER = "my_pics/";
    private final String DATABASE_NAME = "";
    
    private Properties config;
    private ImageView myPicIv;
    private ImageView chosenPicIv;
    private DB461SQLite database;
    
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ContextManager.setContext(getApplicationContext());
        
        initVars();
    }
    
    private void initVars() {
        config = new Properties();
        myPicIv = (ImageView) findViewById(R.id.my_pic_iv);
        chosenPicIv = (ImageView) findViewById(R.id.chosen_pic_iv);
        try {
            database = MDb.getInstance();
        } catch (DB461Exception excp) {
            excp.printStackTrace();
        }
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
        if(requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE){
            
            Bitmap takedPhoto = (Bitmap) data.getExtras().get("data");
            
            myPicIv.setImageBitmap(takedPhoto);
//            String dirName = "";
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File (sdCard.getAbsolutePath() + "/" + PICTURE_FOLDER + MY_PICTURE_FOLDER);
            File[] myPictures = dir.listFiles();
            if(myPictures != null && myPictures.length >= 1)
                for(File myPicture : myPictures){
                    myPicture.delete();
                }
            
            dir.mkdirs();
            String myPictureFullPath = dir.getPath() + System.currentTimeMillis() + My_PICTURE_NAME;
            //File file = new File(this.getExternalFilesDir(null), this.dirName+fileName); //this function give null pointer exception so im using other one
            File file = new File(myPictureFullPath);
            
            try {
                FileOutputStream fos = new FileOutputStream(file);
                takedPhoto.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }else if(requestCode == CHOOSE_PICTURE_ACTIVITY_REQUEST_CODE){
            try{
                Bitmap chosedPhoto = (Bitmap) data.getExtras().get("data");
            }catch (NullPointerException excp){
                
            }
        }
    }
    
    /**
     * will be called when choose picture button is clicked
     * @param view
     */
    public void choosePic(View view){
        
        // Try to update gallery viewer
        Log.e("onActivityResult: ", "file://" + Environment.getExternalStorageDirectory());
        sendBroadcast(new Intent(
                Intent.ACTION_MEDIA_MOUNTED,
                Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, CHOOSE_PICTURE_ACTIVITY_REQUEST_CODE);
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
        
//        OS.startServices(OS.rpcServiceClasses);
//        OS.startServices(OS.ddnsServiceClasses);
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