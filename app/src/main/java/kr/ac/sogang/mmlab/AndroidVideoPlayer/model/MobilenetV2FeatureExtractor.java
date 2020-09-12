package kr.ac.sogang.mmlab.AndroidVideoPlayer.model;

import android.graphics.Bitmap;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import kr.ac.sogang.mmlab.AndroidVideoPlayer.filter.BaseModel;

public class MobilenetV2FeatureExtractor extends BaseModel {
    private Module module = null;
    int inputWidth = 224;
    int inputHeight = 224;

    public MobilenetV2FeatureExtractor(String modulePath)
    {
        module = Module.load(modulePath);
    }

    public boolean inference(Bitmap bitmap)
    {
        bitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB
        );
        IValue v = IValue.from(inputTensor);
        final Tensor outputTensor = module.forward(v).toTensor();

        return true;
    }

    public float[] inference_(Bitmap bitmap)
    {
        bitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB
        );
        IValue v = IValue.from(inputTensor);

        final Tensor outputTensor = module.forward(v).toTensor();
        return outputTensor.getDataAsFloatArray(); //len 62720
    }
}
