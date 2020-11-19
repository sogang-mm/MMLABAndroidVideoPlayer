package kr.ac.sogang.mmlab.AndroidVideoPlayer.ui;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import kr.ac.sogang.R;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.ui.resultView.ResultAdapter;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.ui.resultView.ResultItem;

public class ResultViewActivity extends AppCompatActivity {
    @Nullable private PlayerView playerView;
    private SimpleExoPlayer player;
    private JSONArray searchResult;
    private ListView listView;
    private ArrayList<ResultItem> resultItemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_view_activity);

        playerView = findViewById(R.id.result_player_view);
        listView = (ListView) findViewById(R.id.result_player_list);

        player = ExoPlayerFactory.newSimpleInstance(this.getApplicationContext());
        try {
            searchResult =  new JSONArray(getIntent().getStringExtra("searchResult"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        playerView.setPlayer(player);

        resultItemList = new ArrayList<>();
        for(int i=0; i<searchResult.length(); i++){
            try {
                JSONObject result = searchResult.getJSONObject(i);
                String reference = result.getString("reference");
                String referenceSegment = result.getString("reference_segment");
                String score = result.getString("score");
                String querySegment = result.getString("query_segment");
                String count = result.getString("count");
                ResultItem item = new ResultItem(0, reference, referenceSegment, score);
                resultItemList.add(item);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        MediaSource mediaSource = buildMediaSource(Uri.parse("http://mlcupid.sogang.ac.kr:8777/videos/vcdb_core_mp4/" + resultItemList.get(0).getReference() + ".mp4"));
        player.prepare(mediaSource, true, false);

        playerView.setControllerVisibilityListener(new PlayerControlView.VisibilityListener() {//플레이어 컨트롤러 셋팅
            @Override
            public void onVisibilityChange(int visibility) {
            }
        });
        ResultAdapter adapter = new ResultAdapter(this, R.layout.result_item, resultItemList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                Toast.makeText(getApplicationContext(), "Prepareing...", Toast.LENGTH_LONG).show();
                playerView.onPause();
                String[] segment = resultItemList.get(position).getReferenceSegment().split(" - ");
                float start = Float.parseFloat(segment[0]);
                float end = Float.parseFloat(segment[1]);
                MediaSource mediaSource = buildMediaSource(Uri.parse("http://mlcupid.sogang.ac.kr:8777/videos/vcdb_core_mp4/" + resultItemList.get(position).getReference() + ".mp4"));
                player.prepare(mediaSource, true, false);
                playerView.setControllerVisibilityListener(new PlayerControlView.VisibilityListener() {//플레이어 컨트롤러 셋팅
                    @Override
                    public void onVisibilityChange(int visibility) {
                    }
                });
                player.seekTo(player.getCurrentWindowIndex(), (int)(start * 1000));
            }
        });

    }

    private MediaSource buildMediaSource(Uri uri) {
        String userAgent = Util.getUserAgent(this, "blackJin");

        return new ExtractorMediaSource.Factory(new DefaultHttpDataSourceFactory(userAgent))
                .createMediaSource(uri);
    }
}