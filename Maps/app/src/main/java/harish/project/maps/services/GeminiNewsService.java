package harish.project.maps.services;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import harish.project.maps.ArticlesActivity;

public class GeminiNewsService {
  private static final String TAG = "GeminiNewsService";

  private final Context context;
  private final GeminiService geminiService;
  private final ExecutorService executorService;
  private final NewsService newsService;

  public GeminiNewsService(Context context) {
    this.context = context;
    this.geminiService = new GeminiService();
    this.executorService = Executors.newFixedThreadPool(3);
    this.newsService = new NewsService(context);
  }

  public interface GeminiNewsCallback {
    void onNewsReceived(List<ArticlesActivity.Article> articles);

    void onError(String error);
  }

  public void fetchLiveTrafficNews(Location location, GeminiNewsCallback callback) {
    executorService.execute(() -> {
      try {
        List<ArticlesActivity.Article> articles = new ArrayList<>();

        // First, get traditional news from existing service
        newsService.fetchLocalNews(location, new NewsService.NewsCallback() {
          @Override
          public void onNewsReceived(List<ArticlesActivity.Article> traditionalArticles) {
            articles.addAll(traditionalArticles);

            // Then enhance with Gemini-generated traffic news
            geminiService.generateTrafficNewsUpdate(location, new GeminiService.GeminiCallback() {
              @Override
              public void onSuccess(String response) {
                try {
                  List<ArticlesActivity.Article> geminiArticles = parseGeminiNewsResponse(response);
                  articles.addAll(geminiArticles);

                  // Get live traffic alerts
                  geminiService.generateTrafficAlerts(location, new GeminiService.GeminiCallback() {
                    @Override
                    public void onSuccess(String alertsResponse) {
                      try {
                        List<ArticlesActivity.Article> alertArticles = parseGeminiAlertsResponse(alertsResponse);
                        articles.addAll(alertArticles);

                        // Sort and prioritize articles
                        articles.sort((a1, a2) -> {
                          // Prioritize alerts and traffic-related news
                          boolean a1Alert = a1.getTitle().toLowerCase().contains("alert") ||
                              a1.getTitle().toLowerCase().contains("accident") ||
                              a1.getTitle().toLowerCase().contains("closure");
                          boolean a2Alert = a2.getTitle().toLowerCase().contains("alert") ||
                              a2.getTitle().toLowerCase().contains("accident") ||
                              a2.getTitle().toLowerCase().contains("closure");

                          if (a1Alert && !a2Alert)
                            return -1;
                          if (!a1Alert && a2Alert)
                            return 1;

                          // Then prioritize traffic-related news
                          boolean a1Traffic = a1.getTitle().toLowerCase().contains("traffic") ||
                              a1.getDescription().toLowerCase().contains("traffic");
                          boolean a2Traffic = a2.getTitle().toLowerCase().contains("traffic") ||
                              a2.getDescription().toLowerCase().contains("traffic");

                          if (a1Traffic && !a2Traffic)
                            return -1;
                          if (!a1Traffic && a2Traffic)
                            return 1;

                          return 0;
                        });

                        callback.onNewsReceived(articles);
                      } catch (Exception e) {
                        Log.e(TAG, "Error parsing alerts response", e);
                        callback.onNewsReceived(articles);
                      }
                    }

                    @Override
                    public void onError(String error) {
                      Log.e(TAG, "Error generating traffic alerts: " + error);
                      callback.onNewsReceived(articles);
                    }
                  });
                } catch (Exception e) {
                  Log.e(TAG, "Error parsing Gemini news response", e);
                  callback.onNewsReceived(articles);
                }
              }

              @Override
              public void onError(String error) {
                Log.e(TAG, "Error generating traffic news: " + error);
                callback.onNewsReceived(articles);
              }
            });
          }

          @Override
          public void onError(String error) {
            Log.e(TAG, "Error fetching traditional news: " + error);
            // Continue with Gemini-generated news only
            generateGeminiOnlyNews(location, callback);
          }
        });

      } catch (Exception e) {
        Log.e(TAG, "Error in fetchLiveTrafficNews", e);
        callback.onError("Failed to fetch news: " + e.getMessage());
      }
    });
  }

  private void generateGeminiOnlyNews(Location location, GeminiNewsCallback callback) {
    geminiService.generateTrafficNewsUpdate(location, new GeminiService.GeminiCallback() {
      @Override
      public void onSuccess(String response) {
        try {
          List<ArticlesActivity.Article> articles = parseGeminiNewsResponse(response);
          callback.onNewsReceived(articles);
        } catch (Exception e) {
          Log.e(TAG, "Error parsing Gemini response", e);
          callback.onError("Failed to parse news response");
        }
      }

      @Override
      public void onError(String error) {
        Log.e(TAG, "Error generating Gemini news: " + error);
        callback.onError("Failed to generate news: " + error);
      }
    });
  }

  private List<ArticlesActivity.Article> parseGeminiNewsResponse(String response) {
    List<ArticlesActivity.Article> articles = new ArrayList<>();

    try {
      // Try to parse as JSON first
      if (response.trim().startsWith("[")) {
        JSONArray jsonArray = new JSONArray(response);
        for (int i = 0; i < jsonArray.length(); i++) {
          JSONObject article = jsonArray.getJSONObject(i);
          String title = article.getString("title");
          String description = article.getString("description");
          String time = article.optString("time", "Just now");

          articles.add(new ArticlesActivity.Article(title, description, time));
        }
      } else {
        // If not JSON, parse as text and create articles
        String[] lines = response.split("\n");
        String currentTitle = "";
        String currentDescription = "";

        for (String line : lines) {
          line = line.trim();
          if (line.isEmpty())
            continue;

          if (line.startsWith("Title:") || line.startsWith("**")) {
            if (!currentTitle.isEmpty() && !currentDescription.isEmpty()) {
              articles.add(new ArticlesActivity.Article(currentTitle, currentDescription, "Just now"));
            }
            currentTitle = line.replace("Title:", "").replace("**", "").trim();
            currentDescription = "";
          } else if (line.startsWith("Description:") || line.startsWith("-")) {
            currentDescription = line.replace("Description:", "").replace("-", "").trim();
          } else if (!currentTitle.isEmpty()) {
            if (currentDescription.isEmpty()) {
              currentDescription = line;
            } else {
              currentDescription += " " + line;
            }
          }
        }

        // Add the last article
        if (!currentTitle.isEmpty() && !currentDescription.isEmpty()) {
          articles.add(new ArticlesActivity.Article(currentTitle, currentDescription, "Just now"));
        }
      }
    } catch (JSONException e) {
      Log.e(TAG, "Error parsing JSON response", e);
      // Fallback: create a single article from the response
      articles.add(new ArticlesActivity.Article(
          "AI-Generated Traffic Update",
          response.length() > 200 ? response.substring(0, 200) + "..." : response,
          "Just now"));
    }

    return articles;
  }

  private List<ArticlesActivity.Article> parseGeminiAlertsResponse(String response) {
    List<ArticlesActivity.Article> articles = new ArrayList<>();

    try {
      if (response.trim().startsWith("[")) {
        JSONArray jsonArray = new JSONArray(response);
        for (int i = 0; i < jsonArray.length(); i++) {
          JSONObject alert = jsonArray.getJSONObject(i);
          String alertType = alert.getString("alert_type");
          String message = alert.getString("message");
          String severity = alert.optString("severity", "medium");

          String title = "🚨 " + alertType.toUpperCase() + " ALERT";
          String description = message + " (Severity: " + severity + ")";

          articles.add(new ArticlesActivity.Article(title, description, "Just now"));
        }
      } else {
        // Parse as text
        String[] lines = response.split("\n");
        for (String line : lines) {
          line = line.trim();
          if (line.isEmpty() || line.startsWith("```"))
            continue;

          if (line.contains("ALERT") || line.contains("Alert")) {
            articles.add(new ArticlesActivity.Article(
                "🚨 " + line,
                "Traffic alert for your area",
                "Just now"));
          }
        }
      }
    } catch (Exception e) {
      Log.e(TAG, "Error parsing alerts response", e);
    }

    return articles;
  }

  public void getLiveTrafficReport(Location location, GeminiNewsCallback callback) {
    geminiService.generateLiveTrafficReport(location, new GeminiService.GeminiCallback() {
      @Override
      public void onSuccess(String response) {
        List<ArticlesActivity.Article> articles = new ArrayList<>();
        articles.add(new ArticlesActivity.Article(
            "🚦 Live Traffic Report",
            response,
            "Live"));
        callback.onNewsReceived(articles);
      }

      @Override
      public void onError(String error) {
        callback.onError("Failed to generate traffic report: " + error);
      }
    });
  }

  public void getTrafficPrediction(Location location, String timeFrame, GeminiNewsCallback callback) {
    geminiService.generateTrafficPrediction(location, timeFrame, new GeminiService.GeminiCallback() {
      @Override
      public void onSuccess(String response) {
        List<ArticlesActivity.Article> articles = new ArrayList<>();
        articles.add(new ArticlesActivity.Article(
            "🔮 Traffic Prediction (" + timeFrame + ")",
            response,
            "Prediction"));
        callback.onNewsReceived(articles);
      }

      @Override
      public void onError(String error) {
        callback.onError("Failed to generate traffic prediction: " + error);
      }
    });
  }

  public void shutdown() {
    executorService.shutdown();
    geminiService.shutdown();
  }
}