package harish.project.maps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;

import harish.project.maps.services.VertexAIService;
import harish.project.maps.utils.PermissionUtils;

public class VertexAITestActivity extends AppCompatActivity {
  private static final String TAG = "VertexAITestActivity";
  private static final int REQUEST_IMAGE_PICK = 1001;
  private static final int REQUEST_CAMERA_PERMISSION = 1002;

  private ImageView testImageView;
  private TextView resultTextView;
  private Button selectImageButton;
  private Button testApiKeyButton;
  private Button testBackendButton;
  private Button backButton;

  private LicensePlateViewModel viewModel;
  private VertexAIService vertexAIService;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_vertex_ai_test);

    // Initialize views
    testImageView = findViewById(R.id.testImageView);
    resultTextView = findViewById(R.id.resultTextView);
    selectImageButton = findViewById(R.id.selectImageButton);
    testApiKeyButton = findViewById(R.id.testApiKeyButton);
    testBackendButton = findViewById(R.id.testBackendButton);
    backButton = findViewById(R.id.backButton);

    // Initialize services
    viewModel = new ViewModelProvider(this).get(LicensePlateViewModel.class);
    vertexAIService = new VertexAIService();

    // Set up button listeners
    selectImageButton.setOnClickListener(v -> selectImage());
    testApiKeyButton.setOnClickListener(v -> testWithApiKey());
    testBackendButton.setOnClickListener(v -> testWithBackend());
    backButton.setOnClickListener(v -> finish());

    // Set up observers
    viewModel.getRecognizedText().observe(this, text -> {
      resultTextView.setText("Result: " + text);
      Log.d(TAG, "License plate result: " + text);
    });
  }

  private void selectImage() {
    if (!PermissionUtils.hasFilePermissions(this)) {
      PermissionUtils.requestFilePermissions(this);
      return;
    }

    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    startActivityForResult(intent, REQUEST_IMAGE_PICK);
  }

  private void testWithApiKey() {
    if (viewModel.getCapturedImageUri().getValue() == null) {
      Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
      return;
    }

    resultTextView.setText("Testing with API Key...");
    Toast.makeText(this, "Testing with API Key method", Toast.LENGTH_SHORT).show();

    vertexAIService.recognizeLicensePlateWithApiKey(
        this,
        viewModel.getCapturedImageUri().getValue(),
        new VertexAIService.LicensePlateCallback() {
          @Override
          public void onSuccess(String licensePlate) {
            runOnUiThread(() -> {
              resultTextView.setText("API Key Result: " + licensePlate);
              Toast.makeText(VertexAITestActivity.this,
                  "Success: " + licensePlate, Toast.LENGTH_LONG).show();
            });
          }

          @Override
          public void onError(String error) {
            runOnUiThread(() -> {
              resultTextView.setText("API Key Error: " + error);
              Toast.makeText(VertexAITestActivity.this,
                  "Error: " + error, Toast.LENGTH_LONG).show();
            });
          }
        });
  }

  private void testWithBackend() {
    if (viewModel.getCapturedImageUri().getValue() == null) {
      Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
      return;
    }

    resultTextView.setText("Testing with Backend...");
    Toast.makeText(this, "Testing with Backend method", Toast.LENGTH_SHORT).show();

    String accessToken = TokenFetcher.fetchAccessToken();
    if (accessToken == null) {
      resultTextView.setText("Backend Error: Could not fetch access token");
      Toast.makeText(this, "Could not fetch access token from backend", Toast.LENGTH_LONG).show();
      return;
    }

    vertexAIService.recognizeLicensePlate(
        this,
        viewModel.getCapturedImageUri().getValue(),
        accessToken,
        new VertexAIService.LicensePlateCallback() {
          @Override
          public void onSuccess(String licensePlate) {
            runOnUiThread(() -> {
              resultTextView.setText("Backend Result: " + licensePlate);
              Toast.makeText(VertexAITestActivity.this,
                  "Success: " + licensePlate, Toast.LENGTH_LONG).show();
            });
          }

          @Override
          public void onError(String error) {
            runOnUiThread(() -> {
              resultTextView.setText("Backend Error: " + error);
              Toast.makeText(VertexAITestActivity.this,
                  "Error: " + error, Toast.LENGTH_LONG).show();
            });
          }
        });
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (PermissionUtils.handlePermissionResult(this, requestCode, permissions, grantResults)) {
      // Permission granted, try to select image again
      selectImage();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == PermissionUtils.REQUEST_MANAGE_STORAGE) {
      if (PermissionUtils.hasFilePermissions(this)) {
        selectImage();
      } else {
        Toast.makeText(this, "Storage permission required to select images", Toast.LENGTH_LONG).show();
      }
    } else if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
      Uri selectedImageUri = data.getData();
      if (selectedImageUri != null) {
        viewModel.setCapturedImageUri(selectedImageUri);
        Glide.with(this).load(selectedImageUri).into(testImageView);
        resultTextView.setText("Image selected. Ready to test.");
      }
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (vertexAIService != null) {
      vertexAIService.shutdown();
    }
  }
}