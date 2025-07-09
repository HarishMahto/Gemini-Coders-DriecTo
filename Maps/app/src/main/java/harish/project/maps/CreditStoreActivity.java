package harish.project.maps;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.view.View;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

public class CreditStoreActivity extends AppCompatActivity {

    private TextView creditPointsText;
    private RecyclerView voucherRecyclerView;
    private VoucherAdapter voucherAdapter;
    private LicensePlateViewModel viewModel;
    private List<Voucher> voucherList = new ArrayList<>();
    private ProgressBar loadingProgress;
    private FloatingActionButton fabRefresh;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit_store);

        creditPointsText = findViewById(R.id.creditPoints);
        voucherRecyclerView = findViewById(R.id.voucherRecyclerView);
        loadingProgress = findViewById(R.id.loadingProgress);
        fabRefresh = findViewById(R.id.fabRefresh);

        viewModel = new ViewModelProvider(this).get(LicensePlateViewModel.class);
        viewModel.getCreditsAvailable().observe(this, credits -> {
            creditPointsText.setText(String.valueOf(credits));
            voucherAdapter.setUserCreditPoints(credits);
            voucherAdapter.notifyDataSetChanged();
        });

        voucherAdapter = new VoucherAdapter(voucherList, 0, voucher -> redeemVoucher(voucher));
        voucherRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        voucherRecyclerView.setAdapter(voucherAdapter);

        // Set up refresh button
        fabRefresh.setOnClickListener(v -> {
            loadingProgress.setVisibility(View.VISIBLE);
            voucherRecyclerView.setVisibility(View.GONE);
            fetchVouchers();
            viewModel.fetchViolationHistory(); // To update credits and fetch user credits
        });

        fetchVouchers();
        viewModel.fetchViolationHistory(); // To update credits and fetch user credits
    }

    private void fetchVouchers() {
        FirebaseFirestore.getInstance().collection("vouchers").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    voucherList.clear();
                    for (var doc : queryDocumentSnapshots.getDocuments()) {
                        Voucher v = doc.toObject(Voucher.class);
                        voucherList.add(v);
                    }

                    // If no vouchers found in Firestore, add sample FASTag vouchers
                    if (voucherList.isEmpty()) {
                        addSampleFastagVouchers();
                    }

                    voucherAdapter.notifyDataSetChanged();
                    loadingProgress.setVisibility(View.GONE);
                    voucherRecyclerView.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    // If Firestore fails, add sample FASTag vouchers
                    voucherList.clear();
                    addSampleFastagVouchers();
                    voucherAdapter.notifyDataSetChanged();
                    loadingProgress.setVisibility(View.GONE);
                    voucherRecyclerView.setVisibility(View.VISIBLE);
                });
    }

    private void addSampleFastagVouchers() {
        voucherList.add(new Voucher(
                "FASTag Recharge ₹100",
                "Recharge your FASTag account with ₹100. Valid for all toll plazas across India.",
                50,
                "Available"));

        voucherList.add(new Voucher(
                "FASTag Recharge ₹200",
                "Recharge your FASTag account with ₹200. Get 5% cashback on toll payments.",
                90,
                "Available"));

        voucherList.add(new Voucher(
                "FASTag Recharge ₹500",
                "Recharge your FASTag account with ₹500. Get 10% cashback and priority lane access.",
                200,
                "Available"));

        voucherList.add(new Voucher(
                "FASTag Recharge ₹1000",
                "Recharge your FASTag account with ₹1000. Get 15% cashback and premium support.",
                350,
                "Available"));

        voucherList.add(new Voucher(
                "FASTag Annual Pass",
                "Get unlimited toll access for 1 year on all highways. Save up to ₹5000 annually.",
                500,
                "Available"));

        voucherList.add(new Voucher(
                "FASTag Express Lane",
                "Get priority access to FASTag lanes at all toll plazas for 30 days.",
                75,
                "Available"));

        voucherList.add(new Voucher(
                "FASTag Weekend Special",
                "Get 20% extra value on FASTag recharge. Valid only on weekends.",
                120,
                "Available"));

        voucherList.add(new Voucher(
                "FASTag Family Pack",
                "Recharge ₹300 for family vehicles. Valid for up to 3 vehicles.",
                150,
                "Available"));

        voucherList.add(new Voucher(
                "FASTag Business Pack",
                "Recharge ₹2000 for business vehicles. Get 25% cashback and dedicated support.",
                400,
                "Available"));
    }

    private void redeemVoucher(Voucher voucher) {
        int credits = viewModel.getCreditsAvailable().getValue() != null ? viewModel.getCreditsAvailable().getValue()
                : 0;
        if (credits >= voucher.getRequiredPoints()) {
            // Calculate new credits after deduction
            int newCredits = credits - voucher.getRequiredPoints();

            // Deduct credits and add voucher to user's collection
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Update user credits first
            db.collection("users").document(userId).update("credits", newCredits)
                    .addOnSuccessListener(aVoid -> {
                        // Then save the voucher with auto-generated ID
                        db.collection("user_vouchers")
                                .add(voucher)
                                .addOnSuccessListener(aVoid2 -> {
                                    Toast.makeText(this, "FASTag voucher redeemed successfully!", Toast.LENGTH_SHORT)
                                            .show();
                                    // Update the ViewModel with new credits
                                    viewModel.updateCreditsAfterVoucherRedemption(newCredits);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to save voucher. Please try again.",
                                            Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update credits. Please try again.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Insufficient credits. You need " + voucher.getRequiredPoints() + " credits.",
                    Toast.LENGTH_SHORT).show();
        }
    }

}