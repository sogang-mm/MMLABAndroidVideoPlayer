package kr.ac.sogang.mmlab.AndroidVideoPlayer.filter;

import android.graphics.Bitmap;

public class BaseModel {
    float avg_PreprocessTime = 0f;
    float avg_ModelTime = 0f;
    int cnt = 0;
    // TODO: Model initialization
    public BaseModel() {

    }

    // TODO: Inference function
    public boolean inference(Bitmap bitmap) {

        return true;
    }

    public float getAvgPreprocessTime()
    {
        return avg_PreprocessTime / cnt;
    }
    public float getAvgModelTime()
    {
        return avg_ModelTime / cnt;
    }
}
