package harish.project.maps;

public class Voucher {
    private String id; // Document ID
    private String title;
    private String description;
    private int requiredPoints;
    private String status; // "Available", "Active", "Used", "Expired"
    private String icon; // Icon resource name or type
    private String userId; // User who owns this voucher
    private String purchaseDate; // When the voucher was purchased

    // Empty constructor needed for Firestore deserialization
    public Voucher() {
        // Required empty constructor for Firestore
    }

    public Voucher(String title, String description, int requiredPoints, String status) {
        this.title = title;
        this.description = description;
        this.requiredPoints = requiredPoints;
        this.status = status;
        this.icon = "fastag"; // Default icon
        this.userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        this.purchaseDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
    }

    public Voucher(String title, String description, int requiredPoints, String status, String icon) {
        this.title = title;
        this.description = description;
        this.requiredPoints = requiredPoints;
        this.status = status;
        this.icon = icon;
        this.userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        this.purchaseDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getRequiredPoints() {
        return requiredPoints;
    }

    public String getStatus() {
        return status;
    }

    public String getIcon() {
        return icon;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}