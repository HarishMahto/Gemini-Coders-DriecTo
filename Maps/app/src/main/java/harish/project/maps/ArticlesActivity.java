package harish.project.maps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import harish.project.maps.services.LocationService;
import harish.project.maps.services.NewsService;
import harish.project.maps.services.GeminiNewsService;

public class ArticlesActivity extends BaseActivity {
  private RecyclerView articlesRecyclerView;
  private ArticlesAdapter articlesAdapter;
  private ProgressBar progressBar;
  private TextView emptyStateText;
  private SwipeRefreshLayout swipeRefreshLayout;
  private TextView locationText;

  private LocationService locationService;
  private NewsService newsService;
  private GeminiNewsService geminiNewsService;
  private Location currentLocation;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_articles);

    // Initialize services
    locationService = new LocationService(this);
    newsService = new NewsService(this);
    geminiNewsService = new GeminiNewsService(this);

    // Initialize views
    initializeViews();
    setupClickListeners();
    setupRecyclerView();

    // Check location permission and fetch news
    checkLocationPermissionAndFetchNews();
  }

  private void initializeViews() {
    articlesRecyclerView = findViewById(R.id.articlesRecyclerView);
    progressBar = findViewById(R.id.progressBar);
    emptyStateText = findViewById(R.id.emptyStateText);
    swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    locationText = findViewById(R.id.locationText);
    ImageButton backButton = findViewById(R.id.backButton);

    // Set up back button
    backButton.setOnClickListener(v -> finish());
  }

  private void setupClickListeners() {
    // Set up swipe refresh
    swipeRefreshLayout.setOnRefreshListener(this::refreshNews);

    // Set up AI feature buttons
    findViewById(R.id.btnLiveReport).setOnClickListener(v -> getLiveTrafficReport());
    findViewById(R.id.btnPrediction).setOnClickListener(v -> showPredictionDialog());
  }

  private void setupRecyclerView() {
    articlesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    articlesAdapter = new ArticlesAdapter(new ArrayList<>());
    articlesRecyclerView.setAdapter(articlesAdapter);
  }

  private void checkLocationPermissionAndFetchNews() {
    if (locationService.hasLocationPermission()) {
      fetchNewsBasedOnLocation();
    } else {
      showLocationPermissionDialog();
    }
  }

  private void showLocationPermissionDialog() {
    new AlertDialog.Builder(this)
        .setTitle("Location Permission Required")
        .setMessage("This app needs location access to show you relevant news and activities around your area.")
        .setPositiveButton("Grant Permission", (dialog, which) -> {
          locationService.requestLocationPermission(this);
        })
        .setNegativeButton("Use Sample Data", (dialog, which) -> {
          showSampleArticles();
        })
        .setCancelable(false)
        .show();
  }

  private void fetchNewsBasedOnLocation() {
    showLoading(true);

    locationService.getCurrentLocation(new LocationService.LocationCallback() {
      @Override
      public void onLocationReceived(Location location) {
        currentLocation = location;
        updateLocationText(location);
        fetchNewsFromService(location);
      }

      @Override
      public void onLocationError(String error) {
        showLoading(false);
        Toast.makeText(ArticlesActivity.this, "Location error: " + error, Toast.LENGTH_SHORT).show();
        showSampleArticles();
      }

      @Override
      public void onPermissionDenied() {
        showLoading(false);
        showLocationPermissionDialog();
      }
    });
  }

  private void updateLocationText(Location location) {
    String locationString = String.format("News around %.2f, %.2f (50km radius)",
        location.getLatitude(), location.getLongitude());
    locationText.setText(locationString);
  }

  private void fetchNewsFromService(Location location) {
    // Use Gemini AI to generate live traffic news
    geminiNewsService.fetchLiveTrafficNews(location, new GeminiNewsService.GeminiNewsCallback() {
      @Override
      public void onNewsReceived(List<Article> articles) {
        runOnUiThread(() -> {
          showLoading(false);
          if (articles.isEmpty()) {
            showEmptyState();
          } else {
            showArticles(articles);
          }
        });
      }

      @Override
      public void onError(String error) {
        runOnUiThread(() -> {
          showLoading(false);
          Toast.makeText(ArticlesActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
          // Fallback to traditional news service
          fetchTraditionalNews(location);
        });
      }
    });
  }

  private void fetchTraditionalNews(Location location) {
    newsService.fetchLocalNews(location, new NewsService.NewsCallback() {
      @Override
      public void onNewsReceived(List<Article> articles) {
        runOnUiThread(() -> {
          showLoading(false);
          if (articles.isEmpty()) {
            showEmptyState();
          } else {
            showArticles(articles);
          }
        });
      }

      @Override
      public void onError(String error) {
        runOnUiThread(() -> {
          showLoading(false);
          Toast.makeText(ArticlesActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
          showSampleArticles();
        });
      }
    });
  }

  private void refreshNews() {
    if (currentLocation != null) {
      fetchNewsFromService(currentLocation);
    } else {
      checkLocationPermissionAndFetchNews();
    }
  }

  private void showLoading(boolean show) {
    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    articlesRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    emptyStateText.setVisibility(View.GONE);
    swipeRefreshLayout.setRefreshing(false);
  }

  private void showArticles(List<Article> articles) {
    articlesAdapter.updateArticles(articles);
    articlesRecyclerView.setVisibility(View.VISIBLE);
    emptyStateText.setVisibility(View.GONE);

    // Show AI indicator if articles contain AI-generated content
    boolean hasAIContent = articles.stream().anyMatch(article -> article.getTitle().contains("🚦") ||
        article.getTitle().contains("🔮") ||
        article.getTitle().contains("🚨") ||
        article.getTitle().contains("AI-Generated"));

    if (hasAIContent) {
      Toast.makeText(this, "🤖 AI-powered traffic insights loaded", Toast.LENGTH_SHORT).show();
    }
  }

  private void showEmptyState() {
    emptyStateText.setVisibility(View.VISIBLE);
    articlesRecyclerView.setVisibility(View.GONE);
    emptyStateText.setText("No recent news found in your area.\nPull down to refresh.");
  }

  private void showSampleArticles() {
    showArticles(getSampleArticles());
    locationText.setText("Sample News (Location not available)");
  }

  public void getLiveTrafficReport() {
    if (currentLocation != null) {
      showLoading(true);
      geminiNewsService.getLiveTrafficReport(currentLocation, new GeminiNewsService.GeminiNewsCallback() {
        @Override
        public void onNewsReceived(List<Article> articles) {
          runOnUiThread(() -> {
            showLoading(false);
            showArticles(articles);
          });
        }

        @Override
        public void onError(String error) {
          runOnUiThread(() -> {
            showLoading(false);
            Toast.makeText(ArticlesActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
          });
        }
      });
    } else {
      Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
    }
  }

  private void showPredictionDialog() {
    String[] timeFrames = { "Next 30 minutes", "Next hour", "Next 2 hours", "Next 4 hours", "Today" };

    new AlertDialog.Builder(this)
        .setTitle("Traffic Prediction")
        .setItems(timeFrames, (dialog, which) -> {
          getTrafficPrediction(timeFrames[which]);
        })
        .setNegativeButton("Cancel", null)
        .show();
  }

  public void getTrafficPrediction(String timeFrame) {
    if (currentLocation != null) {
      showLoading(true);
      geminiNewsService.getTrafficPrediction(currentLocation, timeFrame, new GeminiNewsService.GeminiNewsCallback() {
        @Override
        public void onNewsReceived(List<Article> articles) {
          runOnUiThread(() -> {
            showLoading(false);
            showArticles(articles);
          });
        }

        @Override
        public void onError(String error) {
          runOnUiThread(() -> {
            showLoading(false);
            Toast.makeText(ArticlesActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
          });
        }
      });
    } else {
      Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == 1001) { // LOCATION_PERMISSION_REQUEST_CODE
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        fetchNewsBasedOnLocation();
      } else {
        showSampleArticles();
        Toast.makeText(this, "Location permission denied. Showing sample data.", Toast.LENGTH_SHORT).show();
      }
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (locationService != null) {
      locationService.stopLocationUpdates();
    }
    if (newsService != null) {
      newsService.shutdown();
    }
    if (geminiNewsService != null) {
      geminiNewsService.shutdown();
    }
  }

  private List<Article> getSampleArticles() {
    List<Article> articles = new ArrayList<>();
    articles.add(new Article(
        "Major Traffic Jam on Main Street",
        "Due to ongoing construction work, expect delays of up to 30 minutes on Main Street.",
        "2 hours ago"));
    articles.add(new Article(
        "New Traffic Light Installation",
        "Traffic lights being installed at the intersection of 5th Avenue and Park Street.",
        "4 hours ago"));
    articles.add(new Article(
        "Road Closure Alert",
        "Bridge Street will be closed for maintenance from 10 PM to 6 AM.",
        "6 hours ago"));
    articles.add(new Article(
        "Traffic Pattern Changes",
        "New one-way traffic pattern implemented in downtown area.",
        "Yesterday"));
    return articles;
  }

  public static class Article {
    private String title;
    private String description;
    private String time;

    public Article(String title, String description, String time) {
      this.title = title;
      this.description = description;
      this.time = time;
    }

    public String getTitle() {
      return title;
    }

    public String getDescription() {
      return description;
    }

    public String getTime() {
      return time;
    }
  }
}