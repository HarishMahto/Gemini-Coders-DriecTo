package harish.project.maps;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FirestoreSetupHelper {

  public static void setupInitialCollections() {
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Create sample vouchers collection
    createSampleVouchers(db);

    // Create users collection structure
    createUsersCollectionStructure(db);
  }

  private static void createSampleVouchers(FirebaseFirestore db) {
    // Sample FASTag vouchers
    Map<String, Object> voucher1 = new HashMap<>();
    voucher1.put("title", "FASTag Recharge ₹100");
    voucher1.put("description", "Recharge your FASTag account with ₹100. Valid for all toll plazas across India.");
    voucher1.put("requiredPoints", 50);
    voucher1.put("status", "Available");
    voucher1.put("type", "fastag");

    Map<String, Object> voucher2 = new HashMap<>();
    voucher2.put("title", "FASTag Recharge ₹200");
    voucher2.put("description", "Recharge your FASTag account with ₹200. Get 5% cashback on toll payments.");
    voucher2.put("requiredPoints", 90);
    voucher2.put("status", "Available");
    voucher2.put("type", "fastag");

    Map<String, Object> voucher3 = new HashMap<>();
    voucher3.put("title", "FASTag Recharge ₹500");
    voucher3.put("description", "Recharge your FASTag account with ₹500. Get 10% cashback and priority lane access.");
    voucher3.put("requiredPoints", 200);
    voucher3.put("status", "Available");
    voucher3.put("type", "fastag");

    // Add vouchers to Firestore
    db.collection("vouchers").document("fastag_100").set(voucher1);
    db.collection("vouchers").document("fastag_200").set(voucher2);
    db.collection("vouchers").document("fastag_500").set(voucher3);
  }

  private static void createUsersCollectionStructure(FirebaseFirestore db) {
    // This creates the structure for users collection
    // Individual user documents will be created when users sign up
    Map<String, Object> userStructure = new HashMap<>();
    userStructure.put("credits", 300);
    userStructure.put("totalCreditsEarned", 300);
    userStructure.put("creditsUsed", 0);
    userStructure.put("registrationDate", new java.util.Date());
    userStructure.put("lastLogin", new java.util.Date());

    // This is just for reference - actual user documents will be created per user
    db.collection("users").document("structure_reference").set(userStructure);
  }

  public static void cleanupTestData() {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    // Remove test connection document
    db.collection("test").document("connection").delete();
    // Remove structure reference
    db.collection("users").document("structure_reference").delete();
  }
}