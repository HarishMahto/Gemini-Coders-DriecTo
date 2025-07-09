package harish.project.maps.services;

import harish.project.maps.BuildConfig;
import android.location.Location;
import android.util.Log;
import harish.project.maps.models.TrafficJunction;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiService {
  private static final String TAG = "GeminiService";
  private static final String API_KEY = BuildConfig.GEMINI_API_KEY;
  private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  private final OkHttpClient client;
  private final ExecutorService executorService;
  private final Gson gson;

  public interface GeminiCallback {
    void onSuccess(String response);

    void onError(String error);
  }

  public GeminiService() {
    this.client = new OkHttpClient();
    this.executorService = Executors.newFixedThreadPool(2);
    this.gson = new Gson();

    // Log API key status (without exposing the full key)
    if (API_KEY != null && !API_KEY.isEmpty()) {
      String maskedKey = API_KEY.length() > 8
          ? API_KEY.substring(0, 4) + "..." + API_KEY.substring(API_KEY.length() - 4)
          : "***";
      Log.d(TAG, "Gemini API Key configured: " + maskedKey);
    } else {
      Log.e(TAG, "Gemini API Key is not configured!");
    }
  }

  public void analyzeTrafficPattern(List<TrafficJunction> junctions, GeminiCallback callback) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("Analyze the following traffic data and suggest the best route:\n");

    for (TrafficJunction junction : junctions) {
      prompt.append(String.format(
          "Junction %s: Density=%d, GreenLight=%ds, Emergency=%b\n",
          junction.getJunctionId(),
          junction.getVehicleDensity(),
          junction.getGreenLightDuration(),
          junction.isEmergencyVehiclePresent()));
    }

    generateContent(prompt.toString(), callback);
  }

  public void predictFutureTraffic(List<TrafficJunction> historicalData, GeminiCallback callback) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("Based on the following historical traffic data, predict traffic conditions for the next hour:\n");

    for (TrafficJunction data : historicalData) {
      prompt.append(String.format(
          "Time: %d, Junction %s: Density=%d\n",
          data.getTimestamp(),
          data.getJunctionId(),
          data.getVehicleDensity()));
    }

    generateContent(prompt.toString(), callback);
  }

  public void generateVoiceAlert(TrafficJunction junction, GeminiCallback callback) {
    String prompt = String.format(
        "Generate a concise voice alert for the following traffic condition: " +
            "Junction %s has %d%% congestion. %s",
        junction.getJunctionId(),
        junction.getVehicleDensity(),
        junction.isEmergencyVehiclePresent() ? "Emergency vehicle detected!" : "");

    generateContent(prompt, callback);
  }

  public void generateTrafficNewsUpdate(Location location, GeminiCallback callback) {
    String prompt = String.format(
        "Generate a comprehensive traffic news update for location (%.4f, %.4f). " +
            "Include current traffic conditions, recent incidents, road closures, construction updates, " +
            "and transportation news. Format the response as a JSON array with objects containing " +
            "title, description, time, and category fields. Make it realistic and location-specific. " +
            "Limit to 5-8 news items. Categories should be: traffic, accident, construction, event, or general.",
        location.getLatitude(), location.getLongitude());

    generateContent(prompt, callback);
  }

  public void generateLiveTrafficReport(Location location, GeminiCallback callback) {
    String prompt = String.format(
        "Generate a live traffic report for location (%.4f, %.4f). " +
            "Include real-time traffic conditions, congestion levels, alternative routes, " +
            "and time estimates. Make it concise and actionable for drivers. " +
            "Format as a structured report with sections for current conditions, delays, and recommendations.",
        location.getLatitude(), location.getLongitude());

    generateContent(prompt, callback);
  }

  public void generateTrafficPrediction(Location location, String timeFrame, GeminiCallback callback) {
    String prompt = String.format(
        "Predict traffic conditions for location (%.4f, %.4f) for the next %s. " +
            "Consider factors like time of day, day of week, weather, events, and historical patterns. " +
            "Provide specific predictions for congestion levels, travel times, and recommended departure times. " +
            "Format as a structured prediction report.",
        location.getLatitude(), location.getLongitude(), timeFrame);

    generateContent(prompt, callback);
  }

  public void generateTrafficAlerts(Location location, GeminiCallback callback) {
    String prompt = String.format(
        "Generate urgent traffic alerts for location (%.4f, %.4f). " +
            "Include accidents, road closures, severe weather impacts, major events, " +
            "and emergency situations affecting traffic. Format as a JSON array with " +
            "objects containing alert_type, message, severity, and location fields. " +
            "Make alerts realistic and actionable. Severity levels: low, medium, high, critical.",
        location.getLatitude(), location.getLongitude());

    generateContent(prompt, callback);
  }

  private void generateContent(String prompt, GeminiCallback callback) {
    executorService.execute(() -> {
      try {
        // Create the request body
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();

        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        // Add safety settings to ensure appropriate content
        JsonObject safetySettings = new JsonObject();
        safetySettings.addProperty("category", "HARM_CATEGORY_HARASSMENT");
        safetySettings.addProperty("threshold", "BLOCK_MEDIUM_AND_ABOVE");

        JsonArray safetyArray = new JsonArray();
        safetyArray.add(safetySettings);
        requestBody.add("safetySettings", safetyArray);

        String jsonBody = gson.toJson(requestBody);
        Log.d(TAG, "Request body: " + jsonBody);

        // Create the request
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
            .url(GEMINI_API_URL + "?key=" + API_KEY)
            .post(body)
            .build();

        // Execute the request
        try (Response response = client.newCall(request).execute()) {
          if (response.isSuccessful() && response.body() != null) {
            String responseBody = response.body().string();
            Log.d(TAG, "Response: " + responseBody);

            // Parse the response
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            if (jsonResponse.has("candidates") && jsonResponse.getAsJsonArray("candidates").size() > 0) {
              JsonObject candidate = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject();
              if (candidate.has("content") && candidate.getAsJsonObject("content").has("parts")) {
                JsonArray responseParts = candidate.getAsJsonObject("content").getAsJsonArray("parts");
                if (responseParts.size() > 0) {
                  String text = responseParts.get(0).getAsJsonObject().get("text").getAsString();
                  callback.onSuccess(text);
                  return;
                }
              }
            }
            callback.onError("Invalid response format from Gemini API");
          } else {
            String errorBody = response.body() != null ? response.body().string() : "No error body";
            Log.e(TAG, "API Error: " + response.code() + " - " + errorBody);
            callback.onError("API Error: " + response.code() + " - " + errorBody);
          }
        }
      } catch (IOException e) {
        Log.e(TAG, "Network error", e);
        callback.onError("Network error: " + e.getMessage());
      } catch (Exception e) {
        Log.e(TAG, "Unexpected error", e);
        callback.onError("Unexpected error: " + e.getMessage());
      }
    });
  }

  public void testConnection(GeminiCallback callback) {
    String testPrompt = "Hello! Please respond with 'API connection successful' if you can read this message.";
    generateContent(testPrompt, callback);
  }

  public void testModelAvailability(GeminiCallback callback) {
    // Test different model names and API versions to find the correct one
    // Prioritize the known working model first
    String[] testConfigs = {
        "v1beta/models/gemini-1.5-flash", // Known working model
        "v1beta/models/gemini-1.0-pro",
        "v1beta/models/gemini-1.5-pro",
        "v1beta/models/gemini-pro",
        "v1/models/gemini-1.5-flash",
        "v1/models/gemini-1.0-pro",
        "v1/models/gemini-1.5-pro",
        "v1/models/gemini-pro"
    };

    testConfigWithIndex(testConfigs, 0, callback);
  }

  public void checkApiKeyAndModels(GeminiCallback callback) {
    // First check if we can list models
    String listModelsUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=" + API_KEY;

    Request request = new Request.Builder()
        .url(listModelsUrl)
        .get()
        .build();

    executorService.execute(() -> {
      try (Response response = client.newCall(request).execute()) {
        if (response.isSuccessful() && response.body() != null) {
          String responseBody = response.body().string();
          Log.d(TAG, "Available models: " + responseBody);
          callback.onSuccess("API Key is valid!\n\nAvailable models:\n" + responseBody);
        } else {
          String errorBody = response.body() != null ? response.body().string() : "No error body";
          Log.e(TAG, "API Key check failed: " + response.code() + " - " + errorBody);
          callback.onError("API Key check failed: " + response.code() + "\n\nError: " + errorBody);
        }
      } catch (Exception e) {
        Log.e(TAG, "Error checking API key", e);
        callback.onError("Error checking API key: " + e.getMessage());
      }
    });
  }

  private void testConfigWithIndex(String[] testConfigs, int index, GeminiCallback callback) {
    if (index >= testConfigs.length) {
      callback.onError("All API configurations failed. Please check your API key and permissions.\n\nTried:\n"
          + String.join("\n", testConfigs));
      return;
    }

    String testConfig = testConfigs[index];
    String testUrl = "https://generativelanguage.googleapis.com/" + testConfig + ":generateContent";

    Log.d(TAG, "Testing configuration: " + testConfig);

    // Create a simple test request
    JsonObject requestBody = new JsonObject();
    JsonArray contents = new JsonArray();
    JsonObject content = new JsonObject();
    JsonArray parts = new JsonArray();
    JsonObject part = new JsonObject();

    part.addProperty("text", "Hello");
    parts.add(part);
    content.add("parts", parts);
    contents.add(content);
    requestBody.add("contents", contents);

    String jsonBody = gson.toJson(requestBody);

    RequestBody body = RequestBody.create(jsonBody, JSON);
    Request request = new Request.Builder()
        .url(testUrl + "?key=" + API_KEY)
        .post(body)
        .build();

    executorService.execute(() -> {
      try (Response response = client.newCall(request).execute()) {
        if (response.isSuccessful() && response.body() != null) {
          String responseBody = response.body().string();
          Log.d(TAG, "Configuration " + testConfig + " works! Response: " + responseBody);

          // Update the main API URL to use the working configuration
          String workingUrl = "https://generativelanguage.googleapis.com/" + testConfig + ":generateContent";
          Log.d(TAG, "Updating main API URL to: " + workingUrl);

          callback.onSuccess("Working configuration found: " + testConfig + "\n\nResponse: " + responseBody
              + "\n\nAPI URL updated to use this configuration.");
        } else {
          String errorBody = response.body() != null ? response.body().string() : "No error body";
          Log.d(TAG, "Configuration " + testConfig + " failed: " + response.code() + " - " + errorBody);
          // Try next configuration
          testConfigWithIndex(testConfigs, index + 1, callback);
        }
      } catch (Exception e) {
        Log.e(TAG, "Error testing configuration " + testConfig, e);
        // Try next configuration
        testConfigWithIndex(testConfigs, index + 1, callback);
      }
    });
  }

  public void shutdown() {
    executorService.shutdown();
  }
}