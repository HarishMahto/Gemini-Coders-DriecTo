package harish.project.maps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import android.widget.ImageView;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ViolationHistoryActivity extends AppCompatActivity {
  private RecyclerView recyclerView;
  private ViolationAdapter adapter;
  private List<ViolationEntry> violationList = new ArrayList<>();
  private ProgressBar loadingProgress;
  private LinearLayout emptyStateLayout;
  private TextView violationCount;
  private MaterialButton btnSubmitViolation;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_violation_history);
    recyclerView = findViewById(R.id.violationRecyclerView);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    adapter = new ViolationAdapter(violationList);
    recyclerView.setAdapter(adapter);

    // Initialize UI elements
    loadingProgress = findViewById(R.id.loadingProgress);
    emptyStateLayout = findViewById(R.id.emptyStateLayout);
    violationCount = findViewById(R.id.violationCount);
    btnSubmitViolation = findViewById(R.id.btnSubmitViolation);

    // Set up button click listeners
    btnSubmitViolation.setOnClickListener(v -> {
      // Navigate to license plate activity to submit a violation
      android.content.Intent intent = new android.content.Intent(this, LicensePlateActivity.class);
      startActivity(intent);
    });

    // Add a simple test to check if we can load sample data
    Log.d("ViolationHistory", "Activity created, checking for sample data...");

    // Add long press to load sample data
    recyclerView.setOnLongClickListener(v -> {
      Log.d("ViolationHistory", "Long press detected, loading sample data...");
      SampleDataHelper.addSampleViolations();
      SampleDataHelper.addSampleLicensePlates();
      Toast.makeText(this, "Sample data loaded! Refreshing...", Toast.LENGTH_SHORT).show();
      fetchViolations();
      return true;
    });

    fetchViolations();
  }

  private void fetchViolations() {
    String userId = FirebaseAuth.getInstance().getCurrentUser() != null
        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
        : null;

    Log.d("ViolationHistory", "Current user ID: " + userId);

    if (userId == null) {
      Toast.makeText(this, "Please log in to view violation history", Toast.LENGTH_SHORT).show();
      return;
    }

    Log.d("ViolationHistory", "Fetching violations for user: " + userId);

    // Show loading state
    showLoading(true);

    // Try the query with ordering first
    FirebaseFirestore.getInstance().collection("violations")
        .whereEqualTo("userId", userId)
        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
        .get()
        .addOnFailureListener(e -> {
          Log.e("ViolationHistory", "Ordered query failed, trying without order: " + e.getMessage());
          // Fallback: try without ordering if the index doesn't exist
          FirebaseFirestore.getInstance().collection("violations")
              .whereEqualTo("userId", userId)
              .get()
              .addOnSuccessListener(queryDocumentSnapshots -> {
                Log.d("ViolationHistory",
                    "Fallback query successful. Found " + queryDocumentSnapshots.size() + " documents");
                violationList.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                  String location = doc.getString("location");
                  long timestamp = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0;
                  String status = doc.getString("status");
                  Log.d("ViolationHistory", "Violation: " + location + " - " + status + " - " + timestamp);
                  violationList.add(new ViolationEntry(location, timestamp, status));
                }
                Log.d("ViolationHistory", "Total violations in list: " + violationList.size());
                adapter.notifyDataSetChanged();
              })
              .addOnFailureListener(e2 -> {
                Log.e("ViolationHistory", "Fallback query also failed: " + e2.getMessage());
                Toast.makeText(this, "Error loading violations: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
              });
        })
        .addOnSuccessListener(queryDocumentSnapshots -> {
          Log.d("ViolationHistory", "Query successful. Found " + queryDocumentSnapshots.size() + " documents");
          violationList.clear();
          for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
            String location = doc.getString("location");
            long timestamp = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0;
            String status = doc.getString("status");
            Log.d("ViolationHistory", "Violation: " + location + " - " + status + " - " + timestamp);
            violationList.add(new ViolationEntry(location, timestamp, status));
          }
          Log.d("ViolationHistory", "Total violations in list: " + violationList.size());
          adapter.notifyDataSetChanged();
          updateUIAfterLoad();
        })
        .addOnFailureListener(e -> {
          Log.e("ViolationHistory", "Error fetching violations: " + e.getMessage());
          Toast.makeText(this, "Error loading violations: " + e.getMessage(), Toast.LENGTH_SHORT).show();
          showEmptyState();
        });
  }

  static class ViolationEntry {
    String location;
    long timestamp;
    String status;

    ViolationEntry(String location, long timestamp, String status) {
      this.location = location;
      this.timestamp = timestamp;
      this.status = status;
    }
  }

  static class ViolationAdapter extends RecyclerView.Adapter<ViolationAdapter.ViolationViewHolder> {
    private final List<ViolationEntry> list;

    ViolationAdapter(List<ViolationEntry> list) {
      this.list = list;
    }

    @NonNull
    @Override
    public ViolationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_violation, parent, false);
      return new ViolationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViolationViewHolder holder, int position) {
      ViolationEntry entry = list.get(position);
      holder.location.setText(entry.location);
      holder.timestamp
          .setText(new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(entry.timestamp)));

      // Set status with proper styling
      if ("Verified".equalsIgnoreCase(entry.status)) {
        holder.status.setText("Verified");
        holder.status.setTextColor(Color.parseColor("#4CAF50"));
        holder.statusDot.setBackgroundColor(Color.parseColor("#4CAF50"));
        holder.statusIndicator.setBackgroundColor(Color.parseColor("#4CAF50"));
      } else if ("Rejected".equalsIgnoreCase(entry.status)) {
        holder.status.setText("Rejected");
        holder.status.setTextColor(Color.parseColor("#F44336"));
        holder.statusDot.setBackgroundColor(Color.parseColor("#F44336"));
        holder.statusIndicator.setBackgroundColor(Color.parseColor("#F44336"));
      } else {
        holder.status.setText("Pending");
        holder.status.setTextColor(Color.parseColor("#FF9800"));
        holder.statusDot.setBackgroundColor(Color.parseColor("#FF9800"));
        holder.statusIndicator.setBackgroundColor(Color.parseColor("#FF9800"));
      }
    }

    @Override
    public int getItemCount() {
      return list.size();
    }

    static class ViolationViewHolder extends RecyclerView.ViewHolder {
      TextView location, timestamp, status;
      View statusDot, statusIndicator;
      ImageView violationIcon;

      ViolationViewHolder(@NonNull View itemView) {
        super(itemView);
        location = itemView.findViewById(R.id.violationLocation);
        timestamp = itemView.findViewById(R.id.violationTimestamp);
        status = itemView.findViewById(R.id.violationStatus);
        statusDot = itemView.findViewById(R.id.statusDot);
        statusIndicator = itemView.findViewById(R.id.statusIndicator);
        violationIcon = itemView.findViewById(R.id.violationIcon);
      }
    }
  }

  private void showLoading(boolean show) {
    loadingProgress.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
    recyclerView.setVisibility(show ? android.view.View.GONE : android.view.View.VISIBLE);
    emptyStateLayout.setVisibility(android.view.View.GONE);
  }

  private void showEmptyState() {
    recyclerView.setVisibility(android.view.View.GONE);
    emptyStateLayout.setVisibility(android.view.View.VISIBLE);
    loadingProgress.setVisibility(android.view.View.GONE);
    violationCount.setText("0");
  }

  private void updateUIAfterLoad() {
    showLoading(false);
    if (violationList.isEmpty()) {
      showEmptyState();
    } else {
      recyclerView.setVisibility(android.view.View.VISIBLE);
      emptyStateLayout.setVisibility(android.view.View.GONE);
      violationCount.setText(String.valueOf(violationList.size()));
    }
  }
}