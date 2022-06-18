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
import kr.ac.sogang.mmlab.AndroidVideoPlayer.feature.playback.VideoPlaybackService;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.ui.resultView.ResultAdapter;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.ui.resultView.ResultItem;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.Logging;

public class ResultViewActivity extends AppCompatActivity {
    @Nullable private PlayerView playerView;
    private SimpleExoPlayer player;
    private JSONObject searchResult;
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
            searchResult =  new JSONObject(getIntent().getStringExtra("searchResult"));
        } catch (JSONException e) {
            Logging.logE("Parse Error");
            e.printStackTrace();
        }
        playerView.setPlayer(player);

        resultItemList = new ArrayList<>();
        String url, video, video_name, thumbnail, name, feature, uploaded_date, updated_date, rank1_video = "";
        String[] video_name_tmp;
        JSONObject metadata;
        int topk, window, match_threshold;
        double score_threshold;
        JSONArray results;

        try {
            url = searchResult.getString("url");
            video = searchResult.getString("_video");
            video_name_tmp = video.split("/");
            video_name = video_name_tmp[video_name_tmp.length - 1];
            thumbnail = searchResult.getString("thumbnail");
            name = searchResult.getString("name");
            metadata = searchResult.getJSONObject("metadata");
            topk = searchResult.getInt("topk");
            window = searchResult.getInt("window");
            score_threshold = searchResult.getDouble("score_threshold");
            match_threshold = searchResult.getInt("match_threshold");
            feature = searchResult.getString("feature");
            uploaded_date = searchResult.getString("uploaded_date");
            updated_date = searchResult.getString("updated_date");
            results = searchResult.getJSONArray("results");

            for(int i = 0; i < results.length(); i++) {
                JSONObject result = results.getJSONObject(i);
                int rank = result.getInt("rank");
                int query_start = result.getInt("query_start");
                int query_end = result.getInt("query_end");
                int reference_start = result.getInt("reference_start");
                int reference_end = result.getInt("reference_end");
                int match = result.getInt("match");
                double score = result.getDouble("score");
                JSONObject reference = result.getJSONObject("reference");
                String ref_video = reference.getString("_video");
                String[] ref_video_name_tmp = ref_video.split("/");
                String ref_video_name = ref_video_name_tmp[ref_video_name_tmp.length - 1];

                ResultItem item = new ResultItem(ref_video, ref_video_name, rank, query_start, query_end, reference_start, reference_end, match, score);
                resultItemList.add(item);

                if (i == 0){
                    rank1_video = ref_video;
                }
            }
            MediaSource mediaSource = buildMediaSource(Uri.parse(rank1_video));
            player.prepare(mediaSource, true, false);
            playerView.setControllerVisibilityListener(new PlayerControlView.VisibilityListener() {
                @Override
                public void onVisibilityChange(int visibility) {
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ResultAdapter adapter = new ResultAdapter(this, R.layout.result_item, resultItemList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                Toast.makeText(getApplicationContext(), "Loading...", Toast.LENGTH_SHORT).show();
                playerView.onPause();
                ResultItem selectedItem = resultItemList.get(position);
                String video_url = selectedItem.getVideo();
                double reference_start = selectedItem.getReferenceStart();
                double reference_end = selectedItem.getReferenceEnd();
                MediaSource mediaSource = buildMediaSource(Uri.parse(video_url));
                player.prepare(mediaSource, true, false);
                playerView.setControllerVisibilityListener(new PlayerControlView.VisibilityListener() {//플레이어 컨트롤러 셋팅
                    @Override
                    public void onVisibilityChange(int visibility) {
                    }
                });
                player.seekTo(player.getCurrentWindowIndex(), (int)(reference_start * 1000));
            }
        });
    }

    private MediaSource buildMediaSource(Uri uri) {
        String userAgent = Util.getUserAgent(this, "blackJin");

        return new ExtractorMediaSource.Factory(new DefaultHttpDataSourceFactory(userAgent))
                .createMediaSource(uri);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        player.stop();
    }
}