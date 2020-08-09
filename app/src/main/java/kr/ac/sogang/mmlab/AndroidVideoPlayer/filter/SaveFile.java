package kr.ac.sogang.mmlab.AndroidVideoPlayer.filter;

import android.graphics.Bitmap;
import java.io.File;
import java.io.FileOutputStream;


public class SaveFile {
    public void saveImage(Bitmap finalBitmap, String imageDirPath, String imageName) {
        File imageDirFile = new File(imageDirPath);

        String fileName = imageName + ".jpg";
        File file = new File(imageDirFile, fileName);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
