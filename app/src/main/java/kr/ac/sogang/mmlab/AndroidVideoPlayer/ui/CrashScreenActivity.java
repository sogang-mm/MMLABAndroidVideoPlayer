package kr.ac.sogang.mmlab.AndroidVideoPlayer.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import kr.ac.sogang.R;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.ConfigKeys;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class CrashScreenActivity extends AppCompatActivity
{
    public static final String INTENT_EXTRA_THREAD_NAME = "exThreadName";
    public static final String INTENT_EXTRA_CAUSE_SHORT = "exCauseShort";
    public static final String INTENT_EXTRA_CAUSE_MESSAGE = "exCauseMessage";
    public static final String INTENT_EXTRA_CAUSE_STACKTRACE = "exCauseStacktrace";

    SharedPreferences appPreferences;

    Button resumePlaybackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.crash_activity);

        appPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        resumePlaybackButton = findViewById(R.id.crashac_btn_resume);

        resumePlaybackButton.setEnabled(canResumePlayback());

        Intent i = getIntent();
        if (i == null) return;

        String crashDetails = i.getStringExtra(INTENT_EXTRA_CAUSE_STACKTRACE);

        if (crashDetails == null || crashDetails.isEmpty())
        {
            StringBuilder s = new StringBuilder();
            s.append("no stacktrace! dumping intent now:\n")
                    .append(i.toString())
                    .append("\n")
                    .append(i.getData())
                    .append("\n\n");

            Bundle extras = i.getExtras();
            if (extras != null)
            {
                for (String key : extras.keySet())
                {
                    s.append(key)
                            .append(" = ")
                            .append(extras.get(key));
                }
            }
            else
            {
                s.append("no extras");
            }
            crashDetails = s.toString();
        }

        crashDetails = buildDeviceInfoString() + crashDetails;

        TextView crashDetailsView = findViewById(R.id.crashac_txt_crash_details);
        crashDetailsView.setText(crashDetails);


    }

    public void crashac_OnClick(View view)
    {
        switch (view.getId())
        {
            case R.id.crashac_btn_close_app:
            {
                finish();
                break;
            }
            case R.id.crashac_btn_resume:
            {
                resumePlayback();
                break;
            }
        }
    }

    private String buildDeviceInfoString()
    {
        StringBuilder s = new StringBuilder();
        s.append("Platform Info:\n")
                .append("Device: ")
                .append(Build.MANUFACTURER)//Google
                .append(" ")
                .append(Build.MODEL)//AOSP on IA Emulator
                .append(" (")
                .append(Build.PRODUCT)//sdk_gphone_x86_arm
                .append(")\nBoard: ")
                .append(Build.BOARD)//goldfish_x86
                .append("\nType&Tags: ")
                .append(Build.TYPE)//user
                .append(" (")
                .append(Build.TAGS)//release-keys
                .append(")\nAndroid ")
                .append(Build.VERSION.RELEASE)//9
                .append(" SDK ")
                .append(Build.VERSION.SDK_INT)//28
                .append(" (")
                .append(Build.VERSION.CODENAME)//REL
                .append(")\nABIs: ");

        for (String abi : Build.SUPPORTED_ABIS)
        {
            s.append(abi).append(", ");
        }

        s.append("\n\nStacktrace:\n");
        return s.toString();
    }

    private void resumePlayback()
    {
        if (!canResumePlayback()) return;

        String resumeTitle = getLastPlayedTitle();
        String resumeUrlStr = getLastPlayedUrl();

        Uri resumeUri = Uri.parse(resumeUrlStr);
        if (resumeUri == null) return;

        Intent launchIntent = new Intent(this, LaunchActivity.class);
        launchIntent.setAction(Intent.ACTION_VIEW);
        launchIntent.setData(resumeUri);
        launchIntent.putExtra(Intent.EXTRA_TITLE, resumeTitle);

        startActivity(launchIntent);
        finish();
    }

    private boolean canResumePlayback()
    {
        if (getLastPlayedUrl() == null || getLastPlayedTitle() == null) return false;

        return appPreferences.getLong(ConfigKeys.KEY_LAST_PLAYED_POSITION, -1) > 0;
    }

    private String getLastPlayedUrl()
    {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        return appPreferences.getString(ConfigKeys.KEY_LAST_PLAYED_URL, null);
    }

    private String getLastPlayedTitle()
    {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        return appPreferences.getString(ConfigKeys.KEY_LAST_PLAYED_TITLE, null);
    }
}
