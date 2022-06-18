package kr.ac.sogang.mmlab.AndroidVideoPlayer.util;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;


public class Requests {
    private String TAG = "Requests";
    public JSONObject searchImage(String strUrl, final int[] modules, final String filePath, final int topk) {
        try {
            @SuppressLint("StaticFieldLeak")
            AsyncTask<String, Void, HttpResponse> asyncTask = new AsyncTask<String, Void, HttpResponse>() {
                @Override
                protected HttpResponse doInBackground(String... url) {
                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    builder.addPart("image", new FileBody(new File(filePath)));
                    try {
                        builder.addPart("engine", new StringBody(Integer.toString(modules[0])));
                        builder.addPart("dataset", new StringBody(Integer.toString(modules[1])));
                        builder.addPart("extractor", new StringBody(Integer.toString(modules[2])));
                        builder.addPart("topk",  new StringBody(Integer.toString(topk)));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    HttpClient httpClient = AndroidHttpClient.newInstance("Android");
                    HttpPost httpPost = new HttpPost(url[0]);
                    httpPost.setEntity(builder.build());
                    try {
                        HttpResponse httpResponse = httpClient.execute(httpPost);

                        return httpResponse;
                    } catch(NetworkOnMainThreadException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Request fail");
                        return null;
                    }catch(IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Request fail");
                        return null;
                    }
                }
            };
            InputStream inputStream = null;

            HttpResponse response = asyncTask.execute(strUrl).get();
            HttpEntity httpEntity = response.getEntity();
            inputStream = httpEntity.getContent();
            BufferedReader bufferdReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;

            while ((line = bufferdReader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            inputStream.close();
            JSONObject jsonResponse = new JSONObject(stringBuilder.toString());

            return jsonResponse;
        } catch(Exception e) {
            return null;
        }
    }

    public JSONObject searchVideo(String serverUrl, final String filePath, final int topk, final int window, final double score_threshold, final int match_threshold) {
        try {
            @SuppressLint("StaticFieldLeak")
            AsyncTask<String, Void, HttpResponse> asyncTask = new AsyncTask<String, Void, HttpResponse>() {
                @Override
                protected HttpResponse doInBackground(String... url) {
                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    builder.addPart("video", new FileBody(new File(filePath)));
                    try {
                        builder.addPart("topk", new StringBody(Integer.toString(topk)));
                        builder.addPart("window", new StringBody(Integer.toString(window)));
                        builder.addPart("score_threshold", new StringBody(Double.toString(score_threshold)));
                        builder.addPart("match_threshold",  new StringBody(Integer.toString(match_threshold)));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    HttpClient httpClient = AndroidHttpClient.newInstance("Android");
                    HttpPost httpPost = new HttpPost(url[0]);
                    httpPost.setEntity(builder.build());
                    try {
                        HttpResponse httpResponse = httpClient.execute(httpPost);

                        return httpResponse;
                    } catch(NetworkOnMainThreadException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Request fail");
                        return null;
                    }catch(IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Request fail");
                        return null;
                    }
                }
            };
            InputStream inputStream = null;

            HttpResponse response = asyncTask.execute(serverUrl).get();
            HttpEntity httpEntity = response.getEntity();
            inputStream = httpEntity.getContent();
            BufferedReader bufferdReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;

            while ((line = bufferdReader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            inputStream.close();
            JSONObject jsonResponse = new JSONObject(stringBuilder.toString());

            return jsonResponse;
        } catch(Exception e) {
            return null;
        }
    }

    public JSONObject searchFeature(String strUrl, final String filePath, final int topk) {
        try {
            @SuppressLint("StaticFieldLeak")
            AsyncTask<String, Void, HttpResponse> asyncTask = new AsyncTask<String, Void, HttpResponse>() {
                @Override
                protected HttpResponse doInBackground(String... url) {
                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    builder.addPart("feature", new FileBody(new File(filePath)));
                    try {
                        builder.addPart("topk",  new StringBody(Integer.toString(topk)));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    HttpClient httpClient = AndroidHttpClient.newInstance("Android");
                    HttpPost httpPost = new HttpPost(url[0]);
                    httpPost.setEntity(builder.build());
                    try {
                        HttpResponse httpResponse = httpClient.execute(httpPost);

                        return httpResponse;
                    } catch(NetworkOnMainThreadException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Request fail");
                        return null;
                    }catch(IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Request fail");
                        return null;
                    }
                }
            };
            InputStream inputStream = null;

            HttpResponse response = asyncTask.execute(strUrl).get();
            HttpEntity httpEntity = response.getEntity();
            inputStream = httpEntity.getContent();
            BufferedReader bufferdReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;

            while ((line = bufferdReader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            inputStream.close();
            JSONObject jsonResponse = new JSONObject(stringBuilder.toString());

            return jsonResponse;
        } catch(Exception e) {
            return null;
        }
    }

    public JSONObject searchFeatureUpdate(String strUrl, final String filePath, final int id) {
        try {
            @SuppressLint("StaticFieldLeak")
            AsyncTask<String, Void, HttpResponse> asyncTask = new AsyncTask<String, Void, HttpResponse>() {
                @Override
                protected HttpResponse doInBackground(String... url) {
                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    builder.addPart("feature", new FileBody(new File(filePath)));

                    HttpClient httpClient = AndroidHttpClient.newInstance("Android");
                    HttpPut httpPut = new HttpPut(url[0] + id +  "/stream/");
                    httpPut.setEntity(builder.build());
                    try {
                        HttpResponse httpResponse = httpClient.execute(httpPut);

                        return httpResponse;
                    } catch(NetworkOnMainThreadException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Request fail");
                        return null;
                    }catch(IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Request fail");
                        return null;
                    }
                }
            };
            InputStream inputStream = null;

            HttpResponse response = asyncTask.execute(strUrl).get();
            HttpEntity httpEntity = response.getEntity();
            inputStream = httpEntity.getContent();
            BufferedReader bufferdReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;

            while ((line = bufferdReader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            inputStream.close();
            JSONObject jsonResponse = new JSONObject(stringBuilder.toString());

            return jsonResponse;
        } catch(Exception e) {
            return null;
        }
    }

    public String getResults(JSONObject result) {
        try {
            @SuppressLint("StaticFieldLeak")
            AsyncTask<String, Void, String> asyncTask = new AsyncTask<String, Void, String>() {
                @Override
                protected String doInBackground(String... url) {
                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

                    HttpClient httpClient = AndroidHttpClient.newInstance("Android");
                    HttpGet httpPost = new HttpGet(url[0]);
                    try {
                        HttpResponse httpResponse = httpClient.execute(httpPost);

                        InputStream inputStream = httpResponse.getEntity().getContent();

                        BufferedReader bufferdReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

                        StringBuilder stringBuilder = new StringBuilder();
                        String line = null;

                        while ((line = bufferdReader.readLine()) != null) {
                            stringBuilder.append(line + "\n");
                        }
                        inputStream.close();

                        return stringBuilder.toString();
                    } catch(NetworkOnMainThreadException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Request fail");
                        return null;
                    }catch(IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Request fail");
                        return null;
                    }
                }
            };
            InputStream inputStream = null;

            return  asyncTask.execute(result.getString("results")).get();
        } catch(Exception e) {
            return null;
        }
    }
    public String getStringValue(JSONObject result, String key) {
        try {
            return result.getString(key);
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }
    public Bitmap getBitmapFromAsolutePath(String asolutePath) {
        try {
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            return BitmapFactory.decodeFile(asolutePath, bmOptions);
        } catch(Exception e) {
            return null;
        }
    }

}
