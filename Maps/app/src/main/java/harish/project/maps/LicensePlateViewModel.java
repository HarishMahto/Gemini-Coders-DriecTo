package harish.project.maps;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.List;

import okhttp3.*;
import org.json.JSONObject;
import harish.project.maps.services.VertexAIService;

public class LicensePlateViewModel extends AndroidViewModel {
    private LicensePlateRepository repository;
    private MutableLiveData<String> recognizedText = new MutableLiveData<>();
    private MutableLiveData<Uri> capturedImageUri = new MutableLiveData<>();
    private TextRecognizer textRecognizer;
    private MutableLiveData<String> violationType = new MutableLiveData<>();
    private MutableLiveData<String> location = new MutableLiveData<>();
    private MutableLiveData<List<LicensePlate>> violationHistory = new MutableLiveData<>();
    private MutableLiveData<Integer> creditsAvailable = new MutableLiveData<>(0);
    private MutableLiveData<Integer> creditsUsed = new MutableLiveData<>(0);
    private MutableLiveData<Integer> creditsTotal = new MutableLiveData<>(0);

    private VertexAIService vertexAIService;

    public LicensePlateViewModel(@NonNull Application application) {
        super(application);
        repository = new LicensePlateRepository();
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        vertexAIService = new VertexAIService();
    }

    public LiveData<String> getRecognizedText() {
        return recognizedText;
    }

    public LiveData<Uri> getCapturedImageUri() {
        return capturedImageUri;
    }

    public LiveData<String> getUploadStatus() {
        return repository.getUploadStatus();
    }

    public LiveData<String> getViolationType() {
        return violationType;
    }

    public void setViolationType(String type) {
        violationType.setValue(type);
    }

    public LiveData<String> getLocation() {
        return location;
    }

    public void setLocation(String loc) {
        location.setValue(loc);
    }

    public void setCapturedImageUri(Uri uri) {
        capturedImageUri.setValue(uri);
    }

    public LiveData<List<LicensePlate>> getViolationHistory() {
        return violationHistory;
    }

    public LiveData<Integer> getCreditsAvailable() {
        return creditsAvailable;
    }

    public LiveData<Integer> getCreditsUsed() {
        return creditsUsed;
    }

    public LiveData<Integer> getCreditsTotal() {
        return creditsTotal;
    }

    public void fetchViolationHistory() {
        repository.fetchViolationHistory(violationHistory);
        // Fetch credits from Firestore instead of calculating from violations
        repository.getUserCredits(creditsAvailable);
    }

    public void processImage(Uri imageUri) {
        // Try to get access token first (backend method)
        String accessToken = TokenFetcher.fetchAccessToken();

        if (accessToken != null) {
            // Use backend token method
            vertexAIService.recognizeLicensePlate(
                    getApplication(),
                    imageUri,
                    accessToken,
                    new VertexAIService.LicensePlateCallback() {
                        @Override
                        public void onSuccess(String licensePlate) {
                            recognizedText.postValue(licensePlate);
                        }

                        @Override
                        public void onError(String error) {
                            recognizedText.postValue("Error: " + error);
                        }
                    });
        } else {
            // Fallback to direct API key method
            vertexAIService.recognizeLicensePlateWithApiKey(
                    getApplication(),
                    imageUri,
                    new VertexAIService.LicensePlateCallback() {
                        @Override
                        public void onSuccess(String licensePlate) {
                            recognizedText.postValue(licensePlate);
                        }

                        @Override
                        public void onError(String error) {
                            recognizedText.postValue("Error: " + error);
                        }
                    });
        }
    }

    public void saveLicensePlate() {
        Uri imageUri = capturedImageUri.getValue();
        String plateText = recognizedText.getValue();
        String selectedViolation = violationType.getValue();
        String loc = location.getValue();
        if (imageUri != null && plateText != null && selectedViolation != null && !plateText.startsWith("Error")
                && !plateText.equals("No license plate detected")) {
            repository.saveLicensePlate(imageUri, plateText, selectedViolation, loc);
        } else {
            repository.getUploadStatus().setValue("Error: Invalid image, text, or violation type");
        }
    }

    public void fetchUserCredits() {
        repository.getUserCredits(creditsAvailable);
    }

    public void redeemVoucher(Voucher voucher) {
        int credits = creditsAvailable.getValue() != null ? creditsAvailable.getValue() : 0;
        if (credits >= voucher.getRequiredPoints()) {
            int newCredits = credits - voucher.getRequiredPoints();
            repository.updateUserCredits(newCredits);
            creditsAvailable.setValue(newCredits);
            // Optionally, add voucher to user_vouchers collection here
        }
    }

    public void addCreditsForViolation(int creditsToAdd) {
        repository.addCreditsForViolation(creditsToAdd);
        // Update the LiveData after adding credits
        int currentCredits = creditsAvailable.getValue() != null ? creditsAvailable.getValue() : 0;
        creditsAvailable.setValue(currentCredits + creditsToAdd);
    }

    public void updateCreditsAfterVoucherRedemption(int newCredits) {
        creditsAvailable.setValue(newCredits);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        textRecognizer.close();
        if (vertexAIService != null) {
            vertexAIService.shutdown();
        }
    }
}