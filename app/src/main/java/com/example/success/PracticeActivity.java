package com.example.success;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import androidx.camera.view.PreviewView;

import com.google.common.util.concurrent.ListenableFuture;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PracticeActivity extends AppCompatActivity {
    // declare variables
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    PreviewView previewView;

    String gestureNameToPractice;
    int practiceNumber = 0;
    int requestCode = 101;
    String TAG = "VIDEO_RECORD_TAG";
    Intent intent;
    private VideoCapture videoCapture;
    String fileName = null;
    String file_path = null;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);

        // Map filename to actual name
        HashMap<String, String> gestureToName = new HashMap<>();
        gestureToName.put("h_0", "Num0");
        gestureToName.put("h_1", "Num1");
        gestureToName.put("h_2", "Num2");
        gestureToName.put("h_3", "Num3");
        gestureToName.put("h_4", "Num4");
        gestureToName.put("h_5", "Num5");
        gestureToName.put("h_6", "Num6");
        gestureToName.put("h_7", "Num7");
        gestureToName.put("h_8", "Num8");
        gestureToName.put("h_9", "Num9");
        gestureToName.put("h_lighton", "LightOn");
        gestureToName.put("h_lightoff", "LightOff");
        gestureToName.put("h_fanon", "FanOn");
        gestureToName.put("h_fanoff", "FanOff");
        gestureToName.put("h_increasefanspeed", "FanUp");
        gestureToName.put("h_decreasefanspeed", "FanDown");
        gestureToName.put("h_setthermo", "SetThermo");
        intent = getIntent();
        gestureNameToPractice = gestureToName.get(intent.getStringExtra("gesture_to_practice"));

        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        // start CameraX
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.i(TAG, "Error getting camera provider " + e);
            }

        }, getExecutor());

        // Buttons
        Button recordButton = findViewById(R.id.recordButton);
        Button stopRecord = findViewById(R.id.stopButton);
        recordButton.setOnClickListener(view -> recordVideo());
        stopRecord.setOnClickListener(view -> videoCapture.stopRecording());
    }

    Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @SuppressLint("RestrictedApi")
    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        videoCapture = new VideoCapture.Builder().build();

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);
    }

    @SuppressLint("RestrictedApi")
    public void recordVideo() {
        if (videoCapture != null) {
            Log.i(TAG, "Video capture starting...");
            practiceNumber++;
            fileName = gestureNameToPractice + "_PRACTICE_" + practiceNumber + "_case";
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");

            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, requestCode);
                }
                videoCapture.startRecording(
                        new VideoCapture.OutputFileOptions.Builder(
                                getContentResolver(),
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                        ).build(),
                        getExecutor(),
                        new VideoCapture.OnVideoSavedCallback() {
                            @Override
                            public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                                Log.i(TAG, "Success");
                                String filePath = getRealPathFromUri(outputFileResults.getSavedUri(), PracticeActivity.this);
                                Log.i(TAG,"Filepath is: " + filePath);
                                file_path = filePath;
                            }

                            @Override
                            public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                                Log.i(TAG, "Error " + message);
                            }
                        }
                );
            } catch (Exception e) {
                Log.i(TAG,"Exception during recording " + e);
            }
        }
    }

    // calls API to upload video
    public void uploadVideo(View view) {
        Log.i(TAG, "Begin upload video ...");
        try {
            run(file_path);

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.i(TAG, "Error occurred during upload " + e);
        }
    }

    public String getRealPathFromUri(Uri uri, Activity activity){
        String[] proj = { MediaStore.Images.Media.DATA };
        try (Cursor cursor = activity.getContentResolver().query(uri, proj, null, null, null)) {
            if (cursor == null) {
                return uri.getPath();
            } else {
                cursor.moveToFirst();
                int id = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                return cursor.getString(id);
            }
        }
    }

    public void run(String string_path) {
        Log.i(TAG, "Starting POST request to flask server...");
        // String pathPlusName = "/storage/self/primary/Movies/" + fileName + ".mp4";
        File file = new File(string_path);
        Log.i(TAG, "Filename before sending to backend is " + file);
        MediaType mediaType = MediaType.parse("video/mp4");

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        RequestBody requestBody=new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file",file.getName(),RequestBody.create(file, mediaType))
                .build();

        Request request = new Request.Builder()
                .url("http://10.0.0.132:5000/file-upload")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                call.cancel();
                runOnUiThread(() -> Log.i(TAG, "Post request failed " + e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    try {
                        String response_body = Objects.requireNonNull(response.body()).string();
                        System.out.println(response_body);
                        Log.i(TAG, "Response from server" + response_body);

                    } catch (IOException e) {
                        Log.e(TAG, "Problem with API call " + e);
                    }
                });
            }
        });
    }
}