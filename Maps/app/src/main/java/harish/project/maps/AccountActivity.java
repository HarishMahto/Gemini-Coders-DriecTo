package harish.project.maps;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AccountActivity extends AppCompatActivity {

  private TextInputEditText etName, etEmail, etPhone, etLicense;
  private RadioGroup rgVehicleType;
  private RadioButton rbCng, rbFuel, rbElectric;
  private MaterialButton btnEdit, btnSave, btnLogout;
  private LottieAnimationView lottieVehicleType, lottieSuccess;

  private DatabaseReference dbRef;
  private FirebaseUser user;

  private boolean isEditing = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_account);

    etName = findViewById(R.id.etName);
    etEmail = findViewById(R.id.etEmail);
    etPhone = findViewById(R.id.etPhone);
    etLicense = findViewById(R.id.etLicense);
    rgVehicleType = findViewById(R.id.rgVehicleType);
    rbCng = findViewById(R.id.rbCng);
    rbFuel = findViewById(R.id.rbFuel);
    rbElectric = findViewById(R.id.rbElectric);
    btnEdit = findViewById(R.id.btnEdit);
    btnSave = findViewById(R.id.btnSave);
    btnLogout = findViewById(R.id.btnLogout);
    lottieVehicleType = findViewById(R.id.lottieVehicleType);
    lottieSuccess = findViewById(R.id.lottieSuccess);

    dbRef = FirebaseDatabase.getInstance().getReference("users");
    user = FirebaseAuth.getInstance().getCurrentUser();

    fetchUserData();

    btnEdit.setOnClickListener(v -> animateEditMode(true));
    btnSave.setOnClickListener(v -> saveUserData());

    rgVehicleType.setOnCheckedChangeListener((group, checkedId) -> {
      if (isEditing) {
        animateVehicleTypeLottie();
        switchThemeAccordingToVehicle();
      }
    });

    // Add logout functionality
    btnLogout.setOnClickListener(v -> logoutUser());
  }

  private void logoutUser() {
    FirebaseAuth.getInstance().signOut();
    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
  }

  private void fetchUserData() {
    if (user == null) {
      Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
      return;
    }
    String uid = user.getUid();

    dbRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot snapshot) {
        if (snapshot.exists()) {
          String name = snapshot.child("name").getValue(String.class);
          String email = snapshot.child("email").getValue(String.class);
          String phone = snapshot.child("phone").getValue(String.class);
          String license = snapshot.child("license").getValue(String.class);
          String vehicleType = snapshot.child("vehicleType").getValue(String.class);

          if (name != null)
            etName.setText(name);
          if (email != null)
            etEmail.setText(email);
          if (phone != null)
            etPhone.setText(phone);
          if (license != null)
            etLicense.setText(license);

          if (vehicleType != null) {
            switch (vehicleType) {
              case "CNG":
                rbCng.setChecked(true);
                break;
              case "Fuel":
                rbFuel.setChecked(true);
                break;
              case "Electric":
                rbElectric.setChecked(true);
                break;
            }
            animateVehicleTypeLottie();
            switchThemeAccordingToVehicle();
          }
        } else {
          Toast.makeText(AccountActivity.this, "No user data found", Toast.LENGTH_SHORT).show();
        }
      }

      @Override
      public void onCancelled(@NonNull DatabaseError error) {
        Toast.makeText(AccountActivity.this, "Failed to fetch data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
      }
    });
  }

  // Animate enabling/disabling fields and button transitions
  private void animateEditMode(boolean editing) {
    isEditing = editing;
    btnEdit.animate().alpha(editing ? 0f : 1f).setDuration(200)
        .withEndAction(() -> btnEdit.setVisibility(editing ? View.GONE : View.VISIBLE));
    btnSave.setVisibility(View.VISIBLE);
    btnSave.animate().alpha(editing ? 1f : 0f).setDuration(200)
        .withEndAction(() -> btnSave.setVisibility(editing ? View.VISIBLE : View.GONE));
    etName.setEnabled(editing);
    etPhone.setEnabled(editing);
    etLicense.setEnabled(editing);
    rbCng.setEnabled(editing);
    rbFuel.setEnabled(editing);
    rbElectric.setEnabled(editing);
    // Animate field backgrounds for focus
    if (editing) {
      etName.requestFocus();
    }
  }

  private void setEditing(boolean editing) {
    isEditing = editing;
    etName.setEnabled(editing);
    etPhone.setEnabled(editing);
    etLicense.setEnabled(editing);
    rbCng.setEnabled(editing);
    rbFuel.setEnabled(editing);
    rbElectric.setEnabled(editing);
    btnSave.setEnabled(editing);
    btnEdit.setEnabled(!editing);
    // Animate button visibility
    btnEdit.setVisibility(editing ? View.GONE : View.VISIBLE);
    btnSave.setVisibility(editing ? View.VISIBLE : View.GONE);
  }

  private void saveUserData() {
    if (user == null) {
      Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
      return;
    }
    String uid = user.getUid();

    String name = etName.getText().toString().trim();
    String phone = etPhone.getText().toString().trim();
    String license = etLicense.getText().toString().trim();
    String vehicleType = getSelectedVehicleType();

    if (name.isEmpty() || phone.isEmpty() || license.isEmpty() || vehicleType == null) {
      Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
      return;
    }

    // Validate phone number (basic validation)
    if (phone.length() < 10) {
      Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
      return;
    }

    // Validate license number (basic validation)
    if (license.length() < 5) {
      Toast.makeText(this, "Please enter a valid license number", Toast.LENGTH_SHORT).show();
      return;
    }

    // Show loading state
    btnSave.setEnabled(false);
    btnSave.setText("Saving...");

    Map<String, Object> userData = new HashMap<>();
    userData.put("name", name);
    userData.put("email", user.getEmail());
    userData.put("phone", phone);
    userData.put("license", license);
    userData.put("vehicleType", vehicleType);
    userData.put("lastUpdated", System.currentTimeMillis());

    dbRef.child(uid).setValue(userData)
        .addOnSuccessListener(aVoid -> {
          showSuccessAnimation();
          setEditing(false);
          switchThemeAccordingToVehicle();
          // Reset button state
          btnSave.setEnabled(true);
          btnSave.setText("Save");
          Toast.makeText(AccountActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
        })
        .addOnFailureListener(e -> {
          Toast.makeText(AccountActivity.this, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
          // Reset button state
          btnSave.setEnabled(true);
          btnSave.setText("Save");
        });
  }

  private String getSelectedVehicleType() {
    int checkedId = rgVehicleType.getCheckedRadioButtonId();
    if (checkedId == R.id.rbCng)
      return "CNG";
    if (checkedId == R.id.rbFuel)
      return "Fuel";
    if (checkedId == R.id.rbElectric)
      return "Electric";
    return null;
  }

  // Animate the Lottie view for vehicle type
  private void animateVehicleTypeLottie() {
    if (rbCng.isChecked()) {
      lottieVehicleType.setAnimation("traffic_animation.json"); // Use a green-themed animation if available
      lottieVehicleType.playAnimation();
    } else if (rbFuel.isChecked()) {
      lottieVehicleType.setAnimation("traffic_animation.json"); // Use an orange-themed animation if available
      lottieVehicleType.playAnimation();
    } else if (rbElectric.isChecked()) {
      lottieVehicleType.setAnimation("traffic_animation.json"); // Use a blue-themed animation if available
      lottieVehicleType.playAnimation();
    }
  }

  // Show Lottie success animation on save
  private void showSuccessAnimation() {
    lottieSuccess.setVisibility(View.VISIBLE);
    lottieSuccess.playAnimation();
    lottieSuccess.postDelayed(() -> lottieSuccess.setVisibility(View.GONE), 1500);
  }

  private void switchThemeAccordingToVehicle() {
    String vehicleType = getSelectedVehicleType();
    if (vehicleType == null)
      return;
    switch (vehicleType) {
      case "CNG":
        ThemeManager.setTheme(this, ThemeManager.THEME_GREEN);
        ThemeManager.applyTheme(this, ThemeManager.Theme.GREEN);
        break;
      case "Fuel":
        ThemeManager.setTheme(this, ThemeManager.THEME_ORANGE);
        ThemeManager.applyTheme(this, ThemeManager.Theme.ORANGE);
        break;
      case "Electric":
        ThemeManager.setTheme(this, ThemeManager.THEME_BLUE);
        ThemeManager.applyTheme(this, ThemeManager.Theme.BLUE);
        break;
    }
  }
}