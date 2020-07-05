package kr.ac.sogang.mmlab.mobilemediadecoder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private VideoView videoView;
    private Button buttonCapture;
    private MediaMetadataRetriever mediaMetadataRetriever;
    private MediaController myMediaController;

    String[] PERMISSIONS = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
    static final int PERMISSIONS_REQUEST_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        // TextView tv = findViewById(R.id.sample_text);
        // tv.setText(stringFromJNI());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermissions(PERMISSIONS)) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }

        String video_path = "file://" + Environment.getExternalStorageDirectory().toString() + "/Download/test_1080p.mp4";
        File files = new File(video_path);
        if(files.exists()==true) {
            Log.e("VIDEO_NAME", video_path + " exist");
        } else {
            Log.e("VIDEO_NAME", "Not exist");
        }

        videoView = (VideoView) findViewById(R.id.videoView);
        buttonCapture = (Button) findViewById(R.id.capture);
        videoView.setVideoURI(Uri.parse(video_path));
        mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(MainActivity.this, Uri.parse(video_path));
        myMediaController = new MediaController(this);
        videoView.setMediaController(myMediaController);

        videoView.setOnCompletionListener(videoViewCompletionListener);
        videoView.setOnPreparedListener(videoViewPreparedListener);
        videoView.setOnErrorListener(videoViewErrorListener);

        videoView.requestFocus();
        videoView.start();
        buttonCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                int currentPosition = videoView.getCurrentPosition(); //in millisecond
                Toast.makeText(MainActivity.this, "Current Position: " + currentPosition + " (ms)", Toast.LENGTH_LONG).show();
                Bitmap bmFrame = mediaMetadataRetriever.getFrameAtTime(currentPosition * 1000); //unit in microsecond
                Log.e("BITMAP", "witdth: " + Integer.toString(bmFrame.getWidth()) + " / height: " + Integer.toString(bmFrame.getHeight()));

            }
        });
    }

    MediaPlayer.OnCompletionListener videoViewCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer arg0) {
            Toast.makeText(MainActivity.this, "End of Video", Toast.LENGTH_LONG).show();
        }
    };

    MediaPlayer.OnPreparedListener videoViewPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            long duration = videoView.getDuration(); //in millisecond
            Toast.makeText(MainActivity.this, "Duration: " + duration + " (ms)", Toast.LENGTH_LONG).show();

        }
    };

    MediaPlayer.OnErrorListener videoViewErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Toast.makeText(MainActivity.this, "Error!!!", Toast.LENGTH_LONG).show();
            return true;
        }
    };

    private boolean hasPermissions(String[] permissions) {
        int result;

        for (String perms : permissions) {
            result = ContextCompat.checkSelfPermission(this, perms);
            if (result == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
