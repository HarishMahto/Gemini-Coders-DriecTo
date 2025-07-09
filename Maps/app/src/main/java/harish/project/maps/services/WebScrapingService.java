package harish.project.maps.services;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import harish.project.maps.ArticlesActivity;

public class WebScrapingService {
  private static final String TAG = "WebScrapingService";
  private final ExecutorService executorService;
  private final Context context;

  public WebScrapingService(Context context) {
    this.context = context;
    this.executorService = Executors.newFixedThreadPool(2);
  }

  public interface ScrapingCallback {
    void onNewsScraped(List<ArticlesActivity.Article> articles);

    void onError(String error);
  }

  public void scrapeLocalNews(Location userLocation, ScrapingCallback callback) {
    executorService.execute(() -> {
      try {
        List<ArticlesActivity.Article> articles = new ArrayList<>();

        // Scrape from multiple sources
        articles.addAll(scrapeFromTwitter(userLocation));
        articles.addAll(scrapeFromReddit(userLocation));
        articles.addAll(scrapeFromLocalNewsSites(userLocation));
        articles.addAll(scrapeFromTrafficAPIs(userLocation));

        callback.onNewsScraped(articles);
      } catch (Exception e) {
        Log.e(TAG, "Error scraping news", e);
        callback.onError("Failed to scrape news: " + e.getMessage());
      }
    });
  }

  private List<ArticlesActivity.Article> scrapeFromTwitter(Location userLocation) {
    List<ArticlesActivity.Article> articles = new ArrayList<>();

    try {
      // Simulate Twitter scraping for traffic-related tweets
      // In a real implementation, you'd use Twitter API or web scraping
      String cityName = getCityFromLocation(userLocation);

      articles.add(new ArticlesActivity.Article(
          "🚗 Traffic Alert via Twitter",
          "Heavy traffic reported on " + cityName + " highways. Multiple users reporting delays of 20+ minutes.",
          "15 minutes ago"));

      articles.add(new ArticlesActivity.Article(
          "🚧 Construction Update",
          "Road work on Main St causing major delays. Police directing traffic at intersection.",
          "45 minutes ago"));

    } catch (Exception e) {
      Log.e(TAG, "Error scraping Twitter", e);
    }

    return articles;
  }

  private List<ArticlesActivity.Article> scrapeFromReddit(Location userLocation) {
    List<ArticlesActivity.Article> articles = new ArrayList<>();

    try {
      // Simulate Reddit scraping for local community posts
      String cityName = getCityFromLocation(userLocation);

      articles.add(new ArticlesActivity.Article(
          "Reddit Community Alert",
          "r/" + cityName.toLowerCase().replace(" ", "") + " users reporting accident on Highway 101. Avoid the area.",
          "1 hour ago"));

      articles.add(new ArticlesActivity.Article(
          "Local Event Traffic",
          "Major concert tonight at " + cityName + " Arena. Expect heavy traffic around venue from 6-11 PM.",
          "2 hours ago"));

    } catch (Exception e) {
      Log.e(TAG, "Error scraping Reddit", e);
    }

    return articles;
  }

  private List<ArticlesActivity.Article> scrapeFromLocalNewsSites(Location userLocation) {
    List<ArticlesActivity.Article> articles = new ArrayList<>();

    try {
      String cityName = getCityFromLocation(userLocation);

      // Simulate scraping from local news websites
      articles.add(new ArticlesActivity.Article(
          cityName + " News: Traffic Signal Upgrade",
          "City announces upgrade to traffic signals at 15 major intersections. Work to begin next week.",
          "3 hours ago"));

      articles.add(new ArticlesActivity.Article(
          "Public Transportation Changes",
          "New bus routes announced for " + cityName + " metro area. Service changes effective Monday.",
          "4 hours ago"));

      articles.add(new ArticlesActivity.Article(
          "Weather Impact on Traffic",
          "Heavy rain causing flooding on several roads. Multiple road closures reported.",
          "5 hours ago"));

    } catch (Exception e) {
      Log.e(TAG, "Error scraping local news sites", e);
    }

    return articles;
  }

  private List<ArticlesActivity.Article> scrapeFromTrafficAPIs(Location userLocation) {
    List<ArticlesActivity.Article> articles = new ArrayList<>();

    try {
      // Simulate traffic API data
      articles.add(new ArticlesActivity.Article(
          "Real-time Traffic Data",
          "Traffic sensors indicate 30% increase in congestion on downtown routes. Peak hours extended.",
          "10 minutes ago"));

      articles.add(new ArticlesActivity.Article(
          "Accident Detection System",
          "Automated system detected potential accident on I-95. Emergency services dispatched.",
          "25 minutes ago"));

      articles.add(new ArticlesActivity.Article(
          "Traffic Flow Optimization",
          "Smart traffic lights adjusting timing based on real-time conditions. Flow improved by 15%.",
          "1 hour ago"));

    } catch (Exception e) {
      Log.e(TAG, "Error scraping traffic APIs", e);
    }

    return articles;
  }

  private String getCityFromLocation(Location location) {
    double lat = location.getLatitude();
    double lon = location.getLongitude();

    // Simple mapping based on coordinates
    if (lat > 40.0 && lat < 45.0 && lon > -80.0 && lon < -70.0) {
      return "New York";
    } else if (lat > 34.0 && lat < 35.0 && lon > -119.0 && lon < -118.0) {
      return "Los Angeles";
    } else if (lat > 41.0 && lat < 42.0 && lon > -88.0 && lon < -87.0) {
      return "Chicago";
    } else if (lat > 29.0 && lat < 30.0 && lon > -96.0 && lon < -95.0) {
      return "Houston";
    } else if (lat > 33.0 && lat < 34.0 && lon > -112.0 && lon < -111.0) {
      return "Phoenix";
    } else {
      return "Your City";
    }
  }

  public void shutdown() {
    executorService.shutdown();
  }
}