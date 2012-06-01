package cse461.snet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import cse461.snet.R;
import edu.uw.cs.cse461.sp12.OS.ContextManager;
import edu.uw.cs.cse461.sp12.OS.IPFinder;
import edu.uw.cs.cse461.sp12.OS.OS;
import edu.uw.cs.cse461.sp12.util.DB461.DB461Exception;
import edu.uw.cs.cse461.sp12.util.DB461SQLite;
import edu.uw.cs.cse461.sp12.util.SNetDB461;
import edu.uw.cs.cse461.sp12.util.SNetDB461.CommunityRecord;
import edu.uw.cs.cse461.sp12.util.SNetDB461.Photo;
import edu.uw.cs.cse461.sp12.util.SNetDB461.PhotoRecord;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
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
//    private final String MY_PICTURE_FOLDER = "my_pics/";
    private final String DATABASE_NAME = "";
    
    private Properties config;
    private ImageView myPicIv;
    private ImageView chosenPicIv;
    private SNetDB461 db;
    
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ContextManager.setContext(getApplicationContext());
        
        startOS();
        initVars();
    }
    
    private void initVars() {
        
        myPicIv = (ImageView) findViewById(R.id.my_pic_iv);
        chosenPicIv = (ImageView) findViewById(R.id.chosen_pic_iv);
        try {
            db = MDb.getInstance();
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
        
        //return after taking a picture
        if(requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE){
            
            Bitmap takedPhoto = (Bitmap) data.getExtras().get("data");
            
            myPicIv.setImageBitmap(takedPhoto);
//            String dirName = "";
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File (sdCard.getAbsolutePath() + "/" + PICTURE_FOLDER);
            File[] myPictures = dir.listFiles();
            Log.e("onActivityResult", "list: " + Arrays.toString(myPictures));
            dir.mkdirs();
            
            String myPictureFullPath = dir.getPath() + "/" + System.currentTimeMillis() + My_PICTURE_NAME;
            Log.e("onActivityResult", "picturePath: " + myPictureFullPath);
            File myPhotoFile = new File(myPictureFullPath);
            
            try {
                FileOutputStream fos = new FileOutputStream(myPhotoFile);
                takedPhoto.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            //update the database
            try {
                
                CommunityRecord cRec = db.COMMUNITYTABLE.readOne(OS.config().getProperty("host.name"));
                Log.e("onActivityResult", "hostname: " + cRec.name);
                int myPHash = cRec.myPhotoHash;
                cRec.generation++;
                PhotoRecord pr;
                if(myPHash == 0){//first time take picture
                    pr = db.PHOTOTABLE.createRecord();
                }else{//already have one picture
                    pr = db.PHOTOTABLE.readOne(myPHash);
                    pr.file.delete();
                }
                Photo p = new Photo(myPhotoFile);
                cRec.myPhotoHash = p.hash();
                pr.file = p.file();
                pr.hash = p.hash();
                pr.refCount = 0;
                
                db.COMMUNITYTABLE.write(cRec);
                db.PHOTOTABLE.write(pr);
            } catch (DB461Exception excp) {
                excp.printStackTrace();
            } catch (FileNotFoundException excp) {
                excp.printStackTrace();
            } catch (IOException excp) {
                excp.printStackTrace();
            }
            //return after choosing a picture
        }else if(requestCode == CHOOSE_PICTURE_ACTIVITY_REQUEST_CODE){
//            if(requestCode == RESULT_OK){
            try{
                Uri selectedImg = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                
                Cursor cursor = getContentResolver().query(selectedImg, filePathColumn, null, null, null);
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String filePath = cursor.getString(columnIndex);
                
//                    Bitmap chosedPhoto = (Bitmap) data.getExtras().get("data");
                CommunityRecord cRec = db.COMMUNITYTABLE.readOne(OS.config().getProperty("host.name"));
                
            }catch (NullPointerException excp){
                Log.d("onActivityResult", "no picture is choosen");
            }catch (DB461Exception excp) {
                excp.printStackTrace();
            }
//            }
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
    
//    /**
//     * start OS
//     */
//    public void onStart(){
//        super.onStart();
//
//    }
    
    /*
     * start OS
     */
    private void startOS(){
        Toast.makeText(this, "start os", Toast.LENGTH_SHORT).show();
        String ip = getIp();
        ((TextView) findViewById(R.id.ip_tv)).setText(ip);
        IPFinder.getInstance().ip = ip;
        
        config = new Properties();
        try {
            config.load(getAssets().open("foo.bar.config.ini"));
            OS.boot(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
        OS.startServices(OS.rpcServiceClasses);
        OS.startServices(OS.ddnsServiceClasses);
    }
    
    
//    /**
//     * shut the OS down
//     */
//    public void onStop(){
//        super.onStop();
//        Toast.makeText(this, "shut os down", Toast.LENGTH_SHORT).show();
//        OS.shutdown();
//    }
    
    public void onDestroy(){
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