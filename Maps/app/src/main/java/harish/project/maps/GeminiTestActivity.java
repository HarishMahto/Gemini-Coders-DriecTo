package harish.project.maps;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import harish.project.maps.services.GeminiService;

public class GeminiTestActivity extends AppCompatActivity {
  private static final String TAG = "GeminiTestActivity";

  private TextView resultText;
  private Button testButton;
  private GeminiService geminiService;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_gemini_test);

    geminiService = new GeminiService();

    resultText = findViewById(R.id.resultText);
    testButton = findViewById(R.id.testButton);
    Button testModelsButton = findViewById(R.id.testModelsButton);
    Button checkApiButton = findViewById(R.id.checkApiButton);

    testButton.setOnClickListener(v -> testApiConnection());
    testModelsButton.setOnClickListener(v -> testModelAvailability());
    checkApiButton.setOnClickListener(v -> checkApiKeyAndModels());
  }

  private void testApiConnection() {
    testButton.setEnabled(false);
    resultText.setText("Testing API connection...");

    geminiService.testConnection(new GeminiService.GeminiCallback() {
      @Override
      public void onSuccess(String response) {
        runOnUiThread(() -> {
          resultText.setText("✅ Success!\n\nResponse:\n" + response);
          testButton.setEnabled(true);
          Toast.makeText(GeminiTestActivity.this, "API connection successful!", Toast.LENGTH_SHORT).show();
        });
      }

      @Override
      public void onError(String error) {
        runOnUiThread(() -> {
          resultText.setText("❌ Error:\n" + error);
          testButton.setEnabled(true);
          Toast.makeText(GeminiTestActivity.this, "API connection failed: " + error, Toast.LENGTH_LONG).show();
          Log.e(TAG, "API Error: " + error);
        });
      }
    });
  }

  private void testModelAvailability() {
    testButton.setEnabled(false);
    resultText.setText("Testing available models...");

    geminiService.testModelAvailability(new GeminiService.GeminiCallback() {
      @Override
      public void onSuccess(String response) {
        runOnUiThread(() -> {
          resultText.setText("🔍 Model Test Results:\n\n" + response);
          testButton.setEnabled(true);
          Toast.makeText(GeminiTestActivity.this, "Model test completed!", Toast.LENGTH_SHORT).show();
        });
      }

      @Override
      public void onError(String error) {
        runOnUiThread(() -> {
          resultText.setText("❌ Model Test Error:\n" + error);
          testButton.setEnabled(true);
          Toast.makeText(GeminiTestActivity.this, "Model test failed: " + error, Toast.LENGTH_LONG).show();
          Log.e(TAG, "Model Test Error: " + error);
        });
      }
    });
  }

  private void checkApiKeyAndModels() {
    testButton.setEnabled(false);
    resultText.setText("Checking API key and available models...");

    geminiService.checkApiKeyAndModels(new GeminiService.GeminiCallback() {
      @Override
      public void onSuccess(String response) {
        runOnUiThread(() -> {
          resultText.setText("🔑 API Key Check Results:\n\n" + response);
          testButton.setEnabled(true);
          Toast.makeText(GeminiTestActivity.this, "API key check completed!", Toast.LENGTH_SHORT).show();
        });
      }

      @Override
      public void onError(String error) {
        runOnUiThread(() -> {
          resultText.setText("❌ API Key Check Error:\n" + error);
          testButton.setEnabled(true);
          Toast.makeText(GeminiTestActivity.this, "API key check failed: " + error, Toast.LENGTH_LONG).show();
          Log.e(TAG, "API Key Check Error: " + error);
        });
      }
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (geminiService != null) {
      geminiService.shutdown();
    }
  }
}