package harish.project.maps;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends BaseActivity {
    private EditText emailEditText, passwordEditText;
    private FirebaseAuth mAuth;
    private static final int RC_SIGN_IN = 1001;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);

        // Google Sign-In options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Animation setup
        ImageView logo = findViewById(R.id.logoImage);
        TextView title = findViewById(R.id.titleText);
        View divider = findViewById(R.id.titleDivider);
        TextInputLayout emailLayout = findViewById(R.id.emailInputLayout);
        TextInputLayout passwordLayout = findViewById(R.id.passwordInputLayout);
        TextView forgot = findViewById(R.id.textForgotPassword);
        MaterialButton signInBtn = findViewById(R.id.buttonSignIn);
        MaterialButton googleBtn = findViewById(R.id.buttonGoogleSignIn);
        TextView orDivider = findViewById(R.id.orDivider);
        TextView createAccount = findViewById(R.id.textCreateAccount);

        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        Animation popIn = AnimationUtils.loadAnimation(this, R.anim.pop_in);

        logo.startAnimation(fadeIn);
        title.startAnimation(slideUp);
        divider.startAnimation(slideUp);
        emailLayout.startAnimation(slideUp);
        passwordLayout.startAnimation(slideUp);
        forgot.startAnimation(slideUp);
        signInBtn.startAnimation(popIn);
        googleBtn.startAnimation(popIn);
        orDivider.startAnimation(fadeIn);
        createAccount.startAnimation(slideUp);

        findViewById(R.id.buttonSignIn).setOnClickListener(v -> loginUser());
        findViewById(R.id.textCreateAccount).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });
        findViewById(R.id.buttonGoogleSignIn).setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                        .getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Check if user has credits initialized
                        checkAndInitializeCredits();
                    } else {
                        String errorMessage = "Login failed";
                        if (task.getException() != null) {
                            if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                                errorMessage = "User not found. Please check your email or sign up.";
                            } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                errorMessage = "Invalid password. Please try again.";
                            } else {
                                errorMessage = "Login failed: " + task.getException().getMessage();
                            }
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkAndInitializeCredits() {
        String uid = mAuth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("credits")) {
                        // User already has credits, proceed to dashboard
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                        finish();
                    } else {
                        // User doesn't have credits, initialize them
                        initializeUserCredits(uid);
                    }
                })
                .addOnFailureListener(e -> {
                    // If there's an error checking, initialize credits anyway
                    initializeUserCredits(uid);
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
                    Toast.makeText(this, "Login successful! You have 300 credits to start.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Login successful but failed to initialize credits: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
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

    private void firebaseAuthWithGoogle(String idToken) {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Check if user has credits initialized
                        checkAndInitializeCredits();
                    } else {
                        String errorMessage = "Google authentication failed";
                        if (task.getException() != null) {
                            errorMessage = "Google authentication failed: " + task.getException().getMessage();
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}