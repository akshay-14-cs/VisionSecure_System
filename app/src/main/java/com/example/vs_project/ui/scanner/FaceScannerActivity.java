package com.example.vs_project.ui.scanner;

import android.Manifest;
import android.util.Log;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.vs_project.databinding.ActivityFaceScannerBinding;
import com.example.vs_project.utils.CameraUtils;
import com.example.vs_project.utils.TTSManager;
import com.example.vs_project.utils.VerificationUtils;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.content.SharedPreferences;
import java.io.File;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.vs_project.network.ApiService;
import com.example.vs_project.network.RetrofitClient;
import android.view.View;

public class FaceScannerActivity extends AppCompatActivity {

    private ActivityFaceScannerBinding binding;
    private CameraUtils cameraUtils;
    private TTSManager ttsManager;
    private String scannerMode = "VERIFICATION"; 
    private String orgId = "";
    private String userId = ""; 
    
    
    private String selectedDate = "";
    private String selectedTiming = "";
    private String selectedRoom = "";
    private String selectedSubject = "";
    
    
    private String sessionId = "";

    private com.example.vs_project.utils.NetworkReceiver networkReceiver;
    private boolean isFrontCamera = true;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean cameraGranted = result.getOrDefault(Manifest.permission.CAMERA, false);
                Boolean locationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);

                if (cameraGranted && locationGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Permissions (Camera, Location) are required for scanning", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFaceScannerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        cameraUtils = new CameraUtils();
        ttsManager = new TTSManager(this);

        if (getIntent() != null) {
            if (getIntent().hasExtra("SCANNER_MODE")) scannerMode = getIntent().getStringExtra("SCANNER_MODE");
            if (getIntent().hasExtra("ORG_ID")) orgId = getIntent().getStringExtra("ORG_ID");
            if (getIntent().hasExtra("USER_ID")) userId = getIntent().getStringExtra("USER_ID");
            if (getIntent().hasExtra("SELECTED_DATE")) selectedDate = getIntent().getStringExtra("SELECTED_DATE");
            if (getIntent().hasExtra("SELECTED_TIMING")) selectedTiming = getIntent().getStringExtra("SELECTED_TIMING");
            if (getIntent().hasExtra("SELECTED_ROOM")) selectedRoom = getIntent().getStringExtra("SELECTED_ROOM");
            if (getIntent().hasExtra("SELECTED_SUBJECT")) selectedSubject = getIntent().getStringExtra("SELECTED_SUBJECT");
            if (getIntent().hasExtra("SESSION_ID")) sessionId = getIntent().getStringExtra("SESSION_ID");
        }
        
        binding.tvStatus.setText(scannerMode.equals("REGISTRATION") ? "Move your head slowly to\nregister your Face ID." : "Look straight to verify\nyour Face ID.");

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
        
        networkReceiver = new com.example.vs_project.utils.NetworkReceiver();

        binding.btnCancel.setOnClickListener(v -> finish());
        
        binding.btnFlipCamera.setOnClickListener(v -> {
            isFrontCamera = !isFrontCamera;
            startCamera();
        });
        
        setupScanButton();
    }
    
    private void setupScanButton() {
        binding.btnAction.setText("Scan Face");
        binding.btnAction.setEnabled(true);
        binding.progressRing.setIndeterminate(false);
        binding.progressRing.setProgress(0);
        
        binding.btnAction.setOnClickListener(v -> {
            binding.btnAction.setEnabled(false);
            binding.btnAction.setText("Scanning...");
            binding.progressRing.setIndeterminate(true);
            binding.tvStatus.setText("Processing scan...");
            
            cameraUtils.takePhoto(this, ContextCompat.getMainExecutor(this), new CameraUtils.CaptureCallback() {
                @Override
                public void onSuccess(File file) {
                    processScan(file);
                }

                @Override
                public void onError(Exception exc) {
                    binding.progressRing.setIndeterminate(false);
                    Toast.makeText(FaceScannerActivity.this, "Photo capture failed", Toast.LENGTH_SHORT).show();
                    binding.tvStatus.setText("Capture Failed. Try Again.");
                    setupScanButton();
                }
            });
        });
    }

    private void showSuccessUI(String message, String details) {
        binding.progressRing.setIndeterminate(false);
        binding.progressRing.setProgressCompat(100, true);
        binding.viewFinder.setAlpha(0.2f);
        binding.ivSuccessCheck.setVisibility(View.VISIBLE);
        
        binding.tvStatus.setText(message);
        if (details != null && !details.isEmpty()) {
            binding.tvSuccessDetails.setText(details);
            binding.tvSuccessDetails.setVisibility(View.VISIBLE);
        }
        
        binding.btnAction.setEnabled(true);
        binding.btnAction.setText("Continue");
        binding.btnAction.setOnClickListener(v -> finish());
    }
    
    private void showErrorUI(String error, File photoFile) {
        binding.progressRing.setIndeterminate(false);
        binding.progressRing.setProgress(0);
        binding.tvStatus.setText(error);
        
        binding.btnAction.setEnabled(true);
        binding.btnAction.setText("Retake Scan");
        binding.btnAction.setOnClickListener(v -> setupScanButton());
        
        if (photoFile != null && photoFile.exists()) {
            try {
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                
                try {
                    android.media.ExifInterface exif = new android.media.ExifInterface(photoFile.getAbsolutePath());
                    int orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_UNDEFINED);
                    android.graphics.Matrix matrix = new android.graphics.Matrix();
                    switch (orientation) {
                        case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                            matrix.postRotate(90);
                            break;
                        case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                            matrix.postRotate(180);
                            break;
                        case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                            matrix.postRotate(270);
                            break;
                    }
                    bitmap = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                android.widget.ImageView iv = new android.widget.ImageView(this);
                iv.setImageBitmap(bitmap);
                iv.setAdjustViewBounds(true);
                iv.setPadding(32, 32, 32, 32);

                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Intruder Detected!")
                        .setMessage("This face was not recognized in the system. The incident and photo have been securely logged to the Admin.")
                        .setView(iv)
                        .setPositiveButton("Dismiss", null)
                        .show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showErrorUI(String error) {
        showErrorUI(error, null);
    }

    private void startCamera() {
        cameraUtils.startCamera(this, this, binding.viewFinder, isFrontCamera);
    }

    private void processScan(File photoFile) {
        if (scannerMode.equals("REGISTRATION")) {
            uploadFaceForRegistration(photoFile);
        } else if (scannerMode.equals("VERIFICATION_ONLINE")) {
            uploadFaceForVerificationOnline(photoFile);
        } else {
            
            VerificationUtils.verifyOnCampus(this, orgId, new VerificationUtils.VerificationCallback() {
                @Override
                public void onSuccess() {
                    uploadFaceForVerificationOffline(photoFile);
                }

                @Override
                public void onFailure(String reason) {
                    ttsManager.speak("Verification failed. " + reason);
                    showErrorUI("Verification Failed: " + reason);
                }
            });
        }
    }

    private void uploadFaceForRegistration(File photoFile) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        RequestBody reqOrgId = RequestBody.create(MediaType.parse("text/plain"), orgId);
        RequestBody reqUserId = RequestBody.create(MediaType.parse("text/plain"), userId);
        
        SharedPreferences prefs = getSharedPreferences("VisionSecureSession", MODE_PRIVATE);
        String inchargeId = prefs.getString("USER_ID", "unknown");
        RequestBody reqInchargeId = RequestBody.create(MediaType.parse("text/plain"), inchargeId);
        
        RequestBody reqFile = RequestBody.create(MediaType.parse("image/jpeg"), photoFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("faceImage", photoFile.getName(), reqFile);

        apiService.registerFace(reqOrgId, reqUserId, reqInchargeId, body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    ttsManager.speak("Face registered successfully.");
                    showSuccessUI("First Face ID scan complete.", "Face successfully linked to " + userId);
                } else {
                    String errorMsg = "Duplicate face or error.";
                    try {
                        if (response.errorBody() != null) {
                            String errorJson = response.errorBody().string();
                            org.json.JSONObject jObjError = new org.json.JSONObject(errorJson);
                            errorMsg = jObjError.getString("detail");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ttsManager.speak("Registration failed.");
                    showErrorUI(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                ttsManager.speak("Network error.");
                showErrorUI("Error: " + t.getMessage());
            }
        });
    }

    private void uploadFaceForVerificationOffline(File photoFile) {
        SharedPreferences prefs = getSharedPreferences("VisionSecureSession", MODE_PRIVATE);
        String invigilatorId = prefs.getString("USER_ID", "unknown");

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        RequestBody reqOrgId = RequestBody.create(MediaType.parse("text/plain"), orgId);
        RequestBody reqStudentId = RequestBody.create(MediaType.parse("text/plain"), ""); 
        RequestBody reqDate = RequestBody.create(MediaType.parse("text/plain"), selectedDate);
        RequestBody reqTiming = RequestBody.create(MediaType.parse("text/plain"), selectedTiming);
        RequestBody reqClassroom = RequestBody.create(MediaType.parse("text/plain"), selectedRoom);
        RequestBody reqInvigilator = RequestBody.create(MediaType.parse("text/plain"), invigilatorId);
        
        RequestBody reqFile = RequestBody.create(MediaType.parse("image/jpeg"), photoFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("faceImage", photoFile.getName(), reqFile);

        apiService.verifyFaceOffline(reqOrgId, reqStudentId, reqDate, reqTiming, reqClassroom, reqInvigilator, body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    String successMsg = "Student Marked Present.";
                    try {
                        if (response.body() != null) {
                            String successJson = response.body().string();
                            org.json.JSONObject jObjSuccess = new org.json.JSONObject(successJson);
                            successMsg = jObjSuccess.getString("message");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ttsManager.speak("Verification successful.");
                    showSuccessUI("Face ID scan complete.", successMsg);
                } else {
                    String errorMsg = "ACCESS DENIED\nUnrecognized Face Detected!";
                    try {
                        if (response.errorBody() != null) {
                            String errorJson = response.errorBody().string();
                            org.json.JSONObject jObjError = new org.json.JSONObject(errorJson);
                            errorMsg = "ACCESS DENIED\n" + jObjError.getString("detail");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    ttsManager.speak("Face not recognized. Incident logged.");
                    showErrorUI(errorMsg, photoFile);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                ttsManager.speak("Network error.");
                showErrorUI("Verification Failed: Network Error");
            }
        });
    }

    private void uploadFaceForVerificationOnline(File photoFile) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        RequestBody reqOrgId = RequestBody.create(MediaType.parse("text/plain"), orgId);
        RequestBody reqSessionId = RequestBody.create(MediaType.parse("text/plain"), sessionId);
        
        RequestBody reqFile = RequestBody.create(MediaType.parse("image/jpeg"), photoFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("faceImage", photoFile.getName(), reqFile);

        apiService.verifyFaceOnline(reqOrgId, reqSessionId, body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    ttsManager.speak("Verification successful. Session unlocked.");
                    showSuccessUI("Face ID scan complete.", "Session Unlocked.");
                } else {
                    ttsManager.speak("Face not recognized.");
                    showErrorUI("ACCESS DENIED\nUnrecognized Face Detected!", photoFile);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                ttsManager.speak("Network error.");
                showErrorUI("Verification Failed: Network Error");
            }
        });
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scannerMode.startsWith("VERIFICATION")) {
            registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (scannerMode.startsWith("VERIFICATION")) {
            try {
                unregisterReceiver(networkReceiver);
            } catch (Exception e) {
                
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
    }
}
