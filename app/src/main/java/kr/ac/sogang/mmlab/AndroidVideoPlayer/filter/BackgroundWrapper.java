package kr.ac.sogang.mmlab.AndroidVideoPlayer.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kr.ac.sogang.R;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.model.PytorchClassifier;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.ConfigKeys;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.ConfigUtil;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.FeatureFileIO;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.Logging;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.Requests;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;


public class BackgroundWrapper extends Thread{
    private String TAG = "BackgroundWrapper";
    private String mVideoURL;
    private String mVideoName;

    private Context applicationContext;
    private String searchResult = null;
    private Requests requests;

    public boolean initialize(Context context, String videoURL) {
        applicationContext = context;
        mVideoURL = videoURL;

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
        Handler mHandler = new Handler(Looper.getMainLooper());

        try {
            requests = new Requests();
            final JSONObject result;
            result = requests.searchVideo(
                    applicationContext.getResources().getString(R.string.search_url),
                    mVideoURL,
                    ConfigUtil.getConfigInt(applicationContext, ConfigKeys.KEY_REST_API_TOPK, R.integer.DEF_REST_API_TOPK),
                    ConfigUtil.getConfigInt(applicationContext, ConfigKeys.KEY_REST_API_WINDOW, R.integer.DEF_REST_API_WINDOW),
                    Double.parseDouble(ConfigUtil.getConfigString(applicationContext, ConfigKeys.KEY_REST_API_SCORE_THRESHOLD, R.string.DEF_REST_API_SCORE_THRESHOLD)),
                    ConfigUtil.getConfigInt(applicationContext, ConfigKeys.KEY_REST_API_MATCH_THRESHOLD, R.integer.DEF_REST_API_MATCH_THRESHOLD));

            searchResult = result.toString();

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(applicationContext, "Search Complete.", Toast.LENGTH_SHORT).show();
                }
            }, 0);
        } catch(Exception e) {
            searchResult = "error";
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(applicationContext, "Connection error occurred. Please contact server admin.", Toast.LENGTH_SHORT).show();
                }
            }, 0);
        }
    }
    public String getSearchResult() {
        return searchResult;
    }
}
