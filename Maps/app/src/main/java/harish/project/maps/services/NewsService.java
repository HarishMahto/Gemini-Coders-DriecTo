package harish.project.maps.services;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import harish.project.maps.ArticlesActivity;

public class NewsService {
  private static final String TAG = "NewsService";
  private static final String NEWS_API_KEY = "cdbe088452ba42be8723dc7192640a72"; // Replace with actual API key
  private static final String NEWS_API_BASE_URL = "https://newsapi.org/v2/everything";
  private static final double SEARCH_RADIUS_KM = 50.0;

  private final ExecutorService executorService;
  private final Context context;
  private final WebScrapingService webScrapingService;

  public NewsService(Context context) {
    this.context = context;
    this.executorService = Executors.newFixedThreadPool(3);
    this.webScrapingService = new WebScrapingService(context);
  }

  public interface NewsCallback {
    void onNewsReceived(List<ArticlesActivity.Article> articles);

    void onError(String error);
  }

  public void fetchLocalNews(Location userLocation, NewsCallback callback) {
    executorService.execute(() -> {
      try {
        List<ArticlesActivity.Article> articles = new ArrayList<>();

        // Fetch from News API
        articles.addAll(fetchFromNewsAPI(userLocation));

        // Fetch from web scraping (social media, local news sites)
        webScrapingService.scrapeLocalNews(userLocation, new WebScrapingService.ScrapingCallback() {
          @Override
          public void onNewsScraped(List<ArticlesActivity.Article> scrapedArticles) {
            articles.addAll(scrapedArticles);

            // Fetch from local news sources (simulated)
            articles.addAll(fetchFromLocalSources(userLocation));

            // Fetch traffic and transportation news
            articles.addAll(fetchTrafficNews(userLocation));

            // Sort by relevance and recency
            articles.sort((a1, a2) -> {
              // Prioritize traffic-related news
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
          }

          @Override
          public void onError(String error) {
            Log.e(TAG, "Web scraping error: " + error);
            // Continue with other sources even if scraping fails
            articles.addAll(fetchFromLocalSources(userLocation));
            articles.addAll(fetchTrafficNews(userLocation));
            callback.onNewsReceived(articles);
          }
        });
      } catch (Exception e) {
        Log.e(TAG, "Error fetching news", e);
        callback.onError("Failed to fetch news: " + e.getMessage());
      }
    });
  }

  private List<ArticlesActivity.Article> fetchFromNewsAPI(Location userLocation) {
    List<ArticlesActivity.Article> articles = new ArrayList<>();

    try {
      // Calculate search area
      double lat = userLocation.getLatitude();
      double lon = userLocation.getLongitude();

      // Create search query for local news
      String query = String.format("traffic OR transportation OR road OR accident OR construction OR event");

      String urlString = String.format("%s?q=%s&language=en&sortBy=publishedAt&pageSize=20&apiKey=%s",
          NEWS_API_BASE_URL, query, NEWS_API_KEY);

      URL url = new URL(urlString);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("User-Agent", "Mozilla/5.0");

      int responseCode = connection.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
          response.append(line);
        }
        reader.close();

        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONArray articlesArray = jsonResponse.getJSONArray("articles");

        for (int i = 0; i < articlesArray.length() && i < 10; i++) {
          JSONObject article = articlesArray.getJSONObject(i);
          String title = article.getString("title");
          String description = article.getString("description");
          String publishedAt = article.getString("publishedAt");

          // Filter articles that might be relevant to the user's area
          if (isRelevantToLocation(title, description, userLocation)) {
            String timeAgo = getTimeAgo(publishedAt);
            articles.add(new ArticlesActivity.Article(title, description, timeAgo));
          }
        }
      }
    } catch (Exception e) {
      Log.e(TAG, "Error fetching from News API", e);
    }

    return articles;
  }

  private List<ArticlesActivity.Article> fetchFromLocalSources(Location userLocation) {
    List<ArticlesActivity.Article> articles = new ArrayList<>();

    // Simulate local news sources based on location
    String cityName = getCityFromLocation(userLocation);

    // Add some simulated local news based on common patterns
    articles.add(new ArticlesActivity.Article(
        "Local Traffic Update: " + cityName + " Downtown",
        "Heavy traffic reported on Main Street due to ongoing construction. Expect delays of 15-20 minutes.",
        "1 hour ago"));

    articles.add(new ArticlesActivity.Article(
        "New Traffic Light Installation in " + cityName,
        "Traffic lights being installed at the busy intersection of 5th Avenue and Park Street. Work expected to complete by end of week.",
        "3 hours ago"));

    articles.add(new ArticlesActivity.Article(
        "Road Closure Alert: " + cityName + " Bridge",
        "Bridge Street will be closed for maintenance from 10 PM to 6 AM starting tonight. Detour routes available.",
        "5 hours ago"));

    return articles;
  }

  private List<ArticlesActivity.Article> fetchTrafficNews(Location userLocation) {
    List<ArticlesActivity.Article> articles = new ArrayList<>();

    // Add traffic-specific news
    articles.add(new ArticlesActivity.Article(
        "Traffic Pattern Changes",
        "New one-way traffic pattern implemented in downtown area to improve flow during peak hours.",
        "2 hours ago"));

    articles.add(new ArticlesActivity.Article(
        "Accident Alert: Highway 101",
        "Multi-vehicle accident reported on Highway 101 near exit 15. Traffic backed up for 2 miles.",
        "30 minutes ago"));

    articles.add(new ArticlesActivity.Article(
        "Public Transportation Update",
        "Bus routes 15 and 22 experiencing delays due to road construction. Alternative routes suggested.",
        "1 hour ago"));

    return articles;
  }

  private boolean isRelevantToLocation(String title, String description, Location userLocation) {
    String text = (title + " " + description).toLowerCase();

    // Keywords that indicate local relevance
    String[] localKeywords = {
        "traffic", "road", "street", "highway", "construction", "accident",
        "transportation", "bus", "train", "metro", "subway", "bridge",
        "intersection", "signal", "light", "closure", "detour", "delay"
    };

    for (String keyword : localKeywords) {
      if (text.contains(keyword)) {
        return true;
      }
    }

    return false;
  }

  private String getCityFromLocation(Location location) {
    // This is a simplified version. In a real app, you'd use reverse geocoding
    double lat = location.getLatitude();
    double lon = location.getLongitude();

    // Simple mapping based on coordinates (this is just for demo)
    if (lat > 40.0 && lat < 45.0 && lon > -80.0 && lon < -70.0) {
      return "New York";
    } else if (lat > 34.0 && lat < 35.0 && lon > -119.0 && lon < -118.0) {
      return "Los Angeles";
    } else if (lat > 41.0 && lat < 42.0 && lon > -88.0 && lon < -87.0) {
      return "Chicago";
    } else {
      return "Your City";
    }
  }

  private String getTimeAgo(String publishedAt) {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
      Date publishedDate = sdf.parse(publishedAt);
      Date now = new Date();

      long diffInMillis = now.getTime() - publishedDate.getTime();
      long diffInHours = diffInMillis / (60 * 60 * 1000);

      if (diffInHours < 1) {
        return "Just now";
      } else if (diffInHours < 24) {
        return diffInHours + " hour" + (diffInHours > 1 ? "s" : "") + " ago";
      } else {
        long diffInDays = diffInHours / 24;
        return diffInDays + " day" + (diffInDays > 1 ? "s" : "") + " ago";
      }
    } catch (Exception e) {
      return "Recently";
    }
  }

  public void shutdown() {
    executorService.shutdown();
    webScrapingService.shutdown();
  }
}