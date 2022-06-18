package kr.ac.sogang.mmlab.AndroidVideoPlayer.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;


import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kr.ac.sogang.R;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.model.PytorchClassifier;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.FeatureFileIO;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.Logging;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.Requests;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private PytorchClassifier model;

    private double mExtractFps;

    private String mSaveFrameDirPath;
    private Date mDate;
    private DirFileUtil mDirFileUtil;
    private Context applicationContext;
    private FeatureFileIO featureFileIO;
    private Requests requests;
    private int queryId;
    private int cnt = 0;
    private String searchResult = null;

    public boolean initializeVideo(Context context, String videoURL, int extractFps) {
        applicationContext = context;
        mVideoName = getVideoName(videoURL);
        mInferTime = 0;
        mDate = new Date();
        mDirFileUtil = new DirFileUtil();
        mSaveFrameDirPath = mDirFileUtil.CreateSaveDir(mDate, mVideoName);
        mVideoURL = videoURL;
        featureFileIO = new FeatureFileIO(applicationContext);
        requests = new Requests();

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
        //model = new BaseModel();
        String modelPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AndroidVideoPlayer/model/mobilenetv2_cpu_jh.pt";
        long s = System.currentTimeMillis();
        model = new PytorchClassifier(modelPath);
        long e = System.currentTimeMillis();
        float modelLoadingTime = (e - s) / 1000f;
        Logging.logD(TAG + " - Video initialization is succeeded : " + modelLoadingTime);

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
            options = " -vf fps=" + mExtractFps + " ";
        }
        Logging.logD(TAG + "(" + Config.TAG + ")"  + "- Execution: ffmpeg  -hide_banner -loglevel panic -i " + mVideoURL + " -vsync 2 -q:v 0 " + options + mSaveFrameDirPath + "/%%06d.jpg");

        int rc = FFmpeg.execute(" -hide_banner -loglevel panic -i " + mVideoURL + " -vsync 2 -q:v 0 " + options + mSaveFrameDirPath + "/%06d.jpg");

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

    private float[] inferenceFrame(Bitmap bitmapFrame) {
        long inferStartTime = 0, inferEndTime = 0, inferTime = 0;
        try {
            mFramecount += 1;
            // TODO: inference
            inferStartTime = System.currentTimeMillis();
            float[] feature = model.inferenceFloat(bitmapFrame);
//            cnt++;
//            float[] feature = model.inferenceTensorfile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/AndroidVideoPlayer/features/inputTensor_" + cnt + ".txt");
            inferEndTime = System.currentTimeMillis();

            inferTime = (inferEndTime - inferStartTime);
            mInferTime += inferTime;

            if (mFramecount % 50 == 0) {
                Logging.logD(TAG + "(Infer) - inference framecount: " + mFramecount +
                        "  /\tInference time: " + (double)(inferTime)/1000
                );
            }
            return feature;
        } catch(Exception e) {
            Logging.logD(TAG + "(Infer) - inference fail");
            e.printStackTrace();
            return null;
        }
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
        if(getFrameList()) {
            Logging.logD(TAG + "(FFmpeg) -  Frame list has been loaded successfully.(# of frame: " + mFrameList.size() + ")");
        }


        float[][] features = null;
        String featureFilename = null;
        int count = 1;
        int feature_count = 0;
        for (int i = 0; i < mFrameList.size(); i++) {
            bitmap = getBitmapFromAsolutePath(mFrameList.get(i).toString());
            float[] feature = inferenceFrame(bitmap);
            int length = 10;
            if (bitmap != null) {
//                if (features == null) {
//                    features = new float[length][];
//                    features[feature_count++] = feature.clone();
//                } else if ((i+1) % length == 0) {
//                    features[feature_count++] = feature.clone();
//                    for (int j = 0; j < features.length; j++) {
//                        if (features[j] != null)
//                            Logging.logE("FFmpeg - Feature " + (i+1) + "/" + j + " length: " + features[j].length);
//                        else {
//                            Logging.logE("FFmpeg - Feature " + i + "/" + j + " length: null");
//                        }
//                    }
//                    try {
//                        long startMergeTime = System.nanoTime();
//                        for (int j = 0; j < features[0].length; j++) {
//                            float tmp = 0f;
//                            for (int k = 0; k < features.length; k++) {
//                                if (k == 0) {
//                                    tmp = features[k][j];
//                                } else {
//                                    tmp = Math.max(tmp, features[k][j]);
//                                }
//                            }
//                        }
//                        long endMergeTime = System.nanoTime();
//                        float mergeTime = (endMergeTime - startMergeTime) / 1000000f;
//                        Logging.logE("FFmpegWrapper - " + i + " feature max time:\t" + mergeTime + "\tmilliseconds");
//                    } catch (Exception e) {
//                        Logging.logE("FFmpegWrapper - features length is too short(features.length < " + length + ")");
//                    }
//                    features = null;
//                    features = new float[length][];
//                    feature_count = 0;
//                } else {
//                    features[feature_count++] = feature.clone();
//                }
                if (features == null) {
                    features = new float[10][];
                    featureFilename = featureFileIO.FeatureWriter(count++);
                    featureFileIO.WriteFeature(feature);
                }
                else if ((i + 1) % 10 == 0) {
                    features = null;
                    featureFileIO.FeatureFileClose();
                    final JSONObject result;
                    if ((i+1) == 10) {
                        result = requests.searchFeature(
                                applicationContext.getResources().getString(R.string.search_url),
                                featureFilename,
                                20);
                        try {
                            queryId = result.getInt("id");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        result = requests.searchFeatureUpdate(
                                applicationContext.getResources().getString(R.string.search_url),
                                featureFilename, queryId
                                );
                    }

                    Handler mHandler = new Handler(Looper.getMainLooper());
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            String strResults = requests.getResults(result);
                            Toast.makeText(applicationContext, "Query id: " + queryId,Toast.LENGTH_SHORT).show();
                        }
                    }, 0);
                } else {
                    featureFileIO.WriteFeature(feature);
                    featureFileIO.WriteComma();
                }
            } else {
                Logging.logD(TAG + "(Infer) -  bitmap is null.(# of frame: " + (i + 1) + ")");
            }
            bitmap = null;
        }

        /* TODO: Dummy data for visualize */
        searchResult = new String("[{\"query_segment\": \"0.0 - 53.5\", \"reference\": \"0ab11b52561e9255423b01f29f7904a6dcadd87b.flv\", \"reference_segment\": \"0.0 - 53.5\", \"score\": 106.54796600341797, \"count\": 107}, {\"query_segment\": \"0.0 - 46.0\", \"reference\": \"cb593efe5b194fe67a8e5ffe33b35cca954cdcd4.flv\", \"reference_segment\": \"130.0 - 175.5\", \"score\": 72.25775146484375, \"count\": 83}, {\"query_segment\": \"12.0 - 24.5\", \"reference\": \"cb593efe5b194fe67a8e5ffe33b35cca954cdcd4.flv\", \"reference_segment\": \"35.0 - 50.0\", \"score\": 15.402772903442383, \"count\": 18}, {\"query_segment\": \"46.0 - 53.5\", \"reference\": \"cb593efe5b194fe67a8e5ffe33b35cca954cdcd4.flv\", \"reference_segment\": \"92.5 - 109.0\", \"score\": 10.738666534423828, \"count\": 13}, {\"query_segment\": \"20.0 - 29.5\", \"reference\": \"cb593efe5b194fe67a8e5ffe33b35cca954cdcd4.flv\", \"reference_segment\": \"95.0 - 109.5\", \"score\": 7.319665431976318, \"count\": 9}, {\"query_segment\": \"46.0 - 53.0\", \"reference\": \"cb593efe5b194fe67a8e5ffe33b35cca954cdcd4.flv\", \"reference_segment\": \"141.0 - 152.5\", \"score\": 5.751912593841553, \"count\": 7}, {\"query_segment\": \"46.0 - 53.0\", \"reference\": \"cb593efe5b194fe67a8e5ffe33b35cca954cdcd4.flv\", \"reference_segment\": \"35.0 - 48.5\", \"score\": 4.107478618621826, \"count\": 5}, {\"query_segment\": \"1.5 - 3.0\", \"reference\": \"cb593efe5b194fe67a8e5ffe33b35cca954cdcd4.flv\", \"reference_segment\": \"38.5 - 40.5\", \"score\": 2.5060946941375732, \"count\": 3}, {\"query_segment\": \"21.0 - 24.5\", \"reference\": \"cb593efe5b194fe67a8e5ffe33b35cca954cdcd4.flv\", \"reference_segment\": \"59.5 - 64.5\", \"score\": 2.490096092224121, \"count\": 3}, {\"query_segment\": \"46.0 - 51.0\", \"reference\": \"cb593efe5b194fe67a8e5ffe33b35cca954cdcd4.flv\", \"reference_segment\": \"25.0 - 26.5\", \"score\": 2.436229705810547, \"count\": 3}, {\"query_segment\": \"47.0 - 53.5\", \"reference\": \"cb593efe5b194fe67a8e5ffe33b35cca954cdcd4.flv\", \"reference_segment\": \"59.5 - 64.5\", \"score\": 2.418823719024658, \"count\": 3}, {\"query_segment\": \"2.0 - 29.0\", \"reference\": \"5a6a49b7660b858476a69819cdf15a13ecf13c36.mp4\", \"reference_segment\": \"0.0 - 36.5\", \"score\": 29.61270523071289, \"count\": 35}, {\"query_segment\": \"46.0 - 53.0\", \"reference\": \"5a6a49b7660b858476a69819cdf15a13ecf13c36.mp4\", \"reference_segment\": \"21.0 - 32.5\", \"score\": 9.096687316894531, \"count\": 11}, {\"query_segment\": \"46.0 - 53.5\", \"reference\": \"5a6a49b7660b858476a69819cdf15a13ecf13c36.mp4\", \"reference_segment\": \"9.5 - 20.0\", \"score\": 2.4543046951293945, \"count\": 3}, {\"query_segment\": \"9.0 - 25.0\", \"reference\": \"b91aa7020c5dd0a6518d11f745b275363f854bce.flv\", \"reference_segment\": \"12.5 - 21.0\", \"score\": 13.109444618225098, \"count\": 15}, {\"query_segment\": \"46.0 - 49.0\", \"reference\": \"b91aa7020c5dd0a6518d11f745b275363f854bce.flv\", \"reference_segment\": \"14.0 - 20.5\", \"score\": 4.970665454864502, \"count\": 6}, {\"query_segment\": \"4.0 - 8.5\", \"reference\": \"b91aa7020c5dd0a6518d11f745b275363f854bce.flv\", \"reference_segment\": \"13.5 - 18.0\", \"score\": 3.3749687671661377, \"count\": 4}, {\"query_segment\": \"12.5 - 23.0\", \"reference\": \"1e04e15f77363ebb7cdfb8811cd3d5d6d8b18305.flv\", \"reference_segment\": \"30.0 - 46.5\", \"score\": 12.877286911010742, \"count\": 15}, {\"query_segment\": \"19.5 - 23.5\", \"reference\": \"1e04e15f77363ebb7cdfb8811cd3d5d6d8b18305.flv\", \"reference_segment\": \"55.0 - 63.0\", \"score\": 4.9199628829956055, \"count\": 6}, {\"query_segment\": \"46.5 - 51.0\", \"reference\": \"1e04e15f77363ebb7cdfb8811cd3d5d6d8b18305.flv\", \"reference_segment\": \"57.0 - 61.5\", \"score\": 4.096626281738281, \"count\": 5}, {\"query_segment\": \"47.0 - 49.0\", \"reference\": \"1e04e15f77363ebb7cdfb8811cd3d5d6d8b18305.flv\", \"reference_segment\": \"33.0 - 39.0\", \"score\": 3.298031806945801, \"count\": 4}, {\"query_segment\": \"19.5 - 23.0\", \"reference\": \"1e04e15f77363ebb7cdfb8811cd3d5d6d8b18305.flv\", \"reference_segment\": \"20.0 - 23.0\", \"score\": 3.252869129180908, \"count\": 4}, {\"query_segment\": \"47.5 - 53.0\", \"reference\": \"1e04e15f77363ebb7cdfb8811cd3d5d6d8b18305.flv\", \"reference_segment\": \"18.5 - 21.5\", \"score\": 3.2414193153381348, \"count\": 4}, {\"query_segment\": \"26.0 - 46.0\", \"reference\": \"0dbcff0b6d671e1579ad468eed54d84f7e9b7289.mp4\", \"reference_segment\": \"58.5 - 78.0\", \"score\": 12.278562545776367, \"count\": 15}, {\"query_segment\": \"10.5 - 26.5\", \"reference\": \"0dbcff0b6d671e1579ad468eed54d84f7e9b7289.mp4\", \"reference_segment\": \"34.5 - 47.5\", \"score\": 11.093164443969727, \"count\": 13}, {\"query_segment\": \"10.0 - 16.5\", \"reference\": \"0dbcff0b6d671e1579ad468eed54d84f7e9b7289.mp4\", \"reference_segment\": \"4.5 - 11.5\", \"score\": 5.067935943603516, \"count\": 6}, {\"query_segment\": \"50.5 - 53.0\", \"reference\": \"0dbcff0b6d671e1579ad468eed54d84f7e9b7289.mp4\", \"reference_segment\": \"23.0 - 28.5\", \"score\": 2.4122533798217773, \"count\": 3}, {\"query_segment\": \"7.5 - 24.5\", \"reference\": \"0ed02c5bbb500542f88f0d7285e56e1186ac9bf5.flv\", \"reference_segment\": \"0.0 - 13.0\", \"score\": 24.291576385498047, \"count\": 26}, {\"query_segment\": \"1.0 - 3.0\", \"reference\": \"0ed02c5bbb500542f88f0d7285e56e1186ac9bf5.flv\", \"reference_segment\": \"2.0 - 8.5\", \"score\": 3.3167176246643066, \"count\": 4}, {\"query_segment\": \"47.0 - 53.0\", \"reference\": \"0ed02c5bbb500542f88f0d7285e56e1186ac9bf5.flv\", \"reference_segment\": \"4.5 - 11.5\", \"score\": 3.3005313873291016, \"count\": 4}, {\"query_segment\": \"2.0 - 48.0\", \"reference\": \"13f06a567cbee6f40952cc13817a7774caa72903.flv\", \"reference_segment\": \"3.5 - 52.5\", \"score\": 63.315040588378906, \"count\": 73}, {\"query_segment\": \"4.0 - 24.5\", \"reference\": \"49eccab61ab137fc0ab04c8029e3caec0fa2b35f.flv\", \"reference_segment\": \"0.0 - 19.0\", \"score\": 29.881744384765625, \"count\": 34}, {\"query_segment\": \"47.0 - 53.0\", \"reference\": \"49eccab61ab137fc0ab04c8029e3caec0fa2b35f.flv\", \"reference_segment\": \"7.0 - 16.0\", \"score\": 3.2725205421447754, \"count\": 4}, {\"query_segment\": \"0.5 - 18.5\", \"reference\": \"cc3b8791039b9e7f664260d76f8ca6cc48004823.flv\", \"reference_segment\": \"36.0 - 53.0\", \"score\": 23.120351791381836, \"count\": 27}, {\"query_segment\": \"0.0 - 8.5\", \"reference\": \"cc3b8791039b9e7f664260d76f8ca6cc48004823.flv\", \"reference_segment\": \"0.0 - 10.5\", \"score\": 13.044205665588379, \"count\": 15}, {\"query_segment\": \"21.0 - 26.0\", \"reference\": \"cc3b8791039b9e7f664260d76f8ca6cc48004823.flv\", \"reference_segment\": \"67.5 - 76.0\", \"score\": 3.255617141723633, \"count\": 4}, {\"query_segment\": \"49.5 - 53.0\", \"reference\": \"cc3b8791039b9e7f664260d76f8ca6cc48004823.flv\", \"reference_segment\": \"67.5 - 73.5\", \"score\": 2.4412174224853516, \"count\": 3}, {\"query_segment\": \"0.0 - 24.5\", \"reference\": \"8ad458ed4d45f32e8baa3bf5d84ad2a2a0324300.flv\", \"reference_segment\": \"0.0 - 32.0\", \"score\": 36.742340087890625, \"count\": 41}, {\"query_segment\": \"46.0 - 49.0\", \"reference\": \"8ad458ed4d45f32e8baa3bf5d84ad2a2a0324300.flv\", \"reference_segment\": \"27.5 - 32.0\", \"score\": 4.943924903869629, \"count\": 6}, {\"query_segment\": \"48.0 - 52.0\", \"reference\": \"8ad458ed4d45f32e8baa3bf5d84ad2a2a0324300.flv\", \"reference_segment\": \"21.0 - 22.5\", \"score\": 2.4115147590637207, \"count\": 3}, {\"query_segment\": \"4.0 - 19.0\", \"reference\": \"42fe16072c4e7fbf1d1e280df7c1ad727446a615.mp4\", \"reference_segment\": \"0.0 - 8.5\", \"score\": 14.77822208404541, \"count\": 17}, {\"query_segment\": \"47.0 - 49.0\", \"reference\": \"42fe16072c4e7fbf1d1e280df7c1ad727446a615.mp4\", \"reference_segment\": \"3.5 - 8.5\", \"score\": 3.290670394897461, \"count\": 4}, {\"query_segment\": \"13.0 - 20.5\", \"reference\": \"0c06d64856ceae3c5146fd0cae9ac6fc5107083e.flv\", \"reference_segment\": \"0.5 - 15.5\", \"score\": 6.774156093597412, \"count\": 8}, {\"query_segment\": \"10.5 - 13.0\", \"reference\": \"0c06d64856ceae3c5146fd0cae9ac6fc5107083e.flv\", \"reference_segment\": \"4.0 - 9.5\", \"score\": 2.548945426940918, \"count\": 3}, {\"query_segment\": \"47.0 - 50.5\", \"reference\": \"0c06d64856ceae3c5146fd0cae9ac6fc5107083e.flv\", \"reference_segment\": \"3.5 - 8.5\", \"score\": 2.4547624588012695, \"count\": 3}, {\"query_segment\": \"0.0 - 19.0\", \"reference\": \"afc75e8c02a5ecba44739b0623befa16afa1d896.flv\", \"reference_segment\": \"9.0 - 27.0\", \"score\": 20.665037155151367, \"count\": 24}, {\"query_segment\": \"6.0 - 19.0\", \"reference\": \"2d38fff9674d85ddccdd873df98475153c20aedd.flv\", \"reference_segment\": \"7.5 - 15.5\", \"score\": 9.553303718566895, \"count\": 11}, {\"query_segment\": \"2.0 - 4.0\", \"reference\": \"2d38fff9674d85ddccdd873df98475153c20aedd.flv\", \"reference_segment\": \"7.5 - 13.0\", \"score\": 2.4855360984802246, \"count\": 3}, {\"query_segment\": \"9.0 - 17.0\", \"reference\": \"e2d1ca4b2657cd82092765801adda59a7f71bc56.mp4\", \"reference_segment\": \"0.0 - 5.0\", \"score\": 6.935962200164795, \"count\": 8}, {\"query_segment\": \"5.5 - 9.0\", \"reference\": \"811a34d320785afe0f50b9ff7a67a8b7c22e9891.flv\", \"reference_segment\": \"16.0 - 17.5\", \"score\": 2.563648223876953, \"count\": 3}, {\"query_segment\": \"4.0 - 9.0\", \"reference\": \"f5d187eb9c7e5cfc6eaa206f479ede5c10603c68.flv\", \"reference_segment\": \"28.0 - 30.5\", \"score\": 3.411672353744507, \"count\": 4}]");

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
                + "\tAvagage preprocessing time\t" + (double)(model.getAvgPreprocessTime()) + "\n"
                + "\tAvagage CNN inference time\t" + (double)(model.getAvgModelTime()) + "\n"
        );
        Handler mHandler = new Handler(Looper.getMainLooper());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(applicationContext, "Search Complete.", Toast.LENGTH_SHORT).show();
            }
        }, 0);
    }
    public void stopProcess() {
        FFmpeg.cancel();
        isKeepGoing = false;
    }
    public String getSearchResult() {
        return searchResult;
    }
}
