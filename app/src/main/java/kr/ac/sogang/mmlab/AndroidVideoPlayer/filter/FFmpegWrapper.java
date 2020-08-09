package kr.ac.sogang.mmlab.AndroidVideoPlayer.filter;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.util.Log;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.Logging;


public class FFmpegWrapper extends Thread{
    private FFmpegFrameGrabber mFFmpegFrameGrabber;

    private String mVideoPath;
    private String mSaveFrameDirPath;

    private int mVideoLength;
    private int mFrameWidth;
    private int mFrameHeight;
    private int mExtractFps;

    private double mVideoFps;
    private double mVideoDuration;

    private AndroidFrameConverter mAndroidFrameConverter;
    private SaveFile mSaveFile;
    
    private Date mDate;
    private DirFileUtil mDirFileUtil;
    private int mImageNumber;

    boolean isKeepGoing;

    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMdd");
    private SimpleDateFormat mTimeFormat = new SimpleDateFormat("HHmmss", Locale.KOREA);

    public boolean initializeVideo(String videoURL, int extractFps) {
        String videoName = getVideoName(videoURL);
        Logging.logE("videoURL:" + videoURL);
        mImageNumber = 1;

        mSaveFile = new SaveFile();

        mVideoPath = videoURL;
        if (extractFps == 0) {
            mExtractFps = 1;
        } else {
            mExtractFps = extractFps;
        }

        try {
            mDate = new Date();
            mDirFileUtil = new DirFileUtil();

            mSaveFrameDirPath = mDirFileUtil.CreateSaveDir(mDate, videoName);

            mFFmpegFrameGrabber = new FFmpegFrameGrabber(mVideoPath);
            mFFmpegFrameGrabber.start();
            Logging.logD("FFmpegFrameGrabber is loaded (" + mVideoPath + ")");
        } catch (FrameGrabber.Exception e) {
            Logging.logD("Video initialization is failed (" + mVideoPath + ")");
            return false;
        }


        mAndroidFrameConverter  = new AndroidFrameConverter();
        Logging.logD("AndroidFrameConverter is loaded");

        mVideoFps = mFFmpegFrameGrabber.getFrameRate();
        /*
        mVideoLength = mFFmpegFrameGrabber.getLengthInVideoFrames();
        mVideoDuration = mFFmpegFrameGrabber.getLengthInTime() / 1000.0D;
        mFrameWidth = mFFmpegFrameGrabber.getImageWidth();
        mFrameHeight = mFFmpegFrameGrabber.getImageHeight();
        */

        Logging.logD("Video initialization is succeeded");

        return true;
    }

    @SuppressLint("DefaultLocale")
    public boolean saveFrameToBitmap() {
        Bitmap bitmapFrame;
        Frame frame;
        try {
            frame = mFFmpegFrameGrabber.grabFrame(false, true, true, false);
        } catch(FrameGrabber.Exception e) {
            e.printStackTrace();
            return false;
        }

        if ((mFFmpegFrameGrabber.getFrameNumber() % (Math.round(mVideoFps) * mExtractFps)) == 1) {
            bitmapFrame = mAndroidFrameConverter.convert(frame);
            mSaveFile.saveImage(bitmapFrame, mSaveFrameDirPath, String.format("%06d", mImageNumber++));
        }
        return true;
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

    public void run() {
        isKeepGoing = true;

        while (isKeepGoing) {
            isKeepGoing = saveFrameToBitmap();
        }
    }
    public void setRunningState(boolean state) {
        isKeepGoing = state;
    }
}
