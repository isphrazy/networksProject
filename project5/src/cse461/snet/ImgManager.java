package cse461.snet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.os.Environment;

public class ImgManager {
    
    private static final String PICTURE_FOLDER = "snet_pics/";
    
    public static String saveImage(Bitmap img, String picName){
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + "/" + PICTURE_FOLDER );
        dir.mkdirs();
        String myPictureFullPath = dir.getPath() + picName;
        //File file = new File(this.getExternalFilesDir(null), this.dirName+fileName); //this function give null pointer exception so im using other one
        File file = new File(myPictureFullPath);
        
        try {
            FileOutputStream fos = new FileOutputStream(file);
            img.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myPictureFullPath;
    }
    
    
}
