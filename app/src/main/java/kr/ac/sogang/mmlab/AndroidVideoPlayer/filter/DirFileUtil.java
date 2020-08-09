package kr.ac.sogang.mmlab.AndroidVideoPlayer.filter;

import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DirFileUtil {
    public String CreateSaveDir(Date date, String videoName) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss", Locale.KOREA);

        String root = pathJoin(Environment.getExternalStorageDirectory().getAbsolutePath(), "AndroidVideoPlayer");
        String dateDirPath = pathJoin(root, dateFormat.format(date));
        MakeDateDir(dateDirPath);

        return MakeVideoDir(pathJoin(dateDirPath, videoName), timeFormat.format(date));
    }

    public String pathJoin(String dirPath, String subDirPath) {
        return dirPath + "/" + subDirPath;
    }

    public void MakeDateDir(String dateDirPath){
        File dateDir = new File(dateDirPath);
        if(!dateDir.isDirectory()){
            dateDir.mkdirs();
        }
    }

    public String MakeVideoDir(String videoDirPath, String timestamp){
        File videoDir = new File(videoDirPath);
        if(!videoDir.isDirectory()){
            videoDir.mkdirs();
            return videoDirPath;
        }
        else {
            String returnDirPath = videoDirPath + "_" + timestamp;
            videoDir.delete();
            videoDir = new File(returnDirPath);
            videoDir.mkdirs();
            return returnDirPath;
        }
    }
}