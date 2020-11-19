package kr.ac.sogang.mmlab.AndroidVideoPlayer.ui.resultView;

public class ResultItem {
    private int thumbnail;
    private String reference;
    private String referenceSegment; // 친구 정보
    private String score; // 핸드폰 번호

    public int getThumbnail() {
        return thumbnail;
    }

    public String getReference() {
        return reference;
    }

    public String getReferenceSegment() {
        return referenceSegment;
    }

    public String getScore() {
        return score;
    }


    public ResultItem(int thumbnail, String reference, String referenceSegment, String score) {
        this.thumbnail = thumbnail;
        this.reference = reference;
        this.referenceSegment = referenceSegment;
        this.score = score;
    }
}
