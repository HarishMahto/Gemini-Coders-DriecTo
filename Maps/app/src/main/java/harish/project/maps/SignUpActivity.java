package harish.project.maps;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButton;

public class SignUpActivity extends BaseActivity {
    private EditText nameEditText, phoneEditText, licenseEditText, passwordEditText, emailEditText;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        nameEditText = findViewById(R.id.editTextName);
        phoneEditText = findViewById(R.id.editTextPhone);
        licenseEditText = findViewById(R.id.editTextLicense);
        passwordEditText = findViewById(R.id.editTextPassword);
        emailEditText = findViewById(R.id.editTextEmail);

        // Animation setup
        ImageView logo = findViewById(R.id.logoImage);
        TextView title = findViewById(R.id.titleText);
        View divider = findViewById(R.id.titleDivider);
        TextInputLayout nameLayout = findViewById(R.id.nameInputLayout);
        TextInputLayout emailLayout = findViewById(R.id.emailInputLayout);
        TextInputLayout phoneLayout = findViewById(R.id.phoneInputLayout);
        TextInputLayout licenseLayout = findViewById(R.id.licenseInputLayout);
        TextInputLayout passwordLayout = findViewById(R.id.passwordInputLayout);
        MaterialButton doneBtn = findViewById(R.id.buttonDone);
        TextView alreadyAccount = findViewById(R.id.textAlreadyAccount);

        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        Animation popIn = AnimationUtils.loadAnimation(this, R.anim.pop_in);

        logo.startAnimation(fadeIn);
        title.startAnimation(slideUp);
        divider.startAnimation(slideUp);
        nameLayout.startAnimation(slideUp);
        emailLayout.startAnimation(slideUp);
        phoneLayout.startAnimation(slideUp);
        licenseLayout.startAnimation(slideUp);
        passwordLayout.startAnimation(slideUp);
        doneBtn.startAnimation(popIn);
        alreadyAccount.startAnimation(slideUp);

        findViewById(R.id.buttonDone).setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String license = licenseEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(license) ||
                TextUtils.isEmpty(password) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        User user = new User(name, phone, license, email);
                        // Save user data to Realtime Database
                        FirebaseDatabase.getInstance().getReference("users")
                                .child(uid)
                                .setValue(user)
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful()) {
                                        // Initialize user with 300 credits in Firestore
                                        initializeUserCredits(uid);
                                    } else {
                                        Toast.makeText(this,
                                                "Failed to save user data: " + task2.getException().getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        String errorMessage = "Registration failed";
                        if (task.getException() != null) {
                            if (task.getException() instanceof FirebaseAuthWeakPasswordException) {
                                errorMessage = "Password is too weak. Please use a stronger password.";
                            } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                errorMessage = "Invalid email format. Please check your email.";
                            } else {
                                errorMessage = "Registration failed: " + task.getException().getMessage();
                            }
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void initializeUserCredits(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        java.util.HashMap<String, Object> userData = new java.util.HashMap<>();
        userData.put("credits", 300);
        userData.put("totalCreditsEarned", 300);
        userData.put("creditsUsed", 0);
        userData.put("registrationDate", new java.util.Date());

        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registration successful! You have 300 credits to start.", Toast.LENGTH_LONG)
                            .show();
                    startActivity(new Intent(SignUpActivity.this, DashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Registration successful but failed to initialize credits: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    startActivity(new Intent(SignUpActivity.this, DashboardActivity.class));
                    finish();
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

    public static class User {
        public String name, phone, license, email;

        public User() {
        }

        public User(String name, String phone, String license, String email) {
            this.name = name;
            this.phone = phone;
            this.license = license;
            this.email = email;
        }
    }
}