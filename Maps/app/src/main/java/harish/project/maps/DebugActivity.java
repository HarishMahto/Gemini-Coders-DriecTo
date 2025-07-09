package harish.project.maps;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import harish.project.maps.services.VertexAIService;
import harish.project.maps.utils.PermissionUtils;

public class DebugActivity extends AppCompatActivity {
  private static final String TAG = "DebugActivity";
  private static final int REQUEST_IMAGE_PICK = 1001;

  private TextView debugTextView;
  private Button testApiKeyButton;
  private Button selectImageButton;
  private Button testSimpleButton;
  private Uri selectedImageUri;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_debug);

    debugTextView = findViewById(R.id.debugTextView);
    testApiKeyButton = findViewById(R.id.testApiKeyButton);
    selectImageButton = findViewById(R.id.selectImageButton);
    testSimpleButton = findViewById(R.id.testSimpleButton);

    // Check API key configuration
    checkApiKeyConfiguration();

    selectImageButton.setOnClickListener(v -> selectImage());
    testApiKeyButton.setOnClickListener(v -> testApiKey());
    testSimpleButton.setOnClickListener(v -> testSimpleRequest());
  }

  private void checkApiKeyConfiguration() {
    String apiKey = Config.GOOGLE_CLOUD_API_KEY;
    String status = "API Key Status:\n";

    if (apiKey == null) {
      status += "❌ API Key is NULL\n";
    } else if (apiKey.equals("YOUR_ACTUAL_API_KEY_HERE")) {
      status += "❌ API Key not configured (using placeholder)\n";
    } else if (apiKey.length() < 20) {
      status += "❌ API Key seems too short: " + apiKey + "\n";
    } else {
      status += "✅ API Key configured (length: " + apiKey.length() + ")\n";
      status += "   Key starts with: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "...\n";
    }

    status += "\nProject ID: " + Config.VERTEX_AI_PROJECT_ID + "\n";
    status += "Backend URL: " + Config.BACKEND_URL + "\n";

    debugTextView.setText(status);
  }

  private void selectImage() {
    if (!PermissionUtils.hasFilePermissions(this)) {
      PermissionUtils.requestFilePermissions(this);
      return;
    }

    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    startActivityForResult(intent, REQUEST_IMAGE_PICK);
  }

  private void testApiKey() {
    if (selectedImageUri == null) {
      Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
      return;
    }

    debugTextView.append("\n\nTesting API Key...\n");

    VertexAIService service = new VertexAIService();
    service.recognizeLicensePlateWithApiKey(this, selectedImageUri, new VertexAIService.LicensePlateCallback() {
      @Override
      public void onSuccess(String licensePlate) {
        runOnUiThread(() -> {
          debugTextView.append("✅ Success: " + licensePlate + "\n");
        });
      }

      @Override
      public void onError(String error) {
        runOnUiThread(() -> {
          debugTextView.append("❌ Error: " + error + "\n");
        });
      }
    });
  }

  private void testSimpleRequest() {
    debugTextView.append("\n\nTesting simple API request...\n");

    // Test with a simple image (you can replace this with a test image)
    String testImageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="; // 1x1
                                                                                                                                 // pixel

    VertexAIService service = new VertexAIService();
    service.recognizeLicensePlateWithApiKey(this, null, new VertexAIService.LicensePlateCallback() {
      @Override
      public void onSuccess(String licensePlate) {
        runOnUiThread(() -> {
          debugTextView.append("✅ Simple test success: " + licensePlate + "\n");
        });
      }

      @Override
      public void onError(String error) {
        runOnUiThread(() -> {
          debugTextView.append("❌ Simple test error: " + error + "\n");
        });
      }
    });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
      selectedImageUri = data.getData();
      if (selectedImageUri != null) {
        debugTextView.append("\n✅ Image selected: " + selectedImageUri.toString() + "\n");
        testApiKeyButton.setEnabled(true);
      }
    }
  }
}