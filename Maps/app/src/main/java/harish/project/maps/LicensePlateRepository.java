package harish.project.maps;

import android.net.Uri;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Date;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class LicensePlateRepository {
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private MutableLiveData<String> uploadStatus = new MutableLiveData<>();

    public LicensePlateRepository() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public MutableLiveData<String> getUploadStatus() {
        return uploadStatus;
    }

    public void saveLicensePlate(Uri imageUri, String plateNumber, String violationType, String location) {
        uploadStatus.setValue("Uploading image...");

        // Generate a unique filename
        String filename = "license_plates/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference storageRef = storage.getReference().child(filename);

        // Upload image to Firebase Storage
        UploadTask uploadTask = storageRef.putFile(imageUri);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Get the download URL
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();

                // Create license plate object
                LicensePlate licensePlate = new LicensePlate(plateNumber, new Date(), imageUrl, violationType,
                        location);

                // Save to Firestore
                db.collection("license_plates")
                        .add(licensePlate)
                        .addOnSuccessListener(documentReference -> {
                            uploadStatus.setValue("Success");
                        })
                        .addOnFailureListener(e -> {
                            uploadStatus.setValue("Error saving data: " + e.getMessage());
                        });
            });
        }).addOnFailureListener(e -> {
            uploadStatus.setValue("Error uploading image: " + e.getMessage());
        }).addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            uploadStatus.setValue("Uploading: " + (int) progress + "%");
        });
    }

    public void fetchViolationHistory(MutableLiveData<List<LicensePlate>> historyLiveData) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null) {
            historyLiveData.setValue(new ArrayList<>());
            return;
        }

        db.collection("license_plates")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<LicensePlate> history = new ArrayList<>();
                    for (var doc : queryDocumentSnapshots.getDocuments()) {
                        LicensePlate lp = doc.toObject(LicensePlate.class);
                        if (lp != null) {
                            lp.setId(doc.getId()); // Set the document ID
                            history.add(lp);
                        }
                    }
                    historyLiveData.setValue(history);
                })
                .addOnFailureListener(e -> {
                    historyLiveData.setValue(new ArrayList<>());
                });
    }

    public void getUserCredits(MutableLiveData<Integer> creditsLiveData) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null) {
            creditsLiveData.setValue(0);
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("credits")) {
                        creditsLiveData.setValue(documentSnapshot.getLong("credits").intValue());
                    } else {
                        // If user doesn't have credits initialized, set to 0
                        creditsLiveData.setValue(0);
                    }
                })
                .addOnFailureListener(e -> creditsLiveData.setValue(0));
    }

    public void updateUserCredits(int newCredits) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null) {
            return;
        }

        db.collection("users").document(userId).update("credits", newCredits)
                .addOnSuccessListener(aVoid -> {
                    // Successfully updated credits
                })
                .addOnFailureListener(e -> {
                    // Handle failure if needed
                });
    }

    public void addCreditsForViolation(int creditsToAdd) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null) {
            return;
        }

        // Get current credits and add new ones
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    int currentCredits = 0;
                    if (documentSnapshot.exists() && documentSnapshot.contains("credits")) {
                        currentCredits = documentSnapshot.getLong("credits").intValue();
                    }
                    int newCredits = currentCredits + creditsToAdd;
                    updateUserCredits(newCredits);
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                });
    }
}