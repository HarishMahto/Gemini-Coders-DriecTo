package harish.project.maps;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MyVoucherActivity extends AppCompatActivity {
    private MyVoucherAdapter adapter;
    private List<Voucher> myVouchers = new ArrayList<>();
    private LinearLayout loadingLayout;
    private LinearLayout emptyStateLayout;
    private TextView voucherCount;
    private RecyclerView recyclerView;
    private MaterialButton btnGoToStore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_voucher);

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        fetchMyVouchers();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.myVoucherRecyclerView);
        loadingLayout = findViewById(R.id.loadingLayout);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        voucherCount = findViewById(R.id.voucherCount);
        btnGoToStore = findViewById(R.id.btnGoToStore);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyVoucherAdapter(myVouchers);
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnGoToStore.setOnClickListener(v -> {
            // Add animation to button
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));

            android.content.Intent intent = new android.content.Intent(this, CreditStoreActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh vouchers when returning to this activity
        fetchMyVouchers();
    }

    private void fetchMyVouchers() {
        showLoading(true);
        var user = FirebaseAuth.getInstance().getCurrentUser();
        Log.d("MyVoucherActivity", "Current user: " + user);

        if (user == null) {
            // User is not logged in, handle accordingly
            Toast.makeText(this, "No user session. Please log in to view your vouchers.",
                    Toast.LENGTH_LONG).show();

            android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        String userId = user.getUid();
        FirebaseFirestore.getInstance().collection("user_vouchers")
                .whereEqualTo("userId", userId) // Filter by current user
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myVouchers.clear();
                    for (var doc : queryDocumentSnapshots.getDocuments()) {
                        Voucher v = doc.toObject(Voucher.class);
                        if (v != null) {
                            // Set the document ID for reference
                            v.setId(doc.getId());
                            myVouchers.add(v);
                            Log.d("MyVoucherActivity",
                                    "Found voucher: " + v.getTitle() + " for user: " + v.getUserId());
                        }
                    }
                    Log.d("MyVoucherActivity", "Total vouchers found: " + myVouchers.size());
                    adapter.notifyDataSetChanged();
                    updateUIAfterLoad();
                })
                .addOnFailureListener(e -> {
                    Log.e("MyVoucherActivity", "Error fetching vouchers", e);
                    showLoading(false);
                    showEmptyState();
                    Toast.makeText(this, "Failed to load vouchers. Please try again.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        if (show) {
            loadingLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        } else {
            loadingLayout.setVisibility(View.GONE);
        }
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        loadingLayout.setVisibility(View.GONE);
        voucherCount.setText("0");

        // Add animation to empty state
        emptyStateLayout.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
    }

    private void updateUIAfterLoad() {
        showLoading(false);
        if (myVouchers.isEmpty()) {
            showEmptyState();
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            voucherCount.setText(String.valueOf(myVouchers.size()));

            // Add animation to voucher count
            voucherCount.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pop_in));
        }
    }
}