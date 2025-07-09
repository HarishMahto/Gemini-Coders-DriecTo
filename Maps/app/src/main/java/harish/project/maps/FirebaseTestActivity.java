package harish.project.maps;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;

public class FirebaseTestActivity extends AppCompatActivity {
  private TextView statusText;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_firebase_test);

    statusText = findViewById(R.id.statusText);

    testFirebaseConnection();
  }

  private void testFirebaseConnection() {
    statusText.setText("Testing Firebase connection...");

    // Test Firebase App initialization
    try {
      FirebaseApp app = FirebaseApp.getInstance();
      statusText.append("\n✓ Firebase App initialized");
    } catch (Exception e) {
      statusText.append("\n✗ Firebase App failed: " + e.getMessage());
      return;
    }

    // Test Firebase Auth
    try {
      FirebaseAuth auth = FirebaseAuth.getInstance();
      statusText.append("\n✓ Firebase Auth initialized");

      // Check if user is authenticated
      if (auth.getCurrentUser() != null) {
        statusText.append("\n✓ User is authenticated: " + auth.getCurrentUser().getEmail());
        testDatabaseWrite();
      } else {
        statusText.append("\n⚠ User not authenticated - testing anonymous auth");
        testAnonymousAuth();
      }
    } catch (Exception e) {
      statusText.append("\n✗ Firebase Auth failed: " + e.getMessage());
    }
  }

  private void testAnonymousAuth() {
    FirebaseAuth auth = FirebaseAuth.getInstance();
    auth.signInAnonymously()
        .addOnSuccessListener(authResult -> {
          statusText.append("\n✓ Anonymous authentication successful");
          testDatabaseWrite();
        })
        .addOnFailureListener(e -> {
          statusText.append("\n✗ Anonymous authentication failed: " + e.getMessage());
          statusText.append("\n\nTo fix this:");
          statusText.append("\n1. Go to Firebase Console");
          statusText.append("\n2. Enable Anonymous Authentication");
          statusText.append("\n3. Update Database Rules (see database.rules.json)");
        });
  }

  private void testDatabaseWrite() {
    try {
      FirebaseDatabase database = FirebaseDatabase.getInstance();
      DatabaseReference ref = database.getReference("test");
      statusText.append("\n✓ Firebase Database initialized");

      // Test write operation
      ref.child("connection_test").setValue("success")
          .addOnSuccessListener(aVoid -> {
            statusText.append("\n✓ Database write successful");
            statusText.append("\n\n🎉 All Firebase services working correctly!");
            Toast.makeText(this, "Firebase connection test successful!", Toast.LENGTH_SHORT).show();
          })
          .addOnFailureListener(e -> {
            statusText.append("\n✗ Database write failed: " + e.getMessage());
            statusText.append("\n\nTo fix this:");
            statusText.append("\n1. Go to Firebase Console → Realtime Database");
            statusText.append("\n2. Go to Rules tab");
            statusText.append("\n3. Replace rules with the content from database.rules.json");
            Toast.makeText(this, "Database rules need to be updated!", Toast.LENGTH_LONG).show();
          });
    } catch (Exception e) {
      statusText.append("\n✗ Firebase Database failed: " + e.getMessage());
    }
  }
}