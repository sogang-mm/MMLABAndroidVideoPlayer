package kr.ac.sogang.mmlab.AndroidVideoPlayer.ui.playback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import kr.ac.sogang.R;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.AVPApp;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.feature.controlview.GesturePlayerControlView;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.feature.controlview.TapToHidePlayerControlView;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.feature.gl.GLAnime4K;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.feature.playback.VideoPlaybackService;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.feature.playback.VideoPlaybackServiceListener;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.feature.playerview.PlayerScaleType;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.feature.playerview.AVPEPlayerView;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.feature.playerview.AVPPlayerView;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.filter.FFmpegWrapper;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.ui.AppSettingsActivity;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.ui.ResultViewActivity;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.ui.playback.views.ControlQuickSettingsButton;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.ConfigKeys;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.ConfigUtil;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.Logging;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder;
import com.google.android.exoplayer2.util.Util;
import com.pnikosis.materialishprogress.ProgressWheel;

import org.json.JSONArray;

import java.util.ArrayList;

public class PlaybackActivity extends AppCompatActivity implements AVPApp.ICrashListener, GesturePlayerControlView.Listener
{
    //region ~~ Constants ~~

    /**
     * Id of permission request for external storage
     */
    private static final int PERMISSION_REQUEST_READ_VIDEO_SOURCE = 0;

    /**
     * Intent Extra key that tells the player to immediately jump to the given position in the video
     */
    public static final String INTENT_EXTRA_JUMP_TO = "jumpTo";

    /**
     * Interval in which the battery level is checked
     */
    private static final int BATTERY_WARN_CHECK_INTERVAL_MS = 10000;

    /**
     * With how much delay the buffering indicator is enabled after the player is buffering
     */
    private static final int BUFFERING_INDICATOR_LAZY_TIME_MS = 750;
    //endregion

    //region ~~ Variables ~~
    //region static views
    /**
     * The drawer layout that contains the quick settings and shader effect drawers
     */
    private DrawerLayout quickAccessDrawer;

    /**
     * the placeholder layout that the player view will be added to as a child
     */
    private FrameLayout playerViewPlaceholder;

    /**
     * The normal buffering indicator (not pip)
     */
    private ProgressWheel bufferingIndicatorNormal;

    /**
     * the buffering indicator for pip mode
     */
    private ProgressWheel bufferingIndicatorPip;

    /**
     * The play button in the center of the screen
     */
    private ImageButton playButton;

    /**
     * The Text View used to display the stream title
     */
    private TextView titleTextView;

    /**
     * The Anime4K Quick Settings button
     */
    private ControlQuickSettingsButton anime4kQSButton;

    /**
     * The "go pip" quick settings button
     */
    private ControlQuickSettingsButton goPipQSButton;

    //endregion

    /**
     * The connection to the video playback service
     */
    private VideoServiceConnection playbackServiceConnection;

    /**
     * video playback service that is home to the player
     */
    private VideoPlaybackService playbackService;

    /**
     * The View the Player Renders to when using GL video effects
     */
    @Nullable
    private AVPEPlayerView glPlayerView;

    /**
     * The View the Player Renders to when not using GL video effects
     */
    @SuppressWarnings("FieldCanBeLocal")
    @Nullable
    private AVPPlayerView playerView;

    /**
     * The View that contains and controls the player controls and also handles gestures for us
     */
    private GesturePlayerControlView playerControlView;

    /**
     * The uri of the media requested to be played
     */
    private Uri playbackUri;

    /**
     * Battery manager system service. used to send a warning to the screen when battery charge is dropping below a threshold value
     */
    private BatteryManager batteryManager;

    /**
     * Manages the screen rotation using the buttons
     */
    private ScreenRotationManager screenRotationManager;

    /**
     * The audio manager instance used to adjust media volume by swiping
     */
    private AudioManager audioManager;

    /**
     * Receives events from PIP control items when in pip mode
     */
    private BroadcastReceiver pipBroadcastReceiver;

    /**
     * The Anime4K filter instance that may be active currently.
     * Set to null if filter is inactive
     */
    private GLAnime4K anime4KFilter;

    /**
     * the position playback should start at
     */
    private long playbackStartPosition = 0;

    /**
     * Should the playback start as soon as media is ready?
     */
    private boolean playbackPlayWhenReady = false;

    /**
     * The original volume index before the playback activity was opened
     */
    private int originalVolumeIndex;

    /**
     * is the app currently in pip mode?
     */
    private boolean isPictureInPicture = false;

    /**
     * has the current media ended?
     * (= is the replay button visible?)
     */
    private boolean isPlaybackEnded = false;

    /**
     * flag to indicate that a battery low warning was shown to the user
     */
    private boolean wasBatteryWarningShown = false;

    /**
     * Was the Back button pressed once already?
     * Used for "Press Back again to exit" function
     */
    private boolean wasBackPressedOnce = false;

    /**
     * Should we not save the playback position when exiting the app?
     * Set by VideoServiceCallbackListener.onPlaybackEnded()
     * Reset in onStop()
     */
    private boolean dontSavePlaybackPositionOnExit = false;

    /**
     * if true, the buffering indicator may not be used to indicate buffering
     */
    private boolean isBufferingIndicatorBlocked = false;

    /**
     * is the player currently buffering?
     */
    private boolean isBufferingState = false;
    //endregion

    //region ~~ Message Handler (delayHandler) ~~

    /**
     * Message code constants used by the delayHandler
     */
    private static final class Messages
    {
        /**
         * Message id to reset wasBackPressedOnce flag
         */
        private static final int RESET_BACK_PRESSED = 2;

        /**
         * Message to check the battery level. (only call once - this message calls itself)
         */
        private static final int BATTERY_WARN_CHECK = 3;

        /**
         * Message to make the buffering indicator visible (with a *optional* delay)
         */
        private static final int SHOW_BUFFERING_INDICATOR = 4;

        /**
         * Message to unblock the buffering indicator
         */
        private static final int UNBLOCK_BUFFERING_INDICATOR = 5;

        /**
         * hide the system ui navbar
         */
        private static final int HIDE_SYSTEM_UI_NAVBAR = 6;
    }

    FFmpegWrapper ffmpegWrapper;

    /**
     * Shared handler that can be used to invoke methods and/or functions with a delay,
     */
    private final Handler delayHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case Messages.RESET_BACK_PRESSED:
                {
                    wasBackPressedOnce = false;
                    break;
                }
                case Messages.BATTERY_WARN_CHECK:
                {
                    //abort if there is no battery manager
                    if (batteryManager == null) return;

                    //don't send a warning when charging
                    if (!batteryManager.isCharging())
                    {
                        //get battery level and warn threshold in percent
                        int batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                        int batteryThresh = ConfigUtil.getConfigInt(getApplicationContext(), ConfigKeys.KEY_BATTERY_WARN_THRESHOLD, R.integer.DEF_BATTERY_WARN_THRESHOLD);

                        //check level against threshold
                        Logging.logD("[BatWarn] Battery is at %d %% - warn threshold is %d %%", batteryPercent, batteryThresh);
                        if (batteryPercent <= batteryThresh && !wasBatteryWarningShown)
                        {
                            //send a warning
                            playerControlView.showInfoText(getText(R.string.info_battery_low).toString());
                            wasBatteryWarningShown = true;
                        }
                    }
                    else
                    {
                        //reset warning shown when started charging
                        wasBatteryWarningShown = false;
                    }

                    //call self later
                    delayHandler.sendEmptyMessageDelayed(Messages.BATTERY_WARN_CHECK, BATTERY_WARN_CHECK_INTERVAL_MS);
                    break;
                }
                case Messages.SHOW_BUFFERING_INDICATOR:
                {
                    setBufferingIndicatorVisible(true, isPictureInPicture);
                }
                case Messages.UNBLOCK_BUFFERING_INDICATOR:
                {
                    //set visibility
                    bufferingIndicatorNormal.setWillNotDraw(!isBufferingState || isPictureInPicture);

                    //reset progress + start spinning again
                    bufferingIndicatorNormal.setInstantProgress(0f);
                    bufferingIndicatorNormal.setLinearProgress(false);
                    bufferingIndicatorNormal.spin();

                    //unblock
                    isBufferingIndicatorBlocked = false;
                    break;
                }
                case Messages.HIDE_SYSTEM_UI_NAVBAR:
                {
                    //hide system ui
                    View decor = getWindow().getDecorView();
                    decor.setSystemUiVisibility(decor.getVisibility() | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                }
            }
        }
    };

    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Logging.logD("onCreate of PlaybackActivity called.");
        super.onCreate(savedInstanceState);

        //make app fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //set layout
        setContentView(R.layout.playback_activity);

        //get views
        playerViewPlaceholder = findViewById(R.id.pb_playerViewPlaceholder);
        playerControlView = findViewById(R.id.pb_playerControlView);
        titleTextView = findViewById(R.id.pb_streamTitle);
        quickAccessDrawer = findViewById(R.id.pb_quick_settings_drawer);
        anime4kQSButton = findViewById(R.id.qs_btn_a4k_tgl);
        goPipQSButton = findViewById(R.id.qs_btn_pip);
        bufferingIndicatorNormal = findViewById(R.id.pb_playerBufferingWheel_normal);
        bufferingIndicatorPip = findViewById(R.id.pb_playerBufferingWheel_pipmode);
        playButton = findViewById(R.id.exo_play);

        //add listener for quick access drawer
        quickAccessDrawer.addDrawerListener(new QuickAccessDrawerListener());

        //add listener for player controls visibility
        playerControlView.getPlayerControls().setVisibilityChangeCallback(new PlayerControlsVisibilityListener());

        //set fast-forward and rewind increments
        int seekIncrement = ConfigUtil.getConfigInt(this, ConfigKeys.KEY_SEEK_BUTTON_INCREMENT, R.integer.DEF_SEEK_BUTTON_INCREMENT);
        playerControlView.getPlayerControls().setFastForwardIncrementMs(seekIncrement);
        playerControlView.getPlayerControls().setRewindIncrementMs(seekIncrement);

        //set volume / brightness change listener
        playerControlView.setListener(this);

        //init screen rotation manager
        screenRotationManager = new ScreenRotationManager();
        screenRotationManager.findComponents();

        //get battery manager service
        if (ConfigUtil.getConfigBoolean(this, ConfigKeys.KEY_BATTERY_WARN_ENABLE, R.bool.DEF_BATTERY_WARN_ENABLE))
        {
            batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);

            //queue first check
            delayHandler.sendEmptyMessageDelayed(Messages.BATTERY_WARN_CHECK, BATTERY_WARN_CHECK_INTERVAL_MS);
        }

        //get audio manager for persistent volume
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        //set this activity as a crash handler so we can save the playback position on crashes
        Application app = getApplication();
        if (app instanceof AVPApp)
        {
            Logging.logD("Set self as crash listener...");
            AVPApp AndroidVideoPlayerApp = (AVPApp) app;
            AndroidVideoPlayerApp.setCrashListener(this);
        }
        else
        {
            Logging.logW("getApplication() is not instance of AndroidVideoPlayerApp!");
        }

        //Get the intent this activity was created with
        Intent callIntent = getIntent();

        //get uri (in intents data field)
        playbackUri = callIntent.getData();
        if (playbackUri == null)
        {
            //invalid url
            Toast.makeText(this, "Invalid Playback URL! Exiting...", Toast.LENGTH_SHORT).show();
            finish();
            return;
        } else {
            ffmpegWrapper = new FFmpegWrapper();
            ffmpegWrapper.initializeVideo(getApplicationContext(), getVideoURL(playbackUri), ConfigUtil.getConfigInt(getApplicationContext(), ConfigKeys.KEY_FFMPEG_DECODE_FPS, R.integer.DEF_FFMPEG_DECODE_FPS));
            ffmpegWrapper.start();
        }

        //get title (in intents EXTRA_TITLE field)
        String title = callIntent.getStringExtra(Intent.EXTRA_TITLE);
        if (title == null || title.isEmpty())
        {
            title = "N/A";
        }
        setTitle(title);

        //get position to start playback at
        playbackStartPosition = callIntent.getLongExtra(INTENT_EXTRA_JUMP_TO, 0);

        //get auto play when launching
        playbackPlayWhenReady = ConfigUtil.getConfigBoolean(this, ConfigKeys.KEY_AUTO_PLAY, R.bool.DEF_AUTO_PLAY);

        //update pip button visibility (hide on devices without pip support)
        updatePipButtonVisibility();

        //update window flags
        updateWindowFlags();

        //update effects drawer
        updateGlEffectsDrawer();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        updateWindowFlags();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            updateWindowFlags();
    }

    /**
     * Update System UI visibility for fullscreen mode
     */
    private void updateWindowFlags()
    {
        if (isLandscapeOrientation())
        {
            //enables full screen (immersive) mode
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE
                    // Set the content to appear under the system bars so that the content doesn't resize when the system bars hide and show.
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    // Hide the nav bar and status bar
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
        else
        {
            //reset fullscreen when exiting landscape
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }

    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults)
    {
        Logging.logD("Request Permission Result received for request id %d", requestCode);

        //check if permission request was granted
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        //check which permission request this callback handles
        switch (requestCode)
        {
            case PERMISSION_REQUEST_READ_VIDEO_SOURCE:
            {
                if (granted)
                {
                    //have permissions now, start playing by reload
                    if (playbackService != null)
                        playbackService.reloadMedia();
                }
                else
                {
                    //permissions denied, show toast + close app
                    Toast.makeText(this, getString(R.string.toast_no_permissions_granted), Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    //region ~~ Application Lifecycle ~~

    @Override
    protected void onStart()
    {
        super.onStart();

        //create and bind video playback service
        playbackServiceConnection = new VideoServiceConnection();
        bindService(new Intent(this, VideoPlaybackService.class), playbackServiceConnection, Context.BIND_AUTO_CREATE);

        //get pref for play when ready
        //boolean playWhenReady = getPrefBool(ConfigKeys.KEY_AUTO_PLAY, R.bool.DEF_AUTO_PLAY);

        //load the media
        //playbackService.loadMedia(playbackUri, playWhenReady, playbackStartPosition);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        //restore volume and brightness
        restorePersistentValues(true);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        //save volume and brightness
        savePersistentValues(true);
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        //save playback position to prefs to be able to resume later
        if (!dontSavePlaybackPositionOnExit)
        {
            savePlaybackPosition();
        }
        dontSavePlaybackPositionOnExit = false;

        //disconnect from the service
        disconnectPlaybackService();

        //cancel everything on the handler
        delayHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        //stop the playback service when the app is stopped
        stopService(new Intent(this, VideoPlaybackService.class));
    }

    /**
     * disconnects the playback service safely
     * !! does not stop the service (no stopService() !!
     */
    private void disconnectPlaybackService()
    {
        Logging.logD("Disconnecting from Playback service...");

        //check if service is bound first
        if (playbackServiceConnection == null || !playbackServiceConnection.isConnected)
        {
            //not connected!
            Logging.logW("Playback service is already disconnected?! skipping disconnect.");
            return;
        }

        //prepare playback service for disconnect
        if (playbackService != null)
        {
            //save playback position and play when ready flag before unbinding service
            //so that we can resume where we left off when brought back from background
            playbackStartPosition = playbackService.getPlaybackPosition();
            playbackPlayWhenReady = playbackService.getPlayWhenReady();

            //remove self as listener
            playbackService.setListener(null);
        }

        //unbind the service
        unbindService(playbackServiceConnection);
    }

    @Override
    protected void onUserLeaveHint()
    {
        super.onUserLeaveHint();

        //enter pip mode if enabled
        if (ConfigUtil.getConfigBoolean(this, ConfigKeys.KEY_ENTER_PIP_ON_LEAVE, R.bool.DEF_ENTER_PIP_ON_LEAVE) && playbackService.getIsPlaying())
            tryGoPip();
    }

    @Override
    public void onBackPressed()
    {
        if (wasBackPressedOnce || !ConfigUtil.getConfigBoolean(this, ConfigKeys.KEY_BACK_DOUBLE_PRESS_EN, R.bool.DEF_BACK_DOUBLE_PRESS_EN))
        {
            //back pressed once already, do normal thing...
            super.onBackPressed();
            return;
        }

        //pressed back the first time, set flag
        wasBackPressedOnce = true;

        //send reset message delayed
        delayHandler.sendEmptyMessageDelayed(Messages.RESET_BACK_PRESSED, ConfigUtil.getConfigInt(this, ConfigKeys.KEY_BACK_DOUBLE_PRESS_TIMEOUT, R.integer.DEF_BACK_DOUBLE_PRESS_TIMEOUT));

        //show user a Toast
        Toast.makeText(this, getString(R.string.toast_press_back_again_to_exit), Toast.LENGTH_SHORT).show();
    }

    /**
     * Called on a crash shortly before the app is closed
     * This is part of the apps lifecycle ;)
     *
     * @param ex the exception that caused the crash
     */
    public void onCrash(Throwable ex)
    {
        //save playback position before app closes
        Logging.logD("Saving Playback position on app crash...");
        savePlaybackPosition();
    }

    //endregion

    //region ~~ Button Handling ~~

    /**
     * Common click handler for buttons in Playback activity
     *
     * @param view the view that invoked this handler
     */
    public void playback_OnClick(View view)
    {
        switch (view.getId())
        {
            //region ~~ Screen Rotation Buttons (cycle modes >auto>portrait>landscape>auto>...)~~
            case R.id.pb_screen_rotation_auto:
            {
                //automatic/default screen rotation button
                screenRotationManager.setScreenMode(ScreenRotationManager.SCREEN_LOCK_PORTRAIT);
                break;
            }
            case R.id.pb_screen_rotation_portrait:
            {
                //lock screen to portrait button
                screenRotationManager.setScreenMode(ScreenRotationManager.SCREEN_LOCK_LANDSCAPE);
                break;
            }
            case R.id.pb_screen_rotation_landscape:
            {
                //lock screen to landscape button
                screenRotationManager.setScreenMode(ScreenRotationManager.SCREEN_AUTO);
                break;
            }
            //endregion

            //region ~~ Player Quick Settings ~~
            case R.id.qs_btn_quality:
            {
                //show quality chooser dialog
                //first check that the playback service is ok and the player is valid
                if (!playbackServiceConnection.isConnected || playbackService == null || !playbackService.getIsPlayerValid())
                {
                    //playback service or player is invalid!
                    return;
                }

                //get track selector from service
                DefaultTrackSelector trackSelector = playbackService.getTrackSelector();
                if (trackSelector == null) return;

                //build the track selection dialog
                TrackSelectionDialogBuilder selectionDialogBuilder = new TrackSelectionDialogBuilder(this, "", trackSelector, 0)
                        .setAllowAdaptiveSelections(false)
                        .setAllowMultipleOverrides(false)
                        .setShowDisableOption(false);

                //build dialog and show
                selectionDialogBuilder.build().show();

                //hide player controls
                playerControlView.getPlayerControls().hide();

                //hide drawers
                quickAccessDrawer.closeDrawers();
                break;
            }
            case R.id.qs_btn_jump_to:
            {
                //jump to position in video, show dialog
                JumpToFragment jumpTo = new JumpToFragment();
                jumpTo.show(getSupportFragmentManager(), playbackService.getPlayerInstance());

                //hide player controls
                playerControlView.getPlayerControls().hide();

                //hide drawers
                quickAccessDrawer.closeDrawers();
                break;
            }
            case R.id.qs_btn_pip:
            {
                //open pip player
                tryGoPip();
                break;
            }
            case R.id.qs_btn_repeat_tgl:
            {
                //current video repeat toggle
                ControlQuickSettingsButton btnRepeatMode = findViewById(R.id.qs_btn_repeat_tgl);
                if (btnRepeatMode == null) break;

                //toggle looping state
                playbackService.setLooping(!playbackService.getLooping());

                //update ui
                btnRepeatMode.setIconTint(playbackService.getLooping() ? getColor(R.color.quick_settings_item_tint_active) : getColor(R.color.quick_settings_item_tint_default));
                break;
            }
            case R.id.qs_btn_cast:
            {
                //player cast mode
                break;
            }
            case R.id.qs_btn_captions:
            {
                //subtitles toggle
                break;
            }
            case R.id.qs_btn_skip_intro:
            {
                //skip the (anime) intro
                //quick hack: pretend you use some fancy API to get how long a anime opening is,
                //but actually just always skip 85 seconds since that is (roughly) how long most openings are :P
                //nobody could tell anyways ;)
                if (playbackService != null)
                    playbackService.seekRelative(85000);

                break;
            }
            case R.id.qs_btn_app_settings:
            {
                //open global app settings
                startActivity(new Intent(this, AppSettingsActivity.class));
                break;
            }
            case R.id.qs_btn_detection_result:
            {
                String searchResult = ffmpegWrapper.getSearchResult();
                if (searchResult != null) {
                    Intent searchIntent = new Intent(this, ResultViewActivity.class);
                    searchIntent.putExtra("searchResult", searchResult);
                    startActivity(searchIntent);
                }
                else
                    Toast.makeText(getApplicationContext(), "Processing...", Toast.LENGTH_LONG).show();
                break;
            }

            //endregion

            //region ~~ Player Effect Settings ~~
            case R.id.qs_btn_a4k_tgl:
            {
                //toggle anime4k on/off
                setAnime4kEnabled(!getIsAnime4kEnabled());

                //update button
                anime4kQSButton.setIconTint(getIsAnime4kEnabled() ? getColor(R.color.quick_settings_item_tint_active) : getColor(R.color.quick_settings_item_tint_default));
                break;
            }
            //endregion

            case R.id.pb_quick_settings:
            {
                //open quick settings
                quickAccessDrawer.openDrawer(GravityCompat.END);
                break;
            }
        }
    }
    //endregion

    //region ~~ PIP Mode ~~

    /**
     * Constants for PIP mode
     */
    private static final class PIPConstants
    {
        /**
         * Intent Action for media controls
         */
        private static final String ACTION_MEDIA_CONTROL = "media_control";

        /**
         * Intent Extra key for the request id
         */
        private static final String EXTRA_REQUEST_ID = "control_type";

        /**
         * Request id to Play/Pause playback
         */
        private static final int REQUEST_PLAY_PAUSE = 0;

        /**
         * Request id to Replay the video
         */
        private static final int REQUEST_REPLAY = 1;

        /**
         * Request id to fast- forward the video
         */
        private static final int REQUEST_FAST_FORWARD = 2;

        /**
         * Request id to rewind the video
         */
        private static final int REQUEST_REWIND = 3;
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode)
    {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);

        //hide controls when entering pip, re-enable when exiting pip
        setUseController(!isInPictureInPictureMode);

        if (isInPictureInPictureMode)
        {
            //changed to pip mode, register broadcast receiver
            initPipBroadcastReceiverOnce();
            registerReceiver(pipBroadcastReceiver, new IntentFilter(PIPConstants.ACTION_MEDIA_CONTROL));

            //TODO: test on real device
            //set screen brightness to 0 (=auto) (kinda hacky, but should work just fine)
            //adjustScreenBrightness(-1000, true);

            //hide info text
            playerControlView.hideInfoText(true);
        }
        else
        {
            //changed to normal mode, remove pip broadcast receiver
            unregisterReceiver(pipBroadcastReceiver);
        }

        //set flag for outside use
        isPictureInPicture = isInPictureInPictureMode;
    }

    /**
     * Go into PIP mode
     */
    private void tryGoPip()
    {
        //lock out devices below API26 (don't support PIP)
        if (Util.SDK_INT < 26) return;

        //update pip and enter pip if update succeeded
        PictureInPictureParams params = updatePipControls();
        if (params != null)
        {
            //can enter pip mode, hide ui elements:
            //hide player controls
            setUseController(false);

            //hide quick settings and effect drawers
            quickAccessDrawer.closeDrawers();

            //hide info text immediately
            playerControlView.hideInfoText(true);

            //enter pip
            enterPictureInPictureMode(params);
        }
    }

    /**
     * Initializes the Broadcast Receiver used to receive events in PIP mode
     * Initializes the receiver only once
     */
    private void initPipBroadcastReceiverOnce()
    {
        //only init once
        if (pipBroadcastReceiver != null) return;

        //initialize the receiver
        pipBroadcastReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                //ignore any intent that is not ACTION_MEDIA_CONTROL
                if (intent == null || intent.getAction() == null || !intent.getAction().equals(PIPConstants.ACTION_MEDIA_CONTROL))
                    return;

                //do stuff based on request id
                int rId = intent.getIntExtra(PIPConstants.EXTRA_REQUEST_ID, -1);
                switch (rId)
                {
                    case PIPConstants.REQUEST_PLAY_PAUSE:
                    {
                        //play/pause request, toggle playWhenReady
                        playbackService.setPlayWhenReady(!playbackService.getPlayWhenReady());
                        break;
                    }
                    case PIPConstants.REQUEST_REPLAY:
                    {
                        //replay request, set playWhenReady and seek to 0
                        playbackService.seekTo(0);
                        playbackService.setPlayWhenReady(true);
                        break;
                    }
                    case PIPConstants.REQUEST_FAST_FORWARD:
                    {
                        //fast- forward request, fast- forward video
                        playbackService.seekRelative(ConfigUtil.getConfigInt(getApplicationContext(), ConfigKeys.KEY_SEEK_BUTTON_INCREMENT, R.integer.DEF_SEEK_BUTTON_INCREMENT));
                        break;
                    }
                    case PIPConstants.REQUEST_REWIND:
                    {
                        //rewind request, rewind video
                        playbackService.seekRelative(-ConfigUtil.getConfigInt(getApplicationContext(), ConfigKeys.KEY_SEEK_BUTTON_INCREMENT, R.integer.DEF_SEEK_BUTTON_INCREMENT));
                        break;
                    }
                    default:
                    {
                        //invalid request id, log
                        Logging.logW("Received Invalid PIP Request id: %d", rId);
                        break;
                    }
                }
            }
        };
    }

    /**
     * Update the controls of PIP mode
     * Only enter PIP mode when returned true using enterPictureInPictureMode()
     *
     * @return the set pictureInPictureParams object, or null if not supported
     */
    private PictureInPictureParams updatePipControls()
    {
        //lock out devices below API26 (don't support PIP)
        if (Util.SDK_INT < 26) return null;

        //create a list with all actions
        ArrayList<RemoteAction> actions = new ArrayList<>();

        //add buttons:
        //reverse button
        actions.add(createPipAction(PIPConstants.REQUEST_REWIND, R.drawable.ic_fast_rewind_black_24dp, R.string.pip_title_rewind, R.string.exo_controls_rewind_description));

        //region ~~Play/Pause/Replay Button ~~
        if (isPlaybackEnded)
        {
            //ended, show replay button
            actions.add(createPipAction(PIPConstants.REQUEST_REPLAY, R.drawable.ic_replay_black_24dp, R.string.pip_title_replay, R.string.exo_controls_play_description));
        }
        else
        {
            //playing or paused
            if (playbackService.getPlayWhenReady())
            {
                //currently playing, show pause button
                actions.add(createPipAction(PIPConstants.REQUEST_PLAY_PAUSE, R.drawable.ic_pause_black_24dp, R.string.pip_title_pause, R.string.exo_controls_pause_description));
            }
            else
            {
                //currently paused, show play button
                actions.add(createPipAction(PIPConstants.REQUEST_PLAY_PAUSE, R.drawable.ic_play_arrow_black_24dp, R.string.pip_title_play, R.string.exo_controls_play_description));
            }
        }
        //endregion

        //fast- forward button
        actions.add(createPipAction(PIPConstants.REQUEST_FAST_FORWARD, R.drawable.ic_fast_forward_black_24dp, R.string.pip_title_forward, R.string.exo_controls_fastforward_description));

        //built the pip params
        PictureInPictureParams params = new PictureInPictureParams.Builder().setActions(actions).build();

        //this is how you update action items (etc.) for PIP mode.
        //this call can happen even if not in pip mode. In that case, the params
        //will be used for the next call of enterPictureInPictureMode
        setPictureInPictureParams(params);
        return params;
    }

    /**
     * Create a RemoteAction with MEDIA_CONTROL action and @requestID as extra REQUEST_ID
     *
     * @param requestId            the request id
     * @param resId                the ressource id for the icon
     * @param titleId              the title string id of the action
     * @param contentDescriptionId the description string id of the action
     * @return the remote action, or null if the Android Device does not support PIP (<API26)
     */
    private RemoteAction createPipAction(int requestId, int resId, int titleId, int contentDescriptionId)
    {
        //make absolutely sure that device is >= API26
        if (Util.SDK_INT < 26) return null;

        //create pending intent with MEDIA_CONTROL action and requestID as extra REQUEST_ID
        //PendingIntent pIntent = PendingIntent.getActivity(this, requestId,
        //        new Intent(PIPConstants.ACTION_MEDIA_CONTROL).putExtra(PIPConstants.EXTRA_REQUEST_ID, requestId), 0);
        PendingIntent pIntent = PendingIntent.getBroadcast(this, requestId,
                new Intent(PIPConstants.ACTION_MEDIA_CONTROL).putExtra(PIPConstants.EXTRA_REQUEST_ID, requestId), 0);

        //get the icon of the action by resId
        Icon icon = Icon.createWithResource(this, resId);

        //create the remote action
        return new RemoteAction(icon, getString(titleId), getString(contentDescriptionId), pIntent);
    }

    /**
     * Updates the PIP quick settings button visibility.
     * Hides on devices below API 26 (they don't support pip)
     */
    private void updatePipButtonVisibility()
    {
        if (goPipQSButton != null)
            goPipQSButton.setVisibility((Util.SDK_INT < 26) ? View.GONE : View.VISIBLE);
    }

    //endregion

    //region ~~ Control View / Volume + Brightness change listener

    @Override
    public void onVolumeChange(float volPercent)
    {
        setBufferingIndicatorProgress(volPercent);
    }

    @Override
    public void onBrightnessChange(float brightnessPercent)
    {
        setBufferingIndicatorProgress(brightnessPercent);
    }

    @Override
    public void onNoSwipeClick()
    {
        updateWindowFlags();
    }

    //endregion

    //region ~~ Utility ~~

    /**
     * updates the visiblity of the gl effects drawer
     */
    private void updateGlEffectsDrawer()
    {
        boolean disableGl = ConfigUtil.getConfigBoolean(getApplicationContext(), ConfigKeys.KEY_DISABLE_GL_EFFECTS, R.bool.DEF_DISABLE_GL_EFFECTS);

        //lock effects drawer
        if (quickAccessDrawer != null)
            quickAccessDrawer.setDrawerLockMode(disableGl ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START);
    }

    /**
     * Enable / disable anime4k filter
     *
     * @param enable enable anime4k?
     */
    private void setAnime4kEnabled(boolean enable)
    {
        //skip if gl player view is null (Anime4K button should be disabled in that case, but better safe than sorry)
        if (glPlayerView == null) return;

        Logging.logD("Setting Anime4K Filter to enabled= %b", enable);
        if (enable)
        {
            if (anime4KFilter == null)
            {
                //filter currently not enabled, enable it
                anime4KFilter = new GLAnime4K(this, R.raw.common, R.raw.colorget, R.raw.colorpush, R.raw.gradget, R.raw.gradpush);

                //set anime4k to one pass only
                anime4KFilter.setPasses(1);

                //set a4k as active filter
                glPlayerView.setGlFilter(anime4KFilter);

                //set a4k as video listener
                if (playbackService.getIsPlayerValid())
                {
                    SimpleExoPlayer playerInstance = playbackService.getPlayerInstance();
                    if (playerInstance != null)
                    {
                        playerInstance.addVideoListener(anime4KFilter);
                    }
                }
                //set fps limiting values
                int fpsLimit = -1;
                if (ConfigUtil.getConfigBoolean(this, ConfigKeys.KEY_ANIME4K_FPS_LIMIT_ENABLE, R.bool.DEF_ANIME4K_FPS_LIMIT_EN))
                {
                    //enable the fps limit
                    fpsLimit = ConfigUtil.getConfigInt(this, ConfigKeys.KEY_ANIME4K_FPS_LIMIT, R.integer.DEF_ANIME4K_FPS_LIMIT);
                }
                anime4KFilter.setFpsLimit(fpsLimit);
                Logging.logD("Enabled Anime4K with fps limit= %d", fpsLimit);
            }
        }
        else
        {
            //if filter is currently enabled, disable it
            if (anime4KFilter != null)
            {
                //remove video listener
                if (playbackService.getIsPlayerValid())
                {
                    SimpleExoPlayer playerInstance = playbackService.getPlayerInstance();
                    if (playerInstance != null)
                    {
                        playerInstance.removeVideoListener(anime4KFilter);
                    }
                }

                //remove filter (this calls release on the filter)
                glPlayerView.setGlFilter(null);

                //set variable to null
                anime4KFilter = null;
                Logging.logD("Disabled Anime4K");
            }
        }
    }

    /**
     * @return is the anime4k filter currently enabled?
     */
    private boolean getIsAnime4kEnabled()
    {
        return anime4KFilter != null;
    }

    /**
     * Save the current playback position to save as LAST_PLAYED_POSITION for use in the "resume where i left off" feature
     */
    private void savePlaybackPosition()
    {
        //check playback service is ok
        if (playbackService == null || !playbackService.getIsPlayerValid())
        {
            Logging.logD("Cannot save current playback position: player or playback service invalid!");
            return;
        }

        //get current position
        long pos = playbackService.getPlaybackPosition();

        //save the current position
        savePlaybackPosition(pos);
    }

    /**
     * Save the given playback position to save as LAST_PLAYED_POSITION for use in the "resume where i left off" feature
     *
     * @param positionToSave the playback position to save
     */
    @SuppressLint("ApplySharedPref")
    private void savePlaybackPosition(long positionToSave)
    {
        //get preferences from util class
        SharedPreferences prefs = ConfigUtil.getAppConfig(this);

        //check that the prefs are ok
        if (prefs == null)
        {
            Logging.logD("cannot save playback position: ConfigUtil.getAppConfig() returned null!");
            return;
        }

        //save to prefs
        //we use .commit() here since the main thread could close pretty much the moment this function returns.
        //this would not leave enough time for saving the preferences using .apply() (which is async)...
        prefs.edit().putLong(ConfigKeys.KEY_LAST_PLAYED_POSITION, positionToSave).commit();
        Logging.logD("Saved LAST_PLAYED_POSITION");
    }

    /**
     * saves the current volume and brightness to app preferences
     *
     * @param restoreOriginalVolume should the original volume be restored?
     */
    private void savePersistentValues(@SuppressWarnings("SameParameterValue") boolean restoreOriginalVolume)
    {
        //save volume
        if (ConfigUtil.getConfigBoolean(this, ConfigKeys.KEY_PERSIST_VOLUME_EN, R.bool.DEF_PERSIST_VOLUME_EN))
            ConfigUtil.setConfigInt(this, ConfigKeys.KEY_PERSIST_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC), false);

        //save brightness
        if (ConfigUtil.getConfigBoolean(this, ConfigKeys.KEY_PERSIST_BRIGHTNESS_EN, R.bool.DEF_PERSIST_BRIGHTNESS_EN))
            ConfigUtil.setConfigInt(this, ConfigKeys.KEY_PERSIST_BRIGHTNESS, (int) Math.floor(getWindow().getAttributes().screenBrightness * 100), false);

        //restore original volume after saving
        if (restoreOriginalVolume)
        {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolumeIndex, 0);
        }
    }

    /**
     * Restores persistent volume and brightness from app preferences
     *
     * @param saveOriginalVolume should the original volume be saved?
     */
    private void restorePersistentValues(@SuppressWarnings("SameParameterValue") boolean saveOriginalVolume)
    {
        //save original volume before restore
        if (saveOriginalVolume)
        {
            originalVolumeIndex = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        //restore volume
        if (ConfigUtil.getConfigBoolean(this, ConfigKeys.KEY_PERSIST_VOLUME_EN, R.bool.DEF_PERSIST_VOLUME_EN))
        {
            int persistVolume = ConfigUtil.getAppConfig(this).getInt(ConfigKeys.KEY_PERSIST_VOLUME, originalVolumeIndex);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, persistVolume, 0);
        }

        //restore brightness
        if (ConfigUtil.getConfigBoolean(this, ConfigKeys.KEY_PERSIST_BRIGHTNESS_EN, R.bool.DEF_PERSIST_BRIGHTNESS_EN))
        {
            //get window attributes
            WindowManager.LayoutParams windowAttributes = getWindow().getAttributes();

            //modify screen brightness attribute withing range
            float persistBrightness = (float) ConfigUtil.getAppConfig(this).getInt(ConfigKeys.KEY_PERSIST_BRIGHTNESS, -100) / 100.0f;
            if (persistBrightness > 0)
            {
                windowAttributes.screenBrightness = Math.min(Math.max(persistBrightness, 0f), 1f);
            }

            //set changed window attributes
            getWindow().setAttributes(windowAttributes);
        }
    }

    /**
     * Check if the app was granted the permission.
     * If not granted, the permission will be requested and false will be returned.
     *
     * @param permissions the permission to check
     * @param requestId   the request id. Used to check in callback
     * @return was the permission granted?
     */
    private boolean checkAndRequestPermission(String[] permissions, @SuppressWarnings("SameParameterValue") int requestId)
    {
        //check each permission needed
        boolean hasMissingPermission = false;
        for (String perm : permissions)
        {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED)
            {
                hasMissingPermission = true;
                break;
            }
        }

        //request missing permissions
        if (hasMissingPermission)
        {
            //does not have all permissions, ask for them
            ActivityCompat.requestPermissions(this, permissions, requestId);
        }

        return !hasMissingPermission;
    }

    /**
     * Set the title shown on the titleTextView
     *
     * @param title the title to show. if set to a empty string, the title label is hidden
     */
    private void setTitle(String title)
    {
        if (title == null || title.isEmpty())
        {
            //no title, hide title label
            titleTextView.setText("N/A");
            titleTextView.setVisibility(View.INVISIBLE);
        }
        else
        {
            //show title label
            titleTextView.setText(title);
            titleTextView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Set the visibility of the player controller
     *
     * @param useController should the controller be used?
     */
    private void setUseController(boolean useController)
    {
        Logging.logD("setUseController() useController= %b", useController);

        //skip if not everything is ok
        if (playerControlView == null || playbackService == null || !playbackService.getIsPlayerValid())
        {
            Logging.logW("setUseController(): either playerControlView or playbackService is not valid!");
            return;
        }

        if (useController)
        {
            if (playerControlView.getPlayer() != playbackService.getPlayerInstance())
            {
                //player not set, fix that
                playerControlView.setPlayer(playbackService.getPlayerInstance());
            }

            //show controls
            playerControlView.getPlayerControls().setControlsHidden(false).show();
        }
        else
        {
            //hide controls
            playerControlView.getPlayerControls().setControlsHidden(true).hide();
        }
    }

    /**
     * sets the progress of the normal (NOT pip) buffering indicator
     * used by volume & brightness controls, indicator fills as you swipe
     * blocks the indicator from being used as buffering indicator
     * automatically resets the progress & function of the indicator after fixed duration
     *
     * @param progressPercent how much the indicator should be filled (0.0 - 1.0)
     */
    private void setBufferingIndicatorProgress(float progressPercent)
    {
        //block buffering indicator
        isBufferingIndicatorBlocked = true;

        //set progress of normal indicator instantly and make it visible
        bufferingIndicatorNormal.setWillNotDraw(false);
        bufferingIndicatorNormal.setInstantProgress(progressPercent);
        bufferingIndicatorNormal.setLinearProgress(true);

        //reset later
        int indicatorVisibleDuration = getResources().getInteger(R.integer.info_buffering_indicator_duration)
                + ConfigUtil.getConfigInt(this, ConfigKeys.KEY_INFO_TEXT_DURATION, R.integer.DEF_INFO_TEXT_DURATION);
        delayHandler.removeMessages(Messages.UNBLOCK_BUFFERING_INDICATOR);
        delayHandler.sendEmptyMessageDelayed(Messages.UNBLOCK_BUFFERING_INDICATOR, indicatorVisibleDuration);
    }

    /**
     * set the visibility of the buffering indicator
     *
     * @param isBuffering is currently buffering? (=visible)
     * @param isPip       is the app in pip mode?
     */
    private void setBufferingIndicatorVisible(boolean isBuffering, boolean isPip)
    {
        isBufferingState = isBuffering;

        if (isBuffering)
        {
            //make right indicator visible and spin
            if (isPip)
            {
                //pip
                bufferingIndicatorPip.setWillNotDraw(false);
                if (!isBufferingIndicatorBlocked)
                    bufferingIndicatorPip.spin();
            }
            else
            {
                //normal
                bufferingIndicatorNormal.setWillNotDraw(false);
                if (!isBufferingIndicatorBlocked)
                    bufferingIndicatorNormal.spin();
            }

            //if (bufferingIndicatorNormal != null)
            //    bufferingIndicatorNormal.setVisibility(isPip ? View.GONE : View.VISIBLE);
            //if (bufferingIndicatorPip != null)
            //    bufferingIndicatorPip.setVisibility(isPip ? View.VISIBLE : View.GONE);
        }
        else
        {
            //make both invisible
            if (!isBufferingIndicatorBlocked)
                bufferingIndicatorNormal.setWillNotDraw(true);
            bufferingIndicatorPip.setWillNotDraw(true);

            //if (bufferingIndicatorNormal != null) bufferingIndicatorNormal.setVisibility(View.GONE);
            //if (bufferingIndicatorPip != null) bufferingIndicatorPip.setVisibility(View.GONE);
        }
    }

    /**
     * Forces the screen to stay on if keepOn is true, otherwise clears the KEEP_SCREEN_ON flag
     *
     * @param keepOn should the screen stay on?
     */
    private void setScreenForcedOn(boolean keepOn)
    {
        if (keepOn)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else
        {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /**
     * @return is the activity in landscape orientation?
     */
    private boolean isLandscapeOrientation()
    {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }
    //endregion

    /**
     * Contains functionality to set the screen orientation with three buttons
     */
    private class ScreenRotationManager
    {
        //region ~~ Constants ~~

        /**
         * screen set to follow device
         */
        private static final int SCREEN_AUTO = 0;

        /**
         * Screen locked to portrait mode
         */
        private static final int SCREEN_LOCK_PORTRAIT = 1;

        /**
         * Screen locked to landscape mode
         */
        private static final int SCREEN_LOCK_LANDSCAPE = 2;
        //endregion

        /**
         * The Image Button to set the screen to auto/default mode
         */
        ImageButton btnScreenAuto;

        /**
         * The Image Button to set the screen to lock landscape
         */
        ImageButton btnScreenLockLandscape;

        /**
         * The Image Button to set the screen to locked portrait
         */
        ImageButton btnScreenLockPortrait;

        /**
         * Initially find the components that are the three buttons
         */
        private void findComponents()
        {
            btnScreenAuto = findViewById(R.id.pb_screen_rotation_auto);
            btnScreenLockLandscape = findViewById(R.id.pb_screen_rotation_landscape);
            btnScreenLockPortrait = findViewById(R.id.pb_screen_rotation_portrait);
        }

        /**
         * Set the screen mode + set button visibility
         *
         * @param mode the mode to set
         */
        private void setScreenMode(int mode)
        {
            //act according to mode to set
            switch (mode)
            {
                case SCREEN_AUTO:
                {
                    //enable auto button + reset requested orientation
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);

                    btnScreenAuto.setVisibility(View.VISIBLE);
                    btnScreenLockLandscape.setVisibility(View.INVISIBLE);
                    btnScreenLockPortrait.setVisibility(View.INVISIBLE);

                    Logging.logD("Changed Screen mode to AUTO");
                    break;
                }
                case SCREEN_LOCK_PORTRAIT:
                {
                    //enable lock-portrait button + set requested orientation to portrait
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                    btnScreenLockPortrait.setVisibility(View.VISIBLE);
                    btnScreenAuto.setVisibility(View.INVISIBLE);
                    btnScreenLockLandscape.setVisibility(View.INVISIBLE);

                    Logging.logD("Changed Screen mode to PORTRAIT");
                    break;
                }
                case SCREEN_LOCK_LANDSCAPE:
                {
                    //enable lock-landscape button + set requested orientation to landscape
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

                    btnScreenLockLandscape.setVisibility(View.VISIBLE);
                    btnScreenAuto.setVisibility(View.INVISIBLE);
                    btnScreenLockPortrait.setVisibility(View.INVISIBLE);

                    Logging.logD("Changed Screen mode to LANDSCAPE");
                    break;
                }
            }
        }
    }

    /**
     * The connection to the video playback service
     */
    private class VideoServiceConnection implements ServiceConnection
    {
        /**
         * Is the video playback service currently bound?
         */
        boolean isConnected = false;

        /**
         * called when we connected to the service
         *
         * @param componentName ?
         * @param binder        the service binder we can use to access the service
         */
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder)
        {
            Logging.logD("Service connected!");

            //check binder is right type
            if (binder instanceof VideoPlaybackService.VideoServiceBinder)
            {
                //set service instance
                VideoPlaybackService.VideoServiceBinder serviceBinder = (VideoPlaybackService.VideoServiceBinder) binder;
                playbackService = serviceBinder.getServiceInstance();

                //set flag
                isConnected = true;

                //set callback
                playbackService.setListener(new VideoServiceCallbackListener());

                //load the media
                playbackService.loadMedia(playbackUri, playbackPlayWhenReady, playbackStartPosition);
            }
        }

        /**
         * Called when we disconnected from the service
         *
         * @param componentName ?
         */
        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            Logging.logD("Service Disconnected!");

            //set flag
            isConnected = false;
        }
    }

    /**
     * listens for callbacks of the video service
     */
    private class VideoServiceCallbackListener implements VideoPlaybackServiceListener
    {
        /**
         * the player of the service has been initialized and can now be used to, for example, set the render surface.
         * this is called in onBind() of the service as well as in the end of loadMedia() function
         */
        @Override
        public void onPlayerInitialized()
        {
            //get scale type to use
            PlayerScaleType scaleType;
            if (ConfigUtil.getConfigBoolean(getApplicationContext(), ConfigKeys.KEY_SCALE_TO_WIDTH, R.bool.DEF_SCALE_TO_WIDTH))
            {
                //scale the video to fill the whole width available, even if part of the video is cropped
                scaleType = PlayerScaleType.FillWidth;
            }
            else
            {
                //scale the video to fit inside the viewport (avoid cropping the video)
                scaleType = PlayerScaleType.Fit;
            }

            View pView;
            if (ConfigUtil.getConfigBoolean(getApplicationContext(), ConfigKeys.KEY_DISABLE_GL_EFFECTS, R.bool.DEF_DISABLE_GL_EFFECTS))
            {
                //use normal player view
                pView = playerView = new AVPPlayerView(playerViewPlaceholder.getContext());

                //set player of view
                playerView.setSimplePlayer(playbackService.getPlayerInstance());

                //set scale type
                playerView.setPlayerScaleType(scaleType);
            }
            else
            {
                //use gl player view
                pView = glPlayerView = new AVPEPlayerView(playerViewPlaceholder.getContext());

                //set player of view
                glPlayerView.setSimpleExoPlayer(playbackService.getPlayerInstance());

                //set scale type
                glPlayerView.setPlayerScaleType(scaleType);
            }

            //adjust layout
            pView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            //remove all previous children
            playerViewPlaceholder.removeAllViews();

            //add player view to placeholder
            playerViewPlaceholder.addView(pView);

            //make controls visible
            setUseController(true);
        }

        /**
         * the media that was attempted to be loaded is in a location that cannot be accessed with the app's current permissions.
         * for local files, this means that {@link android.Manifest.permission#READ_EXTERNAL_STORAGE} is missing and should be requested.
         * for streamed files, {@link android.Manifest.permission#INTERNET} is missing and should be requested.
         * <p>
         * if this event is called, the media loading has been aborted and the video service is NOT ready for playback!
         * use this event to request the permissions, and call reloadMedia() once you have the needed permissions
         *
         * @param permissions the permission(s) that are missing for loading the media
         */
        @Override
        public void onMissingPermissions(@NonNull String[] permissions)
        {
            //ignore if permissions list is empty
            if (permissions.length <= 0) return;

            //request permissions
            if (checkAndRequestPermission(permissions, PERMISSION_REQUEST_READ_VIDEO_SOURCE))
            {
                //we already have all permissions? reload media then!
                Logging.logD("onMissingPermissions(): we already have all permissions? reloading media...");
                playbackService.reloadMedia();
            }
        }

        /**
         * A error occurred in the video playback service!
         *
         * @param error the error that occurred (most likely a ExoPlayerException)
         */
        @Override
        public void onError(Exception error)
        {
            //log error
            Logging.logE("VideoServiceCallbackListener:onError(): %s", error.toString());

            //save playback position to prefs to be able to resume later
            savePlaybackPosition();

            //try to call exception handler for the app (to open exception handler)
            Application app = getApplication();
            if (app instanceof AVPApp)
            {
                AVPApp AndroidVideoPlayer = (AVPApp) app;
                AndroidVideoPlayer.uncaughtException(Thread.currentThread(), error);
            }
        }

        /**
         * The current media was loaded and is ready for playback
         *
         * @param playWhenReady is the player set to play as soon as media is ready?
         */
        @Override
        public void onPlaybackReady(boolean playWhenReady)
        {
            //put screen lock while playing
            setScreenForcedOn(playWhenReady);

            //reset play button graphic if still replay button
            if (isPlaybackEnded)
            {
                isPlaybackEnded = false;
                playButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            }
        }

        /**
         * The current media finished playback
         */
        @Override
        public void onPlaybackEnded()
        {
            //lift screen lock
            setScreenForcedOn(false);

            //reset lastPlaybackPosition so we restart the video next time
            savePlaybackPosition(0);

            //prevent onStop() from overwriting the position
            dontSavePlaybackPositionOnExit = true;

            //close app if pref is set
            if (ConfigUtil.getConfigBoolean(getApplicationContext(), ConfigKeys.KEY_CLOSE_WHEN_FINISHED_PLAYING, R.bool.DEF_CLOSE_WHEN_FINISHED_PLAYING))
            {
                //close app
                finish();
            }
            else
            {
                //change play button graphic to replay button
                playButton.setImageResource(R.drawable.ic_replay_black_24dp);
                isPlaybackEnded = true;
            }
        }

        /**
         * the player state changed.
         * !! you dont really need to use this, this is more to keep possibilities open... !!
         *
         * @param playerState the new playback state of the player
         */
        @Override
        public void onPlayerStateChange(int playerState)
        {
            //update pip controls on every state change
            if (isPictureInPicture)
                updatePipControls();
        }

        /**
         * The buffering state of the player changed
         *
         * @param isBuffering is the player currently buffering?
         */
        @Override
        public void onBufferingChanged(boolean isBuffering)
        {
            if (isBuffering)
            {
                //make buffering indicator visible after delay
                delayHandler.sendEmptyMessageDelayed(Messages.SHOW_BUFFERING_INDICATOR, BUFFERING_INDICATOR_LAZY_TIME_MS);
            }
            else
            {
                //make buffering indicator invisible and cancel all events that would make it visible
                delayHandler.removeMessages(Messages.SHOW_BUFFERING_INDICATOR);
                setBufferingIndicatorVisible(false, isPictureInPicture);
            }
        }

        /**
         * The player received new metadata
         *
         * @param metadata the metadata received
         */
        @Override
        public void onNewMetadata(Metadata metadata)
        {

        }
    }

    /**
     * Listener for the quick access drawer
     */
    private class QuickAccessDrawerListener extends DrawerLayout.SimpleDrawerListener
    {
        @Override
        public void onDrawerOpened(View drawerView)
        {
            //keep playback controls forced open while any drawer is open
            //check player controls are ok to use
            if (playerControlView == null || playerControlView.getPlayerControls() == null) return;

            //disable auto- hide on player controls
            //and show the controls to make sure they are visible
            playerControlView.getPlayerControls().setAllowAutoHideControls(false).show();
        }

        @Override
        public void onDrawerClosed(View drawerView)
        {
            //as soon as drawer is closed, allow auto- hide of playback controls
            //check player controls are ok to use
            if (playerControlView == null || playerControlView.getPlayerControls() == null) return;

            //re- enable auto- hide on player controls
            //call showControls() to ensure the controls don't instantly disappear
            playerControlView.getPlayerControls().setAllowAutoHideControls(true).showControls(false);
        }
    }

    /**
     * Listener for player controls, that listens for visibility changes and locks / unlocks the quick access drawer accordingly
     */
    private class PlayerControlsVisibilityListener implements TapToHidePlayerControlView.VisibilityChangeCallback
    {
        /**
         * Callback when the controls transition from hidden to visible
         */
        public void onShow()
        {
            //controls are visible, unlock drawer
            quickAccessDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            updateGlEffectsDrawer();
        }

        /**
         * Callback when the controls transition from visible to hidden
         */
        public void onHide()
        {
            //controls are not visible, lock drawers closed
            quickAccessDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
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
