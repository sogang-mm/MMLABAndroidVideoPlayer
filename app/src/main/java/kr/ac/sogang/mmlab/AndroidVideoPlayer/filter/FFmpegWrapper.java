package kr.ac.sogang.mmlab.AndroidVideoPlayer.filter;

import android.graphics.Bitmap;
import android.net.Uri;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import java.text.SimpleDateFormat;
import java.util.Locale;

import kr.ac.sogang.R;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.ConfigKeys;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.ConfigUtil;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.Logging;


public class FFmpegWrapper extends Thread{
    private FFmpegFrameGrabber mFFmpegFrameGrabber;

    private String mVideoPath;

    private int mVideoLength;

    private int mExtractFps;
    private long mCurrentTime;

    private double mVideoFps;
    private double mVideoDuration;
    private AndroidFrameConverter mAndroidFrameConverter;

    // -- For save frames
    // private int mFrameWidth;
    // private int mFrameHeight;
    // private String mSaveFrameDirPath;
    // private SaveFile mSaveFile;
    // private int mImageNumber;
    // private Date mDate;
    // private DirFileUtil mDirFileUtil;

    private long mDecodeTime;
    private long mInferTime;
    private long mSeekTime;
    private int mFramecount;

    boolean isKeepGoing;
    private BaseModel model;

    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMdd");
    private SimpleDateFormat mDecodeTimeFormat = new SimpleDateFormat("HHmmss", Locale.KOREA);

    public boolean initializeVideo(String videoURL, int extractFps) {

        // -- For save frames
        // String videoName = getVideoName(videoURL);
        // mImageNumber = 1;
        // mSaveFile = new SaveFile();

        mVideoPath = videoURL;
        mDecodeTime = 0;
        mSeekTime = 0;

        if (extractFps == 0) {
            mExtractFps = 1;
        } else {
            mExtractFps = extractFps;
        }

        try {
            // -- For save frames
            // mDate = new Date();
            // mDirFileUtil = new DirFileUtil();
            // mSaveFrameDirPath = mDirFileUtil.CreateSaveDir(mDate, videoName);

            mFFmpegFrameGrabber = new FFmpegFrameGrabber(mVideoPath);
            mFFmpegFrameGrabber.setOption( "hwaccel", "mediacodec");
            mFFmpegFrameGrabber.setOption("skip_frame", "nokey");
            Logging.logD("FFmpeg - option: " + mFFmpegFrameGrabber.getOptions().toString());
            mFFmpegFrameGrabber.start();
            Logging.logD("FFmpegFrameGrabber is loaded (" + mVideoPath + ")");
        } catch (FrameGrabber.Exception e) {
            Logging.logD("Video initialization is failed (" + mVideoPath + ")");
            return false;
        }


        mAndroidFrameConverter  = new AndroidFrameConverter();
        Logging.logD("AndroidFrameConverter is loaded");

        mVideoFps = mFFmpegFrameGrabber.getFrameRate();
        mVideoLength = mFFmpegFrameGrabber.getLengthInVideoFrames();
        mVideoDuration = mFFmpegFrameGrabber.getLengthInTime() / 1000.0D;

        model = new BaseModel();
        /*
        mFrameWidth = mFFmpegFrameGrabber.getImageWidth();
        mFrameHeight = mFFmpegFrameGrabber.getImageHeight();
        */

        Logging.logD("Video initialization is succeeded");

        return true;
    }

    public boolean processFrame() {
        Bitmap bitmapFrame;
        long decodeStartTime = 0, decodeEndTime = 0,
                seekStartTime = 0, seekEndTime = 0,
                inferStartTime = 0, inferEndTime = 0,
                decodeTime = 0, seekTime = 0, inferTime = 0;
        try {
            mCurrentTime = mFFmpegFrameGrabber.getTimestamp() + mExtractFps * 1000000L;

            if (mVideoDuration <= mCurrentTime/1000.0D)
                return false;

            decodeStartTime = System.currentTimeMillis();
            bitmapFrame = mAndroidFrameConverter.convert(mFFmpegFrameGrabber.grabFrame(false, true, true, false));
            // mSaveFile.saveImage(bitmapFrame, mSaveFrameDirPath, String.format("%06d", mImageNumber++));
            decodeEndTime = System.currentTimeMillis();

            // TODO:
            inferStartTime = System.currentTimeMillis();
            boolean ret = model.inference(bitmapFrame);
            inferEndTime = System.currentTimeMillis();

            seekStartTime = System.currentTimeMillis();
            mFFmpegFrameGrabber.setTimestamp(mCurrentTime);
            seekEndTime = System.currentTimeMillis();

            decodeTime = (decodeEndTime - decodeStartTime);
            seekTime = (seekEndTime - seekStartTime);
            inferTime = (inferEndTime - inferStartTime);
            mSeekTime += seekTime;
            mInferTime += inferTime;
            mDecodeTime += decodeTime;

            mFramecount++;


            if (mFramecount % 50 == 0) {
                Logging.logD("FFmpeg - Decoded framecount: " + mFramecount +
                        "  /\tDecode time: " + decodeTime/1000 + "." + decodeTime%1000 +
                        "  /\tInference time: " + inferTime/1000 + "." + inferTime%1000 +
                        "  /\tSeek time: " + seekTime/1000 + "." + seekTime%1000
                );
            }

            bitmapFrame = null;

        } catch(FrameGrabber.Exception e) {
            e.printStackTrace();
            return false;
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

        long startTime = System.currentTimeMillis();
        while (isKeepGoing) {
            isKeepGoing = processFrame();
        }
        long endTime = System.currentTimeMillis();
        long time = endTime - startTime;

        Logging.logD("FFmpeg - Video info: \n"
                + "\tFPS: " + mVideoFps + "\n"
                + "\t# of Frames: " + mVideoLength + "\n"
                + "\tExtract fps" + mExtractFps + "\n"
        );

        double decodeTime = ((double)mDecodeTime/mFramecount)/1000;
        double inferTime = ((double)mInferTime/mFramecount)/1000;
        double seekTime = ((double)mSeekTime/mFramecount)/1000;

        Logging.logD("FFmpeg - process frames info: \n"
                + "\tCount of decoded frames:\t" + mFramecount + "\n"
                + "\tTotal decodeing time:\t" + + time/1000 + "." + time%1000 + "sec" + "\n"
                + "\tOne frame total time(decode + seek time):\t" + (decodeTime + inferTime + seekTime) + "\n"
                + "\tAverage Decode time:\t" + decodeTime + "\n"
                + "\tAverage Inference time:\t" + inferTime + "\n"
                + "\tAverage Seek time:\t" + seekTime + "\n"
        );

        try {
            mFFmpegFrameGrabber.stop();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }
    public void setRunningState(boolean state) {
        isKeepGoing = state;
    }


}
