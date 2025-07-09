package harish.project.maps;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import android.util.Log;

public class SampleDataHelper {

  public static void addSampleLicensePlates() {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String userId = FirebaseAuth.getInstance().getCurrentUser() != null
        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
        : "sample_user";

    // Sample license plate submissions
    addSampleLicensePlate(db, userId, "MH12AB1234", "Helmet", "Mumbai, Maharashtra", "Verified", -7);
    addSampleLicensePlate(db, userId, "DL01CD5678", "Wrong Parking", "New Delhi, Delhi", "Pending", -3);
    addSampleLicensePlate(db, userId, "KA02EF9012", "Triple Seat", "Bangalore, Karnataka", "Verified", -1);
    addSampleLicensePlate(db, userId, "TN03GH3456", "Using Mobile", "Chennai, Tamil Nadu", "Rejected", -5);
    addSampleLicensePlate(db, userId, "AP04IJ7890", "Lane Changing", "Hyderabad, Telangana", "Verified", -2);
    addSampleLicensePlate(db, userId, "KL05KL1234", "Seat Belt", "Kochi, Kerala", "Pending", -4);
    addSampleLicensePlate(db, userId, "GJ06MN5678", "FootPath Driving", "Ahmedabad, Gujarat", "Verified", -6);
    addSampleLicensePlate(db, userId, "UP07OP9012", "Driving in Center", "Lucknow, Uttar Pradesh", "Pending", -8);
  }

  private static void addSampleLicensePlate(FirebaseFirestore db, String userId, String plateNumber,
      String violationType, String location, String status, int daysAgo) {
    // Create a date that is 'daysAgo' days in the past
    Date timestamp = new Date(System.currentTimeMillis() + (daysAgo * 24 * 60 * 60 * 1000L));

    Map<String, Object> licensePlate = new HashMap<>();
    licensePlate.put("plateNumber", plateNumber);
    licensePlate.put("timestamp", timestamp);
    licensePlate.put("imageUrl", "https://example.com/sample_image.jpg"); // Placeholder image URL
    licensePlate.put("violationType", violationType);
    licensePlate.put("location", location);
    licensePlate.put("status", status);
    licensePlate.put("userId", userId);

    db.collection("license_plates")
        .add(licensePlate)
        .addOnSuccessListener(documentReference -> {
          System.out.println("Sample license plate added: " + plateNumber);
        })
        .addOnFailureListener(e -> {
          System.err.println("Failed to add sample license plate: " + e.getMessage());
        });
  }

  public static void addSampleViolations() {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String userId = FirebaseAuth.getInstance().getCurrentUser() != null
        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
        : "sample_user";

    Log.d("SampleDataHelper", "Adding sample violations for user: " + userId);

    // Sample violations for the violations collection
    addSampleViolation(db, userId, "Mumbai, Maharashtra", "Verified", -7);
    addSampleViolation(db, userId, "New Delhi, Delhi", "Pending", -3);
    addSampleViolation(db, userId, "Bangalore, Karnataka", "Verified", -1);
    addSampleViolation(db, userId, "Chennai, Tamil Nadu", "Rejected", -5);
    addSampleViolation(db, userId, "Hyderabad, Telangana", "Verified", -2);
    addSampleViolation(db, userId, "Kochi, Kerala", "Pending", -4);
    addSampleViolation(db, userId, "Ahmedabad, Gujarat", "Verified", -6);
    addSampleViolation(db, userId, "Lucknow, Uttar Pradesh", "Pending", -8);
  }

  private static void addSampleViolation(FirebaseFirestore db, String userId, String location,
      String status, int daysAgo) {
    long timestamp = System.currentTimeMillis() + (daysAgo * 24 * 60 * 60 * 1000L);

    Map<String, Object> violation = new HashMap<>();
    violation.put("location", location);
    violation.put("timestamp", timestamp);
    violation.put("status", status);
    violation.put("userId", userId);

    db.collection("violations")
        .add(violation)
        .addOnSuccessListener(documentReference -> {
          Log.d("SampleDataHelper", "Sample violation added: " + location + " with ID: " + documentReference.getId());
        })
        .addOnFailureListener(e -> {
          Log.e("SampleDataHelper", "Failed to add sample violation: " + e.getMessage());
        });
  }

  public static void clearSampleData() {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    String userId = FirebaseAuth.getInstance().getCurrentUser() != null
        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
        : "sample_user";

    // Clear license_plates collection
    db.collection("license_plates")
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener(queryDocumentSnapshots -> {
          for (var doc : queryDocumentSnapshots.getDocuments()) {
            doc.getReference().delete();
          }
        });

    // Clear violations collection
    db.collection("violations")
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener(queryDocumentSnapshots -> {
          for (var doc : queryDocumentSnapshots.getDocuments()) {
            doc.getReference().delete();
          }
        });
  }
}