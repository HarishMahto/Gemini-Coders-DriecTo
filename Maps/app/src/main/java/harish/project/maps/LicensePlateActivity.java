package harish.project.maps;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.content.Context;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import harish.project.maps.utils.PermissionUtils;

public class LicensePlateActivity extends BaseActivity {
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[] {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private PreviewView viewFinder;
    private ImageView capturedImageView;
    private Button captureButton;
    private Button saveButton;
    private Button uploadButton;
    private Button testVertexAIButton;
    private TextView plateNumberTextView;
    private TextView statusTextView;
    private ChipGroup violationChipGroup;
    private TextView locationTextView;
    private final String[] violationTypes = { "Helmet", "Triple Seat", "Wrong Parking", "Using Mobile", "Lane Changing",
            "Seat Belt", "FootPath Driving", "Driving in Center" };

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private LicensePlateViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license_plate);

        // Initialize views
        viewFinder = findViewById(R.id.viewFinder);
        capturedImageView = findViewById(R.id.capturedImageView);
        captureButton = findViewById(R.id.captureButton);
        saveButton = findViewById(R.id.saveButton);
        uploadButton = findViewById(R.id.uploadButton);
        testVertexAIButton = findViewById(R.id.testVertexAIButton);
        Button debugButton = findViewById(R.id.debugButton);
        plateNumberTextView = findViewById(R.id.plateNumberTextView);
        statusTextView = findViewById(R.id.statusTextView);
        violationChipGroup = findViewById(R.id.violationChipGroup);
        locationTextView = findViewById(R.id.locationTextView);

        // Populate violation chips
        displayViolationChips();

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(LicensePlateViewModel.class);

        // Set up observers
        viewModel.getRecognizedText().observe(this, text -> {
            plateNumberTextView.setText(text);
            boolean validPlate = !text.startsWith("Error") && !text.equals("No license plate detected");
            saveButton.setEnabled(validPlate);
            uploadButton.setEnabled(validPlate && viewModel.getViolationType().getValue() != null);
        });

        viewModel.getCapturedImageUri().observe(this, uri -> {
            if (uri != null) {
                viewFinder.setVisibility(android.view.View.GONE);
                capturedImageView.setVisibility(android.view.View.VISIBLE);
                Glide.with(this).load(uri).into(capturedImageView);
                viewModel.processImage(uri);
            } else {
                viewFinder.setVisibility(android.view.View.VISIBLE);
                capturedImageView.setVisibility(android.view.View.GONE);
            }
        });

        viewModel.getUploadStatus().observe(this, status -> {
            statusTextView.setText(status);
            if (status.equals("Success")) {
                Toast.makeText(this, "License plate saved successfully", Toast.LENGTH_SHORT).show();
                // Reset UI for next capture
                viewModel.setCapturedImageUri(null);
                plateNumberTextView.setText("Plate Number Will Appear Here");
                saveButton.setEnabled(false);
                uploadButton.setEnabled(false);
                statusTextView.setText("Ready to capture");
            } else if (status.startsWith("Error")) {
                Toast.makeText(this, status, Toast.LENGTH_LONG).show();
            }
        });

        // Observe violation type selection
        viewModel.getViolationType().observe(this, type -> {
            for (int i = 0; i < violationChipGroup.getChildCount(); i++) {
                Chip chip = (Chip) violationChipGroup.getChildAt(i);
                chip.setChecked(chip.getText().toString().equals(type));
            }
            // Enable upload if plate is valid and violation selected
            String plate = viewModel.getRecognizedText().getValue();
            boolean validPlate = plate != null && !plate.startsWith("Error")
                    && !plate.equals("No license plate detected");
            uploadButton.setEnabled(validPlate && type != null);
        });

        // Chip selection listener
        violationChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) {
                Chip selectedChip = group.findViewById(checkedId);
                if (selectedChip != null) {
                    viewModel.setViolationType(selectedChip.getText().toString());
                }
            }
        });

        // Fetch and display location
        fetchAndDisplayLocation();

        // Set up button click listeners
        captureButton.setOnClickListener(v -> {
            if (viewFinder.getVisibility() == android.view.View.VISIBLE) {
                takePhoto();
            } else {
                // Reset to camera view
                viewModel.setCapturedImageUri(null);
            }
        });

        saveButton.setOnClickListener(v -> viewModel.saveLicensePlate());
        uploadButton.setOnClickListener(v -> uploadViolationData());
        testVertexAIButton.setOnClickListener(v -> {
            if (PermissionUtils.hasFilePermissions(this)) {
                Intent intent = new Intent(LicensePlateActivity.this, VertexAITestActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(LicensePlateActivity.this, PermissionRequestActivity.class);
                intent.putExtra("target_activity", "harish.project.maps.VertexAITestActivity");
                startActivity(intent);
            }
        });

        debugButton.setOnClickListener(v -> {
            Intent intent = new Intent(LicensePlateActivity.this, DebugActivity.class);
            startActivity(intent);
        });

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void takePhoto() {
        if (imageCapture == null)
            return;

        // Create timestamped output file
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_" + timestamp);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        // Create output options object
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();

        // Capture the image
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = outputFileResults.getSavedUri();
                        if (savedUri != null) {
                            viewModel.setCapturedImageUri(savedUri);
                            statusTextView.setText("Processing image...");
                        } else {
                            statusTextView.setText("Error: Could not save image");
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        statusTextView.setText("Error: " + exception.getMessage());
                    }
                });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Set up the preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // Set up the capture use case
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // Select back camera
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Unbind any bound use cases before rebinding
                cameraProvider.unbindAll();

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                // Handle any errors
                statusTextView.setText("Error starting camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            startCamera();
            // if (allPermissionsGranted()) {
            // startCamera();
            // } else {
            // Toast.makeText(this, "Permissions not granted by the user.",
            // Toast.LENGTH_SHORT).show();
            // finish();
            // }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private void displayViolationChips() {
        violationChipGroup.removeAllViews();
        for (String type : violationTypes) {
            Chip chip = new Chip(this);
            chip.setText(type);
            chip.setCheckable(true);
            violationChipGroup.addView(chip);
        }
    }

    private void fetchAndDisplayLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        String locStr = location.getLatitude() + ", " + location.getLongitude();
                        locationTextView.setText("Location: " + locStr);
                        viewModel.setLocation(locStr);
                    }
                }, null);
            } else {
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 101);
            }
        } catch (Exception e) {
            locationTextView.setText("Location: Unknown");
            viewModel.setLocation(null);
        }
    }

    private void uploadViolationData() {
        String plate = viewModel.getRecognizedText().getValue();
        String violation = viewModel.getViolationType().getValue();
        Uri imageUri = viewModel.getCapturedImageUri().getValue();
        String location = locationTextView.getText().toString();
        long timestamp = System.currentTimeMillis();
        if (plate == null || violation == null || imageUri == null) {
            Toast.makeText(this, "Missing data for upload", Toast.LENGTH_SHORT).show();
            return;
        }
        // Upload image to Firebase Storage, then save metadata to Firestore/Realtime DB
        // (Pseudo code, you may need to adjust for your Firebase setup)
        com.google.firebase.storage.FirebaseStorage storage = com.google.firebase.storage.FirebaseStorage.getInstance();
        String fileName = "violations/" + plate + "_" + timestamp + ".jpg";
        com.google.firebase.storage.StorageReference ref = storage.getReference().child(fileName);
        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                    // Save metadata to Firestore
                    java.util.HashMap<String, Object> data = new java.util.HashMap<>();
                    data.put("plateNumber", plate);
                    data.put("violationType", violation);
                    data.put("imageUrl", downloadUrl.toString());
                    data.put("timestamp", timestamp);
                    data.put("location", location);
                    data.put("status", "Pending");
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("violations")
                            .add(data)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "Uploaded successfully", Toast.LENGTH_SHORT).show();
                                // Optionally reset UI or navigate to history
                            })
                            .addOnFailureListener(e -> Toast
                                    .makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }))
                .addOnFailureListener(
                        e -> Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}