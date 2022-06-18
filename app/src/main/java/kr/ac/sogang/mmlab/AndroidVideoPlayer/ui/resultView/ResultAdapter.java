package kr.ac.sogang.mmlab.AndroidVideoPlayer.ui.resultView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import kr.ac.sogang.R;

public class ResultAdapter  extends BaseAdapter {
    private LayoutInflater inflater;
    private ArrayList<ResultItem> data;
    private int layout;

    public ResultAdapter(Context context, int layout, ArrayList<ResultItem> data) {
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.data = data;
        this.layout = layout;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public String getItem(int position) {
        return data.get(position).getVideoName();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(layout, parent, false);
        }
        ResultItem resultItem = data.get(position);

        TextView textViewVideoName = (TextView) convertView.findViewById(R.id.text_view_video_name);
        TextView textViewRank = (TextView) convertView.findViewById(R.id.text_view_rank);
        TextView textViewScore = (TextView) convertView.findViewById(R.id.text_view_score);
        TextView textViewMatch = (TextView) convertView.findViewById(R.id.text_view_match);
        TextView textViewQuery = (TextView) convertView.findViewById(R.id.text_view_query);
        TextView textViewReference = (TextView) convertView.findViewById(R.id.text_view_reference);
        textViewVideoName.setText(resultItem.getVideoName().replace("_", ""));
        textViewRank.setText(Integer.toString(resultItem.getRank()));
        textViewScore.setText(Double.toString(resultItem.getScore()));
        textViewMatch.setText(Integer.toString(resultItem.getMatch()));
        textViewQuery.setText("(start: " + resultItem.getQueryStart() + " / end: " + resultItem.getQueryEnd() + ")");
        textViewReference.setText("(start: " + resultItem.getReferenceStart() + " / end: " + resultItem.getReferenceEnd() + ")");

        return convertView;
    }
}
