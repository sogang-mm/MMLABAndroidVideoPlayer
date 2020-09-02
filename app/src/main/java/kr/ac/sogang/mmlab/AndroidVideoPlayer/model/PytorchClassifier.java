package kr.ac.sogang.mmlab.AndroidVideoPlayer.model;

import android.graphics.Bitmap;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import kr.ac.sogang.mmlab.AndroidVideoPlayer.filter.BaseModel;
import kr.ac.sogang.mmlab.AndroidVideoPlayer.util.Logging;

public class PytorchClassifier extends BaseModel {
    private Module module = null;
    int inputWidth = 224;
    int inputHeight = 224;


    float avg_PreprocessTime = 0f;
    float avg_ModelTime = 0f;
    int cnt = 0;

    public PytorchClassifier(String modulePath)
    {
        module = Module.load(modulePath);
    }

    public boolean inference(Bitmap bitmap)
    {
        cnt++;
        long s_preTime = System.currentTimeMillis();
        bitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB
        );
        IValue v = IValue.from(inputTensor);
        long e_preTime = System.currentTimeMillis();

        long s_modelTime = System.currentTimeMillis();
        final Tensor outputTensor = module.forward(v).toTensor();
        long e_modelTime = System.currentTimeMillis();
        //int label = getLabelIndex(outputTensor.getDataAsFloatArray());
        float pre = (e_preTime- s_preTime) / 1000f;
        float modelTime = (e_modelTime - s_modelTime) / 1000f;
        avg_PreprocessTime += pre;
        avg_ModelTime += modelTime;
        return true;
    }

    public String inference_(Bitmap bitmap)
    {
        cnt++;
        long s_preTime = System.currentTimeMillis();
        bitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB
        );
        IValue v = IValue.from(inputTensor);
        long e_preTime = System.currentTimeMillis();

        long s_modelTime = System.currentTimeMillis();
        final Tensor outputTensor = module.forward(v).toTensor();
        long e_modelTime = System.currentTimeMillis();
        float[] conf = outputTensor.getDataAsFloatArray();
        int labelIndex = getLabelIndex(conf);
        String label = ImageNetClasses.MODEL_CLASSES[labelIndex];
        float pre = (e_preTime- s_preTime) / 1000f;
        float modelTime = (e_modelTime - s_modelTime) / 1000f;
        avg_PreprocessTime += pre;
        avg_ModelTime += modelTime;
        return String.format("%s: %.2f%", label, conf[labelIndex]);
    }

    private int getLabelIndex(final float[] resultArray)
    {
        int ret = -1;
        float max_val = -99999f;
        for(int i = 0; i < resultArray.length; i++)
        {
            if (max_val < resultArray[i])
            {
                max_val = resultArray[i];
                ret = i;
            }
        }
        return ret;
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
