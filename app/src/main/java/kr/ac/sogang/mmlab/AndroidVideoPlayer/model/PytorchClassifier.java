package kr.ac.sogang.mmlab.AndroidVideoPlayer.model;

import android.graphics.Bitmap;
import android.os.Environment;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import kr.ac.sogang.R;
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

    public float[] inferenceFloat(Bitmap bitmap)
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
        float pre = (e_preTime- s_preTime) / 1000f;
        float modelTime = (e_modelTime - s_modelTime) / 1000f;
        Logging.logE("FFMPEG - CNN inference time: " + modelTime);
        avg_PreprocessTime += pre;
        avg_ModelTime += modelTime;
        return outputTensor.getDataAsFloatArray(); //len 62720
    }

    public float[] inferenceTensorfile(String fileName)
    {
        long s_preTime = System.currentTimeMillis();
        float[] fTensor = null;

        try {
            String strTensor = getStringFromFile(fileName).replace("[", "").replace("]", "");
            Logging.logE("PytorchClassifier - " + strTensor);

            String[] parts = strTensor.split(",");
            fTensor = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                float number = Float.parseFloat(parts[i]);
                fTensor[i] = number;
            }
        } catch (Exception e) {
            Logging.logE("PytorchClassifier - " + "fail");
            e.printStackTrace();
        }
        long[] shape = new long[]{1,3,224,224};

        Tensor inputTensor = Tensor.fromBlob(fTensor, shape);

        IValue v = IValue.from(inputTensor);
        long e_preTime = System.currentTimeMillis();

        long s_modelTime = System.currentTimeMillis();
        final Tensor outputTensor = module.forward(v).toTensor();
        long e_modelTime = System.currentTimeMillis();
        float pre = (e_preTime- s_preTime) / 1000f;
        float modelTime = (e_modelTime - s_modelTime) / 1000f;
        avg_PreprocessTime += pre;
        avg_ModelTime += modelTime;
        return outputTensor.getDataAsFloatArray(); //len 62720
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

    public byte[] bitmapToByteArray( Bitmap $bitmap ) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream() ;
        $bitmap.compress( Bitmap.CompressFormat.JPEG, 100, stream) ;
        byte[] byteArray = stream.toByteArray() ;
        return byteArray ;
    }

    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile (String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }
}
