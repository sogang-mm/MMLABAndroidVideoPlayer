package kr.ac.sogang.mmlab.AndroidVideoPlayer.util;

import android.content.Context;
import android.os.Environment;

import java.io.FileWriter;

import kr.ac.sogang.R;


public class FeatureFileIO {
    private Context context;
    private FileWriter fw;

    public FeatureFileIO(Context current){
        this.context = current;
    }

    public String FeatureWriter(int count) {
//        String featureFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + context.getResources().getString(R.string.app_name) + "/feature_" + count + ".txt";
        String featureFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + context.getResources().getString(R.string.app_name) + "/feature.txt";

        try {
            fw = new FileWriter(featureFilePath) ;

            fw.write("[");
        } catch (Exception e) {
            e.printStackTrace() ;
        }
        return featureFilePath;
    }
    public boolean WriteFeature(float[] feature) {
        try {
            fw.write("[");
            for (int i = 0; i < feature.length; i++) {
                fw.write(Float.toString(feature[i]));
                if (i != feature.length - 1)
                    fw.write(",");
            }
            fw.write("]");
            return true;
        } catch (Exception e) {
            e.printStackTrace() ;
            return false;
        }
    }
    public boolean WriteComma() {
        try {
            fw.write(",");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public boolean FeatureFileClose() {
        // close file.
        if (fw != null) {
            // catch Exception here or throw.
            try {
                fw.write("]");
                fw.close() ;
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
}
