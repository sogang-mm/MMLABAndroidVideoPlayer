package kr.ac.sogang.mmlab.AndroidVideoPlayer.filter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.Logging;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;


public class FFmpegWrapper extends Thread{
    private String TAG = "FFmpegWrapper";
    private String mVideoURL;
    private String mVideoName;

    private List mFrameList;
    int mInferTime;
    int mFramecount;
    boolean isKeepGoing;
    private BaseModel model;

    private double mExtractFps;

    private String mSaveFrameDirPath;
    private Date mDate;
    private DirFileUtil mDirFileUtil;



    public boolean initializeVideo(String videoURL, int extractFps) {
        mVideoName = getVideoName(videoURL);
        mInferTime = 0;
        mDate = new Date();
        mDirFileUtil = new DirFileUtil();
        mSaveFrameDirPath = mDirFileUtil.CreateSaveDir(mDate, mVideoName);
        mVideoURL = videoURL;

        Logging.logD("FFmpegWrapper - Parameter info: \n"
                + "\tVideo URI: " + mVideoURL + "\n"
                + "\tVideo name: " + mVideoName + "\n"
        );
        if(extractFps == 0) mExtractFps = 0;
        else                mExtractFps = 1/(double)extractFps;

        // TODO: Check FFmpeg Load - mobile-ffmpeg
        if (loadFFmpeg()) {
            Logging.logD(TAG + "(" + Config.TAG + ")" + "- FFmpeg load completed successfully.");
        } else {
            Logging.logD(TAG + "(" + Config.TAG + ")" + "- FFmpeg load fail");
        }

        // TODO: CNN Model Load
        model = new BaseModel();
        Logging.logD(TAG + " - Video initialization is succeeded");

        return true;
    }

    private boolean loadFFmpeg() {
        int rc = FFmpeg.execute("-version");

        if (rc == RETURN_CODE_SUCCESS) {
            Logging.logD(TAG + "(" + Config.TAG + ")"  + "- Command execution completed successfully.");
        } else {
            Logging.logE(TAG + "(" + Config.TAG + ")"  + "- " + String.format("Command execution failed with rc=%d and the output below.", rc));
            Config.printLastCommandOutput(Log.INFO);
            return false;
        }
        FFmpeg.cancel();

        return true;
    }

    private boolean extractFramesFFmpeg() {
        String options;
        if (mExtractFps == 0) {
            options = " -vsync 0 -frame_pts true ";
        } else {
            options = " -r " + mExtractFps + " ";
        }
        Logging.logD(TAG + "(" + Config.TAG + ")"  + "- Execution: ffmpeg -skip_frame nokey -i " + mVideoURL + options + "/%%06.jpg");

        int rc = FFmpeg.execute(" -skip_frame nokey -i " + mVideoURL + options + mSaveFrameDirPath + "/%06d.jpg");

        if (rc == RETURN_CODE_SUCCESS) {
            Logging.logD(TAG + "(" + Config.TAG + ")"  + "- Command execution completed successfully.");
        } else if (rc == RETURN_CODE_CANCEL) {
            Logging.logD(TAG + "(" + Config.TAG + ")"  + "- Command execution cancelled by user.");
            return false;
        } else {
            Logging.logD(TAG + "(" + Config.TAG + ")"  + String.format("- Command execution failed with rc=%d and the output below.", rc));
            Config.printLastCommandOutput(Log.INFO);
            return false;
        }
        return true;
    }

    private boolean inferenceFrame(Bitmap bitmapFrame) {
        long inferStartTime = 0, inferEndTime = 0, inferTime = 0;
        try {
            mFramecount += 1;
            // TODO: inference
            inferStartTime = System.currentTimeMillis();
            boolean ret = model.inference(bitmapFrame);
            inferEndTime = System.currentTimeMillis();

            inferTime = (inferEndTime - inferStartTime);
            mInferTime += inferTime;

            if (mFramecount % 50 == 0) {
                Logging.logD(TAG + "(Infer) - inference framecount: " + mFramecount +
                        "  /\tInference time: " + (double)(inferTime)/1000
                );
            }
        } catch(Exception e) {
            Logging.logD(TAG + "(Infer) - inference fail");
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private boolean getFrameList() {
        try {
            mFrameList = new ArrayList();
            File saveFrameDir = new File(mSaveFrameDirPath);
            File frameList[] = saveFrameDir.listFiles();

            for (int i = 0; i < frameList.length; i++) {
                mFrameList.add(frameList[i].getAbsolutePath());
            }

            return true;
        } catch(Exception e) {
            return false;
        }
    }
    
    private String getVideoName(String videoPath) {
        if (videoPath.contains("/")) {
            String[] videoNameTmp = videoPath.split("/");
            return videoNameTmp[videoNameTmp.length - 1];
        }
        else {
            return videoPath;
        }
    }

    private Bitmap getBitmapFromAsolutePath(String asolutePath) {
        try {
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            return BitmapFactory.decodeFile(asolutePath, bmOptions);
        } catch(Exception e) {
            return null;
        }
    }

    public void run() {
        Bitmap bitmap;
        boolean ret;
        isKeepGoing = true;

        long startDecodeTime, endDecodeTime,
                startProcessTime, endProcessTime,
                processTime, decodeTime;
        double averageDecodeTime, averageInferTime;
        startDecodeTime = System.currentTimeMillis();
        ret = extractFramesFFmpeg();
        if(ret) {
            Logging.logD(TAG + "(FFmpeg) - Start extracting frames from video(" + mVideoName + ").");
        }
        endDecodeTime = System.currentTimeMillis();
        decodeTime = endDecodeTime - startDecodeTime;



        startProcessTime = System.currentTimeMillis();
        // TODO: Bitmap load from file path
        if(getFrameList()) {
            Logging.logD(TAG + "(FFmpeg) -  Frame list has been loaded successfully.(# of frame: " + mFrameList.size() + ")");
        }

        for(int i = 0; i < mFrameList.size(); i++) {
            bitmap = getBitmapFromAsolutePath(mFrameList.get(i).toString());
            if (bitmap != null) {
                boolean retInfer = inferenceFrame(bitmap);
            } else {
                Logging.logD(TAG + "(Infer) -  bitmap is null.(# of frame: " + (i + 1) + ")");
            }
            bitmap = null;
        }
        endProcessTime = System.currentTimeMillis();
        processTime = endProcessTime - startProcessTime;

        averageDecodeTime = ((double)decodeTime/mFramecount)/1000;
        averageInferTime = ((double)mInferTime/mFramecount)/1000;

        Logging.logD(TAG + " \n- Processing result: \n"
                + "\tCount of decoded frames:\t" + mFramecount + "\n"
                + "\tExtract fps:\t" + mExtractFps + "\n"
                + "\tTotal processing time(decode + inference):\t" +  + (double)(decodeTime + processTime)/1000 + "\n"
                + "\tOne frame total time(decode + inference):\t" + ((double)(decodeTime + processTime)/mFramecount)/1000 + "\n"

                + "\t-- Decoding info --\n"
                + "\tTotal decodeing time:\t" + (double)(decodeTime)/1000+ "\n"
                + "\tAverage Decode time:\t" + (double)(averageDecodeTime) + "\n"

                + "\t-- Load bitmap from absolute path --\n"
                + "\tLoading bitmap from absolute path time:\t" + (double)(processTime - mInferTime)/1000 + "\n"

                + "\t-- Inference Info --\n"
                + "\tTotal inference time:\t" + (double)(mInferTime)/1000 + "\n"
                + "\tAverage inference time:\t" + (double)(averageInferTime) + "\n"
        );
    }
    public void stopProcess() {
        FFmpeg.cancel();
        isKeepGoing = false;
    }
}
