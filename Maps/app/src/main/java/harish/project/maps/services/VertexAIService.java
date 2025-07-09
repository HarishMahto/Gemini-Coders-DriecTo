package harish.project.maps.services;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VertexAIService {
  private static final String TAG = "VertexAIService";
  private static final String VERTEX_AI_ENDPOINT = "https://us-central1-aiplatform.googleapis.com/v1/projects/YOUR_PROJECT_ID/locations/us-central1/publishers/google/models/text-ocr:predict";
  private static final String VERTEX_AI_VISION_ENDPOINT = "https://vision.googleapis.com/v1/images:annotate";

  private final OkHttpClient httpClient;
  private final ExecutorService executorService;
  private final Gson gson;

  public VertexAIService() {
    this.httpClient = new OkHttpClient();
    this.executorService = Executors.newCachedThreadPool();
    this.gson = new Gson();
  }

  public interface LicensePlateCallback {
    void onSuccess(String licensePlate);

    void onError(String error);
  }

  public void recognizeLicensePlate(Context context, Uri imageUri, String accessToken, LicensePlateCallback callback) {
    executorService.execute(() -> {
      try {
        // Convert image to base64
        String base64Image = convertImageToBase64(context, imageUri);
        if (base64Image == null) {
          callback.onError("Failed to convert image to base64");
          return;
        }

        // Use Vertex AI Vision API for better OCR
        String licensePlate = performVisionOCR(base64Image, accessToken);
        if (licensePlate != null && !licensePlate.isEmpty()) {
          callback.onSuccess(licensePlate);
        } else {
          callback.onError("No license plate detected in the image");
        }

      } catch (Exception e) {
        Log.e(TAG, "Error recognizing license plate", e);
        callback.onError("Error: " + e.getMessage());
      }
    });
  }

  // Alternative method that uses API key directly (no backend required)
  public void recognizeLicensePlateWithApiKey(Context context, Uri imageUri, LicensePlateCallback callback) {
    executorService.execute(() -> {
      try {
        // Convert image to base64
        String base64Image = convertImageToBase64(context, imageUri);
        if (base64Image == null) {
          callback.onError("Failed to convert image to base64");
          return;
        }

        // Use Google Cloud Vision API directly with API key
        String licensePlate = performVisionOCRWithApiKey(base64Image);
        if (licensePlate != null && !licensePlate.isEmpty()) {
          callback.onSuccess(licensePlate);
        } else {
          callback.onError("No license plate detected in the image");
        }

      } catch (Exception e) {
        Log.e(TAG, "Error recognizing license plate", e);
        callback.onError("Error: " + e.getMessage());
      }
    });
  }

  private String convertImageToBase64(Context context, Uri imageUri) {
    try {
      InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
      if (inputStream == null) {
        return null;
      }

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      byte[] buffer = new byte[1024];
      int bytesRead;

      while ((bytesRead = inputStream.read(buffer)) != -1) {
        byteArrayOutputStream.write(buffer, 0, bytesRead);
      }

      inputStream.close();
      byte[] imageBytes = byteArrayOutputStream.toByteArray();
      return Base64.encodeToString(imageBytes, Base64.NO_WRAP);

    } catch (IOException e) {
      Log.e(TAG, "Error converting image to base64", e);
      return null;
    }
  }

  private String performVisionOCR(String base64Image, String accessToken) {
    try {
      // Create the request body for Vision API
      JSONObject requestBody = new JSONObject();
      JSONObject image = new JSONObject();
      image.put("content", base64Image);

      JSONObject feature = new JSONObject();
      feature.put("type", "TEXT_DETECTION");
      feature.put("maxResults", 10);

      JSONArray features = new JSONArray();
      features.put(feature);

      JSONObject request = new JSONObject();
      request.put("image", image);
      request.put("features", features);

      JSONArray requests = new JSONArray();
      requests.put(request);

      requestBody.put("requests", requests);

      // Make the API call
      RequestBody body = RequestBody.create(
          requestBody.toString(),
          MediaType.parse("application/json"));

      Request request_http = new Request.Builder()
          .url(VERTEX_AI_VISION_ENDPOINT + "?key=" + accessToken)
          .post(body)
          .build();

      Response response = httpClient.newCall(request_http).execute();

      if (response.isSuccessful() && response.body() != null) {
        String responseBody = response.body().string();
        return parseVisionResponse(responseBody);
      } else {
        Log.e(TAG, "Vision API request failed: " + response.code() + " " + response.message());
        return null;
      }

    } catch (Exception e) {
      Log.e(TAG, "Error performing Vision OCR", e);
      return null;
    }
  }

  private String performVisionOCRWithApiKey(String base64Image) {
    try {
      Log.d(TAG, "Starting Vision OCR with API key");

      // Create the request body for Vision API
      JSONObject requestBody = new JSONObject();
      JSONObject image = new JSONObject();
      image.put("content", base64Image);

      JSONObject feature = new JSONObject();
      feature.put("type", "TEXT_DETECTION");
      feature.put("maxResults", 10);

      JSONArray features = new JSONArray();
      features.put(feature);

      JSONObject request = new JSONObject();
      request.put("image", image);
      request.put("features", features);

      JSONArray requests = new JSONArray();
      requests.put(request);

      requestBody.put("requests", requests);

      // Make the API call with API key
      RequestBody body = RequestBody.create(
          requestBody.toString(),
          MediaType.parse("application/json"));

      String apiKey = harish.project.maps.Config.GOOGLE_CLOUD_API_KEY;

      if (apiKey == null || apiKey.equals("YOUR_ACTUAL_API_KEY_HERE")) {
        Log.e(TAG, "API key not configured properly");
        return null;
      }

      Log.d(TAG, "Making API request to Vision API");
      Request request_http = new Request.Builder()
          .url(VERTEX_AI_VISION_ENDPOINT + "?key=" + apiKey)
          .post(body)
          .build();

      Response response = httpClient.newCall(request_http).execute();

      Log.d(TAG, "API Response Code: " + response.code());
      Log.d(TAG, "API Response Message: " + response.message());

      if (response.isSuccessful() && response.body() != null) {
        String responseBody = response.body().string();
        Log.d(TAG, "API Response received, length: " + responseBody.length());
        return parseVisionResponse(responseBody);
      } else {
        String errorBody = response.body() != null ? response.body().string() : "No error body";
        Log.e(TAG, "Vision API request failed: " + response.code() + " " + response.message());
        Log.e(TAG, "Error body: " + errorBody);
        return null;
      }

    } catch (Exception e) {
      Log.e(TAG, "Error performing Vision OCR with API key", e);
      return null;
    }
  }

  private String parseVisionResponse(String responseBody) {
    try {
      Log.d(TAG, "Parsing Vision API response: " + responseBody.substring(0, Math.min(500, responseBody.length())));

      JSONObject response = new JSONObject(responseBody);
      JSONArray responses = response.getJSONArray("responses");

      if (responses.length() > 0) {
        JSONObject firstResponse = responses.getJSONObject(0);

        if (firstResponse.has("textAnnotations") && firstResponse.getJSONArray("textAnnotations").length() > 0) {
          JSONArray textAnnotations = firstResponse.getJSONArray("textAnnotations");

          // Get the full text
          String fullText = textAnnotations.getJSONObject(0).getString("description");
          Log.d(TAG, "Full text detected: " + fullText);

          // Extract license plate using regex patterns
          String licensePlate = extractLicensePlate(fullText);

          if (licensePlate != null) {
            Log.d(TAG, "License plate found: " + licensePlate);
            return licensePlate;
          }

          // If no specific pattern found, return the first few words that might be a
          // plate
          String[] words = fullText.split("\\s+");
          Log.d(TAG, "Words found: " + String.join(", ", words));

          for (String word : words) {
            if (isLikelyLicensePlate(word)) {
              Log.d(TAG, "Likely license plate word: " + word);
              return word.toUpperCase();
            }
          }

          Log.d(TAG, "No license plate pattern found in text");
        } else {
          Log.d(TAG, "No text annotations found in response");
        }
      } else {
        Log.d(TAG, "No responses found in API response");
      }

      return null;

    } catch (JSONException e) {
      Log.e(TAG, "Error parsing Vision API response", e);
      Log.e(TAG, "Response body: " + responseBody);
      return null;
    }
  }

  private String extractLicensePlate(String text) {
    // Common Indian license plate patterns
    String[] patterns = {
        // Format: KA-01-AB-1234 or KA01AB1234
        "\\b[A-Z]{2}[\\s-]?[0-9]{1,2}[\\s-]?[A-Z]{1,2}[\\s-]?[0-9]{1,4}\\b",
        // Format: KA-01-1234 or KA011234
        "\\b[A-Z]{2}[\\s-]?[0-9]{1,2}[\\s-]?[0-9]{1,4}\\b",
        // Format: AB-12-CD-1234
        "\\b[A-Z]{2}[\\s-]?[0-9]{2}[\\s-]?[A-Z]{2}[\\s-]?[0-9]{4}\\b",
        // Format: 12-AB-1234
        "\\b[0-9]{2}[\\s-]?[A-Z]{2}[\\s-]?[0-9]{4}\\b"
    };

    for (String pattern : patterns) {
      java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
      java.util.regex.Matcher m = p.matcher(text);

      if (m.find()) {
        String match = m.group();
        // Clean up the match (remove extra spaces and normalize)
        return match.replaceAll("[\\s-]+", "").toUpperCase();
      }
    }

    return null;
  }

  private boolean isLikelyLicensePlate(String word) {
    if (word == null || word.length() < 5 || word.length() > 15) {
      return false;
    }

    // Check if it contains both letters and numbers
    boolean hasLetters = word.matches(".*[A-Za-z].*");
    boolean hasNumbers = word.matches(".*[0-9].*");

    if (!hasLetters || !hasNumbers) {
      return false;
    }

    // Check if it doesn't contain special characters (except common separators)
    if (word.matches(".*[^A-Za-z0-9\\s-].*")) {
      return false;
    }

    return true;
  }

  public void shutdown() {
    executorService.shutdown();
  }
}