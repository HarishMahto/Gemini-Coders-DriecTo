package harish.project.maps;

import java.util.Date;

public class LicensePlate {
    private String id;
    private String plateNumber;
    private Date timestamp;
    private String imageUrl;
    private String violationType;
    private String location;
    private String status;
    private String userId;

    // Empty constructor needed for Firestore
    public LicensePlate() {
    }

    public LicensePlate(String plateNumber, Date timestamp, String imageUrl, String violationType, String location) {
        this.plateNumber = plateNumber;
        this.timestamp = timestamp;
        this.imageUrl = imageUrl;
        this.violationType = violationType;
        this.location = location;
        this.userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getViolationType() {
        return violationType;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}