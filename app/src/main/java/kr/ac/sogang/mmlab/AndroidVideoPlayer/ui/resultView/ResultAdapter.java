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
        ResultItem friendsItem = data.get(position);

//        ImageView thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);
//        thumbnail.setImageResource(friendsItem.getThumbnail());

        TextView reference = (TextView) convertView.findViewById(R.id.reference);
        reference.setText("Rank: " + Integer.toString(friendsItem.getRank()));

//        TextView refrenceSegment = (TextView) convertView.findViewById(R.id.referenceSegment);
//        refrenceSegment.setText("Segment: " + friendsItem.getReferenceSegment());

        TextView score = (TextView) convertView.findViewById(R.id.score);
        score.setText("Score: " + Double.toString(friendsItem.getScore()));

        return convertView;
    }
}
