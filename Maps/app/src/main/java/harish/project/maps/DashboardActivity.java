package harish.project.maps;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.content.Context;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import android.widget.ImageButton;

import android.Manifest;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends BaseActivity {
  private RecyclerView articlesRecyclerView;
  private ArticlesAdapter articlesAdapter;
  private BottomNavigationView bottomNav;
  private FloatingActionButton sosButton;
  private FloatingActionButton fabAccident, fabMedical, fabFire, fabOther;
  private boolean fabOptionsVisible = false;
  private String selectedEmergencyType = null;
  private FrameLayout lottieDialogContainer;
  private LottieAnimationView lottieSuccess;
  private TextView tvSuccess;
  private Vibrator vibrator;
  private VibratorManager vibratorManager;
  private FusedLocationProviderClient fusedLocationClient;
  private Location lastLocation;
  private ActivityResultLauncher<Intent> cameraLauncher;
  private Bitmap capturedBitmap;
  private TextView tvAccident, tvMedical, tvFire, tvOther;
  private ObjectAnimator sosPulseAnimator;
  private com.google.android.material.card.MaterialCardView cardAccident, cardMedical, cardFire, cardOther;
  private ProgressDialog loadingDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_dashboard);

    // Initialize views
    articlesRecyclerView = findViewById(R.id.articlesRecyclerView);
    bottomNav = findViewById(R.id.bottom_navigation);
    sosButton = findViewById(R.id.sosButton);
    ImageButton notificationButton = findViewById(R.id.notificationButton);
    fabAccident = findViewById(R.id.fab_accident);
    fabMedical = findViewById(R.id.fab_medical);
    fabFire = findViewById(R.id.fab_fire);
    fabOther = findViewById(R.id.fab_other);
    lottieDialogContainer = findViewById(R.id.lottie_dialog_container);
    lottieSuccess = findViewById(R.id.lottie_success);
    tvSuccess = findViewById(R.id.tv_success);
    tvAccident = findViewById(R.id.tv_accident);
    tvMedical = findViewById(R.id.tv_medical);
    tvFire = findViewById(R.id.tv_fire);
    tvOther = findViewById(R.id.tv_other);
    cardAccident = findViewById(R.id.card_accident);
    cardMedical = findViewById(R.id.card_medical);
    cardFire = findViewById(R.id.card_fire);
    cardOther = findViewById(R.id.card_other);

    // Initialize vibrator based on Android version
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      vibratorManager = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
      vibrator = vibratorManager.getDefaultVibrator();
    } else {
      vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    // Setup articles RecyclerView
    articlesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    articlesAdapter = new ArticlesAdapter(getSampleArticles());
    articlesRecyclerView.setAdapter(articlesAdapter);

    // Setup notification button
    notificationButton.setOnClickListener(v -> {
      // TODO: Implement notification center
      startActivity(new Intent(this, SettingsActivity.class));
      // Toast.makeText(this, "Notifications coming soon!",
      // Toast.LENGTH_SHORT).show();
    });

    // Setup bottom navigation
    bottomNav.setOnItemSelectedListener(item -> {
      int itemId = item.getItemId();
      if (itemId == R.id.nav_store) {
        startActivity(new Intent(this, CreditStoreActivity.class));
        return true;
      } else if (itemId == R.id.nav_voucher) {
        startActivity(new Intent(this, MyVoucherActivity.class));
        return true;
      } else if (itemId == R.id.nav_account) {
        startActivity(new Intent(this, AccountActivity.class));
        return true;
      } else if (itemId == R.id.nav_violation_history) {
        startActivity(new Intent(this, ViolationHistoryActivity.class));
        return true;
      }
      return false;
    });

    // Add long press listener for violation history to load sample data
    bottomNav.findViewById(R.id.nav_violation_history).setOnLongClickListener(v -> {
      SampleDataHelper.addSampleViolations();
      SampleDataHelper.addSampleLicensePlates();
      Toast.makeText(this, "Sample violation data loaded! Check violation history.", Toast.LENGTH_LONG).show();
      return true;
    });

    // Setup SOS button
    sosButton.setOnClickListener(v -> toggleEmergencyFabs());
    fabAccident.setOnClickListener(v -> onEmergencyTypeSelected("Accident"));
    fabMedical.setOnClickListener(v -> onEmergencyTypeSelected("Medical"));
    fabFire.setOnClickListener(v -> onEmergencyTypeSelected("Fire"));
    fabOther.setOnClickListener(v -> onEmergencyTypeSelected("Other"));

    // Setup feature cards and click listeners
    setupFeatureCards();

    // Pulse animation for SOS button
    startSosPulse();

    // Camera launcher
    cameraLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
          if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Bundle extras = result.getData().getExtras();
            if (extras != null) {
              Bitmap imageBitmap = (Bitmap) extras.get("data");
              if (imageBitmap != null) {
                capturedBitmap = imageBitmap;
                // Show preview and confirmation dialog
                showImagePreviewDialog(imageBitmap);
              } else {
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
              }
            } else {
              Toast.makeText(this, "No image data received", Toast.LENGTH_SHORT).show();
            }
          } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Camera cancelled", Toast.LENGTH_SHORT).show();
          } else {
            Toast.makeText(this, "Camera error occurred", Toast.LENGTH_SHORT).show();
          }
        });

    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
  }

  private void setupFeatureCards() {
    // Live Traffic Card
    CardView liveTrafficCard = findViewById(R.id.liveTrafficCard);
    liveTrafficCard.setOnClickListener(v -> {
      startActivity(new Intent(this, MainActivity.class));
    });

    // Analytics Card
    CardView analyticsCard = findViewById(R.id.trafficAnalyticsCard);
    analyticsCard.setOnClickListener(v -> {
      startActivity(new Intent(this, AnalyticsActivity.class));
    });

    // Settings Card
    CardView settingsCard = findViewById(R.id.settingsCard);
    settingsCard.setOnClickListener(v -> {
      startActivity(new Intent(this, LicensePlateActivity.class));
    });

    // See More Text
    TextView seeMoreText = findViewById(R.id.seeMoreText);
    seeMoreText.setOnClickListener(v -> {
      startActivity(new Intent(this, ArticlesActivity.class));
    });

    /*
     * Traffic Update Card
     * CardView trafficUpdateCard = findViewById(R.id.trafficUpdateCard);
     * trafficUpdateCard.setOnClickListener(v -> {
     * startActivity(new Intent(this, ArticlesActivity.class));
     * });
     */
  }

  private void toggleEmergencyFabs() {
    if (!fabOptionsVisible) {
      showEmergencyFabs();
    } else {
      hideEmergencyFabs();
    }
  }

  private void showEmergencyFabs() {
    fabOptionsVisible = true;
    animateFabWithCard(fabAccident, cardAccident, -300, 0); // Leftmost (180°)
    animateFabWithCard(fabMedical, cardMedical, 150, -260); // Top-left (120°)
    animateFabWithCard(fabFire, cardFire, -150, -260); // Top (60°)
    animateFabWithCard(fabOther, cardOther, 300, 0); // Topmost (0°)
    startSosPulse();
  }

  private void hideEmergencyFabs() {
    fabOptionsVisible = false;
    animateFabWithCard(fabAccident, cardAccident, 0, 0);
    animateFabWithCard(fabMedical, cardMedical, 0, 0);
    animateFabWithCard(fabFire, cardFire, 0, 0);
    animateFabWithCard(fabOther, cardOther, 0, 0);
    stopSosPulse();
  }

  private void animateFabWithCard(FloatingActionButton fab, View card, float translationX, float translationY) {
    fab.setVisibility(View.VISIBLE);
    card.setVisibility(View.VISIBLE);
    AnimatorSet set = new AnimatorSet();
    set.playTogether(
        ObjectAnimator.ofFloat(fab, "translationX", translationX),
        ObjectAnimator.ofFloat(fab, "translationY", translationY),
        ObjectAnimator.ofFloat(fab, "alpha", translationX == 0 && translationY == 0 ? 0f : 1f),
        ObjectAnimator.ofFloat(card, "translationX", translationX),
        ObjectAnimator.ofFloat(card, "translationY", translationY),
        ObjectAnimator.ofFloat(card, "alpha", translationX == 0 && translationY == 0 ? 0f : 1f));
    set.setDuration(300);
    set.start();
    if (translationX == 0 && translationY == 0) {
      fab.postDelayed(() -> {
        fab.setVisibility(View.GONE);
        card.setVisibility(View.GONE);
        fab.setTranslationX(0f);
        fab.setTranslationY(0f);
        fab.setAlpha(1f);
        card.setTranslationX(0f);
        card.setTranslationY(0f);
        card.setAlpha(1f);
      }, 300);
    }
  }

  private void onEmergencyTypeSelected(String type) {
    selectedEmergencyType = type;
    hideEmergencyFabs();
    // Check permissions
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, 1001);
    } else {
      launchCamera();
    }
  }

  private void launchCamera() {
    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    cameraLauncher.launch(cameraIntent);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      launchCamera();
    } else if (requestCode == 1002 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      getLocationAndUpload();
    } else {
      Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show();
    }
  }

  private void getLocationAndUpload() {
    // Check network connectivity
    if (!isNetworkAvailable()) {
      Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
      return;
    }

    if (ContextCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 1002);
      return;
    }

    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
      lastLocation = location;
      uploadImageToFirebase(capturedBitmap, selectedEmergencyType, location);
    }).addOnFailureListener(e -> {
      Toast.makeText(this, "Location unavailable: " + e.getMessage(), Toast.LENGTH_SHORT).show();
      // Upload without location
      uploadImageToFirebase(capturedBitmap, selectedEmergencyType, null);
    });
  }

  private boolean isNetworkAvailable() {
    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    if (connectivityManager != null) {
      NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
      return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    return false;
  }

  private void uploadImageToFirebase(Bitmap bitmap, String type, Location location) {
    if (bitmap == null) {
      Toast.makeText(this, "No image captured", Toast.LENGTH_SHORT).show();
      return;
    }

    // Show loading state
    showLoadingDialog("Sending emergency alert...");

    String userId = FirebaseAuth.getInstance().getCurrentUser() != null
        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
        : "anonymous";
    String alertId = FirebaseDatabase.getInstance().getReference().push().getKey();
    String timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date());

    // Create a simpler storage reference path
    String fileName = "emergency_" + System.currentTimeMillis() + ".jpg";
    StorageReference storageRef = FirebaseStorage.getInstance().getReference()
        .child("emergency_images").child(fileName);

    // Compress image with better quality
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
    byte[] data = baos.toByteArray();

    // Create metadata for the upload
    StorageMetadata metadata = new StorageMetadata.Builder()
        .setContentType("image/jpeg")
        .build();

    // Upload to Firebase Storage with metadata
    storageRef.putBytes(data, metadata)
        .addOnSuccessListener(taskSnapshot -> {
          updateLoadingProgress("Getting download URL...");
          // Get download URL
          storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            updateLoadingProgress("Saving alert data...");
            // Save alert data to Realtime Database
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("type", type);
            alertData.put("imageUrl", uri.toString());
            alertData.put("timestamp", timeStamp);
            alertData.put("userId", userId);
            alertData.put("status", "active");
            alertData.put("alertId", alertId);

            // Location data
            Map<String, Object> loc = new HashMap<>();
            if (location != null) {
              loc.put("lat", location.getLatitude());
              loc.put("lng", location.getLongitude());
              loc.put("accuracy", location.getAccuracy());
            } else {
              loc.put("lat", 0.0);
              loc.put("lng", 0.0);
              loc.put("accuracy", 0.0);
            }
            alertData.put("location", loc);

            // Save to database with simpler path
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference()
                .child("emergency_alerts").child(alertId);
            dbRef.setValue(alertData)
                .addOnSuccessListener(aVoid -> {
                  hideLoadingDialog();
                  showSuccessDialog();
                  // Vibrate to confirm
                  if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                      vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                      vibrator.vibrate(500);
                    }
                  }
                })
                .addOnFailureListener(e -> {
                  hideLoadingDialog();
                  Toast.makeText(this, "Failed to save alert: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
          }).addOnFailureListener(e -> {
            hideLoadingDialog();
            Toast.makeText(this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
          });
        })
        .addOnFailureListener(e -> {
          hideLoadingDialog();
          String errorMessage = "Failed to upload image";
          if (e.getMessage() != null) {
            if (e.getMessage().contains("does not present at location")) {
              errorMessage = "Storage permission denied. Please check Firebase Storage rules.";
            } else {
              errorMessage = "Upload failed: " + e.getMessage();
            }
          }
          Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        })
        .addOnProgressListener(snapshot -> {
          // Show upload progress
          double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
          updateLoadingProgress("Uploading image... " + (int) progress + "%");
        });
  }

  private void showSuccessDialog() {
    lottieDialogContainer.setVisibility(View.VISIBLE);
    lottieSuccess.playAnimation();
    tvSuccess.setText("Emergency alert sent. Help is on the way!");
    lottieDialogContainer.postDelayed(() -> {
      lottieDialogContainer.setVisibility(View.GONE);
      lottieSuccess.cancelAnimation();
    }, 2500);
  }

  private List<Article> getSampleArticles() {
    List<Article> articles = new ArrayList<>();
    articles.add(new Article(
        "Traffic Alert: Major Road Closure",
        "Due to ongoing construction work, Main Street will be closed for the next 3 days.",
        "2 hours ago"));
    articles.add(new Article(
        "Weather Warning: Heavy Rain Expected",
        "Heavy rainfall is expected in the next 24 hours. Please plan your journey accordingly.",
        "5 hours ago"));
    return articles;
  }

  // Article class for RecyclerView items
  private static class Article {
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

  // ArticlesAdapter class for RecyclerView
  private static class ArticlesAdapter extends RecyclerView.Adapter<ArticlesAdapter.ArticleViewHolder> {
    private List<Article> articles;

    public ArticlesAdapter(List<Article> articles) {
      this.articles = articles;
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.item_article, parent, false);
      return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
      Article article = articles.get(position);
      holder.titleTextView.setText(article.getTitle());
      holder.descriptionTextView.setText(article.getDescription());
      holder.timeTextView.setText(article.getTime());
    }

    @Override
    public int getItemCount() {
      return articles.size();
    }

    static class ArticleViewHolder extends RecyclerView.ViewHolder {
      TextView titleTextView;
      TextView descriptionTextView;
      TextView timeTextView;

      public ArticleViewHolder(@NonNull View itemView) {
        super(itemView);
        titleTextView = itemView.findViewById(R.id.articleTitle);
        descriptionTextView = itemView.findViewById(R.id.articleDescription);
        timeTextView = itemView.findViewById(R.id.articleTime);
      }
    }
  }

  private void startSosPulse() {
    if (sosPulseAnimator == null) {
      sosPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
          sosButton,
          PropertyValuesHolder.ofFloat("scaleX", 1f, 1.2f, 1f),
          PropertyValuesHolder.ofFloat("scaleY", 1f, 1.2f, 1f));
      sosPulseAnimator.setDuration(1000);
      sosPulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
    }
    sosPulseAnimator.start();
  }

  private void stopSosPulse() {
    if (sosPulseAnimator != null && sosPulseAnimator.isRunning()) {
      sosPulseAnimator.cancel();
      sosButton.setScaleX(1f);
      sosButton.setScaleY(1f);
    }
  }

  private void showLoadingDialog(String message) {
    if (loadingDialog == null) {
      loadingDialog = new ProgressDialog(this);
      loadingDialog.setCancelable(false);
      loadingDialog.setCanceledOnTouchOutside(false);
    }
    loadingDialog.setMessage(message);
    loadingDialog.show();
  }

  private void updateLoadingProgress(String message) {
    if (loadingDialog != null && loadingDialog.isShowing()) {
      loadingDialog.setMessage(message);
    }
  }

  private void hideLoadingDialog() {
    if (loadingDialog != null && loadingDialog.isShowing()) {
      loadingDialog.dismiss();
    }
  }

  private void showImagePreviewDialog(Bitmap bitmap) {
    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
    View dialogView = getLayoutInflater().inflate(R.layout.dialog_image_preview, null);

    ImageView imagePreview = dialogView.findViewById(R.id.imagePreview);
    TextView emergencyTypeText = dialogView.findViewById(R.id.emergencyTypeText);

    imagePreview.setImageBitmap(bitmap);
    emergencyTypeText.setText("Emergency Type: " + selectedEmergencyType);

    builder.setView(dialogView)
        .setTitle("Confirm Emergency Alert")
        .setMessage("Review the captured image and confirm to send the emergency alert.")
        .setPositiveButton("Send Alert", (dialog, which) -> {
          getLocationAndUpload();
        })
        .setNegativeButton("Retake Photo", (dialog, which) -> {
          launchCamera();
        })
        .setCancelable(false)
        .show();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    hideLoadingDialog();
  }
}