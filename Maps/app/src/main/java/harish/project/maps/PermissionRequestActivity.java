package harish.project.maps;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import harish.project.maps.utils.PermissionUtils;

public class PermissionRequestActivity extends AppCompatActivity {

  private TextView titleTextView;
  private TextView descriptionTextView;
  private Button grantPermissionButton;
  private Button skipButton;
  private String targetActivity;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_permission_request);

    // Get target activity from intent
    targetActivity = getIntent().getStringExtra("target_activity");

    // Initialize views
    titleTextView = findViewById(R.id.titleTextView);
    descriptionTextView = findViewById(R.id.descriptionTextView);
    grantPermissionButton = findViewById(R.id.grantPermissionButton);
    skipButton = findViewById(R.id.skipButton);

    // Set up button listeners
    grantPermissionButton.setOnClickListener(v -> requestPermission());
    skipButton.setOnClickListener(v -> skipPermission());

    // Update UI based on permission status
    updateUI();
  }

  private void updateUI() {
    if (PermissionUtils.hasFilePermissions(this)) {
      titleTextView.setText("Permission Already Granted");
      descriptionTextView
          .setText("You already have file access permission. You can now select images from your device.");
      grantPermissionButton.setText("Continue");
      grantPermissionButton.setOnClickListener(v -> proceedToTarget());
    } else {
      titleTextView.setText("File Access Permission Required");
      descriptionTextView.setText(
          "This app needs access to your files to select images for license plate recognition. This permission is required for the best experience.");
      grantPermissionButton.setText("Grant Permission");
      grantPermissionButton.setOnClickListener(v -> requestPermission());
    }
  }

  private void requestPermission() {
    PermissionUtils.requestFilePermissions(this);
  }

  private void skipPermission() {
    Toast.makeText(this, "You can still use the camera to take photos", Toast.LENGTH_LONG).show();
    proceedToTarget();
  }

  private void proceedToTarget() {
    if (targetActivity != null) {
      try {
        Class<?> targetClass = Class.forName(targetActivity);
        Intent intent = new Intent(this, targetClass);
        startActivity(intent);
        finish();
      } catch (ClassNotFoundException e) {
        // Fallback to LicensePlateActivity
        Intent intent = new Intent(this, LicensePlateActivity.class);
        startActivity(intent);
        finish();
      }
    } else {
      // Default fallback
      Intent intent = new Intent(this, LicensePlateActivity.class);
      startActivity(intent);
      finish();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Check if permission was granted while we were away
    if (PermissionUtils.hasFilePermissions(this)) {
      updateUI();
    }
  }
}