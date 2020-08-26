package kr.ac.sogang.mmlab.AndroidVideoPlayer.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import kr.ac.sogang.BuildConfig;
import kr.ac.sogang.R;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.feature.update.AppUpdateManager;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.feature.update.DefaultUpdateCallback;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.feature.update.UpdateInfo;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.filter.FFmpegWrapper;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.ui.mediapicker.MediaPickerActivity;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.ui.playback.PlaybackActivity;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.ui.update.UpdateHelper;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.ConfigKeys;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.ConfigUtil;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.Logging;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import java.util.Locale;

public class LaunchActivity extends AppCompatActivity
{
    public static final String EXTRA_LAUNCH_NO_DELAY = "launchNoDelay";
    private final AppUpdateManager updateManager = new AppUpdateManager(BuildConfig.UPDATE_VENDOR, BuildConfig.UPDATE_REPO);
    private SharedPreferences appPreferences;

    private Handler splashHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lauch_activity);
        Logging.logD("Launch Activity onCreate was called.");

        appPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (shouldCheckUpdate())
        {
            checkUpdateAndContinueTo();
        }
        else
        {
            continueTo();
        }
    }

    private boolean shouldCheckUpdate()
    {
        if (!BuildConfig.ENABLE_SELF_UPDATE)
            return false;

        if (!ConfigUtil.getConfigBoolean(this, ConfigKeys.KEY_ENABLE_APP_UPDATES, R.bool.DEF_ENABLE_APP_UPDATES))
            return false;

        int updateFrequency = getResources().getInteger(R.integer.update_check_freqency);

        return new UpdateHelper(this).getTimeSinceLastUpdateCheck() >= updateFrequency;
    }

    private void checkUpdateAndContinueTo()
    {
        Toast.makeText(this, R.string.update_check_start_toast, Toast.LENGTH_SHORT).show();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 0);
            Toast.makeText(this, R.string.update_check_fail_toast, Toast.LENGTH_SHORT).show();
            continueTo();
            return;
        }

        final UpdateHelper updateHelper = new UpdateHelper(this);
        updateManager.checkForUpdate(new DefaultUpdateCallback()
        {
            @Override
            public void onUpdateCheckFinished(@Nullable UpdateInfo update, boolean failed)
            {
                if (failed)
                {
                    continueTo();
                    return;
                }

                updateHelper.updateTimeOfLastUpdateCheck();

                if (update == null)
                {
                    updateHelper.setUpdateAvailableFlag(false);
                    continueTo();
                    return;
                }

                updateHelper.setUpdateAvailableFlag(true);

                updateHelper.showUpdateDialog(update, new UpdateHelper.Callback()
                {
                    @Override
                    public void onUpdateFinished(boolean isUpdating)
                    {
                        if (!isUpdating) continueTo();
                    }
                }, false);
            }
        });
    }

    private void continueTo()
    {
        int minSplashDuration = getResources().getInteger(R.integer.min_splash_screen_duration);

        final Intent launchIntent = getIntent();
        if (launchIntent.getBooleanExtra(EXTRA_LAUNCH_NO_DELAY, false))
        {
            minSplashDuration = 0;
        }

        splashHandler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                String action = launchIntent.getAction();
                if (action != null
                        && (action.equals(Intent.ACTION_VIEW) || action.equals(Intent.ACTION_SEND))
                        && launchIntent.getData() != null)
                {
                    continueToPlayback();
                }
                else
                {
                    continueToMediaPicker();
                }
            }
        }, minSplashDuration);
    }

    private void continueToMediaPicker()
    {
        Intent pickerIntent = new Intent(this, MediaPickerActivity.class);
        startActivity(pickerIntent);
        finish();
    }

    private void continueToPlayback()
    {
        if (launchPlayback(getIntent()))
        {
            finish();
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Could not launch Playback Activity!", Toast.LENGTH_LONG).show();
        }
    }

    private boolean launchPlayback(Intent callingIntent)
    {
        dumpIntent(callingIntent, "Calling Intent");

        Uri playbackUrl = parsePlaybackUrl(callingIntent);
        if (playbackUrl == null) return false;
        else {
            FFmpegWrapper ffmpegWrapper = new FFmpegWrapper();
            ffmpegWrapper.initializeVideo(getVideoURL(playbackUrl), 1);
            ffmpegWrapper.start();
        }

        String title = parseTitle(playbackUrl, callingIntent);
        Logging.logE("videoName", title);
        if (title.isEmpty()) return false;

        Intent launchIntent = new Intent(this, PlaybackActivity.class);
        launchIntent.setData(playbackUrl);
        launchIntent.putExtra(Intent.EXTRA_TITLE, title);

        if (canResumePlayback(playbackUrl, title))
        {
            Logging.logD("Putting INTENT_EXTRA_JUMP_TO because playback can be resumed.");
            launchIntent.putExtra(PlaybackActivity.INTENT_EXTRA_JUMP_TO, getResumePosition());
        }
        else {
            Logging.logE("Can't player url(" + playbackUrl + ")");
        }

        dumpIntent(launchIntent, "Launch Intent");

        updateLastPlayed(playbackUrl, title);

        startActivity(launchIntent);
        return true;
    }

    private void updateLastPlayed(Uri url, String title)
    {
        appPreferences.edit().putString(ConfigKeys.KEY_LAST_PLAYED_URL, url.toString())
                .putString(ConfigKeys.KEY_LAST_PLAYED_TITLE, title).apply();
    }

    private boolean canResumePlayback(Uri url, String title)
    {
        if (appPreferences.getLong(ConfigKeys.KEY_LAST_PLAYED_POSITION, -1) <= 0) return false;

        return url.toString().equalsIgnoreCase(appPreferences.getString(ConfigKeys.KEY_LAST_PLAYED_URL, ""))
                || title.equalsIgnoreCase(appPreferences.getString(ConfigKeys.KEY_LAST_PLAYED_TITLE, ""));
    }

    private long getResumePosition()
    {
        return appPreferences.getLong(ConfigKeys.KEY_LAST_PLAYED_POSITION, 0); //TODO: remove a few seconds (10s)
    }

    private void dumpIntent(Intent intent, String desc)
    {
        Logging.logD("========================================");
        Logging.logD("Dumping Intent " + desc);
        Logging.logD("%s of type %s", intent.toString(), intent.getType());
        Uri data = intent.getData();
        Logging.logD("Data: %s (%s)", (data == null) ? "null" : data.toString(), intent.getDataString());

        Logging.logD("Extras: ");
        Bundle extras = intent.getExtras();
        if (extras != null)
        {
            for (String key : extras.keySet())
            {
                Logging.logD("   %s = %s", key, extras.get(key));
            }
        }
        else
        {
            Logging.logD("Intent has no extras.");
        }

        Logging.logD("========================================");
    }

    private Uri parsePlaybackUrl(Intent intent)
    {
        Logging.logD("call Intent: %s", intent.toString());
        Bundle extra = intent.getExtras();
        if (extra != null)
        {
            Logging.logD("call Intent Extras: ");
            for (String key : extra.keySet())
            {
                Object val = extra.get(key);
                Logging.logD("\"%s\" : \"%s\"", key, (val == null ? "NULL" : val.toString()));
            }
        }

        String action = intent.getAction();
        if (action == null || action.equalsIgnoreCase(Intent.ACTION_VIEW))
        {
            return intent.getData();
        }
        else if (action.equalsIgnoreCase(Intent.ACTION_SEND))
        {
            String type = intent.getType();
            if (type == null) return null;

            if (type.equalsIgnoreCase("text/plain"))
            {
                if (intent.hasExtra(Intent.EXTRA_TEXT))
                {
                    return Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT));
                }
            }
            else if (type.startsWith("video/")
                    || type.startsWith("audio/"))
            {
                if (intent.hasExtra(Intent.EXTRA_STREAM))
                {
                    return (Uri) intent.getExtras().get(Intent.EXTRA_STREAM);
                }
            }

            return null;
        }
        else
        {
            Logging.logW("Received Intent with unknown action: %s", intent.getAction());
            return null;
        }
    }

    private String parseTitle(@NonNull Uri uri, @NonNull Intent invokeIntent)
    {
        String title = null;

        Bundle extraData = invokeIntent.getExtras();
        if (extraData != null)
        {
            if (extraData.containsKey(Intent.EXTRA_TITLE))
            {
                title = extraData.getString(Intent.EXTRA_TITLE);
                Logging.logD("Parsing title from default EXTRA_TITLE...");
            }
            else
            {
                for (String key : extraData.keySet())
                {
                    if (key.toLowerCase(Locale.US).contains("title"))
                    {
                        Object val = extraData.get(key);

                        if (val instanceof String)
                        {
                            String valStr = (String) val;

                            if (!valStr.isEmpty())
                            {
                                title = valStr;
                                Logging.logD("Parsing title from non- default title extra (\"%s\" : \"%s\")", key, valStr);
                            }
                        }
                    }
                }
            }


            if (title != null) Logging.logD("parsed final title from extra: %s", title);
        }

        if (title == null || title.isEmpty())
        {
            title = uri.getLastPathSegment();
            if (title != null && !title.isEmpty() && title.indexOf('.') != -1)
            {
                title = title.substring(0, title.lastIndexOf('.'));
                Logging.logD("parse title from uri: %s", title);
            }
        }

        return title;
    }

    public String getVideoURL(Uri videoURL) {
        if (videoURL.toString().contains("http")) {
            return videoURL.toString();
        } else if(videoURL.toString().contains("storage")) {
            return videoURL.getPath();
        }
        else {
            return getPathFromUri(videoURL);
        }
    }
    public String getPathFromUri(Uri uri){
        Cursor cursor = getContentResolver().query(uri, null, null, null, null );
        cursor.moveToNext();
        String path = cursor.getString( cursor.getColumnIndex( "_data" ) );
        cursor.close();

        return path;

    }
}
