package kr.ac.sogang.mmlab.AndroidVideoPlayer.ui.resultView;

public class ResultItem {
    private String video;
    private String video_name;
    private int rank, query_start, query_end;
    private int reference_start, reference_end, match;
    private double score;

    public String getVideo() {
        return video;
    }
    public String getVideoName() {
        return video_name;
    }
    public int getRank() { return rank; }
    public int getQueryStart() { return query_start; }
    public int getQueryEnd() { return query_end; }
    public int getReferenceStart() {
        return reference_start;
    }
    public int getReferenceEnd() {
        return reference_end;
    }
    public int getMatch() { return match; }
    public double getScore() {
        return score;
    }


    public ResultItem(String video, String video_name, int rank, int query_start, int query_end, int reference_start, int reference_end, int match, double score) {
        this.video = video;
        this.video_name = video_name;
        this.rank = rank;
        this.query_start = query_start;
        this.query_end = query_end;
        this.reference_start = reference_start;
        this.reference_end = reference_end;
        this.match = match;
        this.score = score;
    }


}
