package harish.project.maps;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

public class TokenFetcher {
  public static String fetchAccessToken() {
    try {
      OkHttpClient client = new OkHttpClient();
      Request request = new Request.Builder()
          .url(Config.BACKEND_URL + "/token")
          .addHeader("x-api-key", Config.BACKEND_API_KEY)
          .get()
          .build();
      Response response = client.newCall(request).execute();
      if (response.isSuccessful() && response.body() != null) {
        String responseBody = response.body().string();
        JSONObject json = new JSONObject(responseBody);
        return json.optString("access_token", null);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  // Alternative method to get API key directly (if you prefer not to use backend)
  public static String getGoogleCloudApiKey() {
    return Config.GOOGLE_CLOUD_API_KEY;
  }
}