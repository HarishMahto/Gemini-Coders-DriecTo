package harish.project.maps;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyVoucherAdapter extends RecyclerView.Adapter<MyVoucherAdapter.MyVoucherViewHolder> {
    private List<Voucher> vouchers;

    public MyVoucherAdapter(List<Voucher> vouchers) {
        this.vouchers = vouchers;
    }

    @NonNull
    @Override
    public MyVoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_voucher, parent, false);
        return new MyVoucherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyVoucherViewHolder holder, int position) {
        Voucher voucher = vouchers.get(position);
        holder.title.setText(voucher.getTitle());
        holder.description.setText(voucher.getDescription());
        holder.status.setText(voucher.getStatus());

        // Set purchase date with better formatting
        if (voucher.getPurchaseDate() != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                Date date = inputFormat.parse(voucher.getPurchaseDate());
                if (date != null) {
                    holder.purchaseDate.setText(" • Claimed on " + outputFormat.format(date));
                } else {
                    holder.purchaseDate.setText(" • Recently claimed");
                }
            } catch (Exception e) {
                holder.purchaseDate.setText(" • Recently claimed");
            }
        } else {
            holder.purchaseDate.setText(" • Recently claimed");
        }

        // Set icon based on voucher type
        if (voucher.getTitle().toLowerCase().contains("fastag")) {
            holder.voucherIcon.setImageResource(R.drawable.gift); // Using gift icon for now
        } else if (voucher.getTitle().toLowerCase().contains("fuel")) {
            holder.voucherIcon.setImageResource(R.drawable.gift);
        } else if (voucher.getTitle().toLowerCase().contains("parking")) {
            holder.voucherIcon.setImageResource(R.drawable.gift);
        } else {
            holder.voucherIcon.setImageResource(R.drawable.gift);
        }

        // Set status colors and button state
        if ("Active".equals(voucher.getStatus())) {
            holder.status.setTextColor(Color.parseColor("#4CAF50"));
            holder.statusIndicator.setBackgroundColor(Color.parseColor("#4CAF50"));
            holder.actionButton.setText("Use Now");
            holder.actionButton.setEnabled(true);
            holder.actionButton.setBackgroundColor(Color.parseColor("#F38120"));
        } else if ("Used".equals(voucher.getStatus())) {
            holder.status.setTextColor(Color.parseColor("#888"));
            holder.statusIndicator.setBackgroundColor(Color.parseColor("#888"));
            holder.actionButton.setText("Used");
            holder.actionButton.setEnabled(false);
            holder.actionButton.setBackgroundColor(Color.parseColor("#CCCCCC"));
        } else if ("Expired".equals(voucher.getStatus())) {
            holder.status.setTextColor(Color.parseColor("#F44336"));
            holder.statusIndicator.setBackgroundColor(Color.parseColor("#F44336"));
            holder.actionButton.setText("Expired");
            holder.actionButton.setEnabled(false);
            holder.actionButton.setBackgroundColor(Color.parseColor("#F44336"));
        }

        // Set credit cost if available
        if (voucher.getRequiredPoints() > 0) {
            holder.creditCost.setText("Cost: " + voucher.getRequiredPoints() + " credits");
        } else {
            holder.creditCost.setText("Free voucher");
        }

        // Set up action button click listener
        holder.actionButton.setOnClickListener(v -> {
            if ("Active".equals(voucher.getStatus())) {
                // Add animation to button
                v.startAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.pulse));

                // Handle voucher usage
                useVoucher(voucher, holder);
            } else {
                Toast.makeText(v.getContext(), "This voucher cannot be used", Toast.LENGTH_SHORT).show();
            }
        });

        // Add item click animation
        holder.itemView.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(v.getContext(), R.anim.pulse));
        });
    }

    private void useVoucher(Voucher voucher, MyVoucherViewHolder holder) {
        // Here you would implement the actual voucher usage logic
        // For now, we'll just show a toast and update the UI
        Toast.makeText(holder.itemView.getContext(),
                "Using voucher: " + voucher.getTitle(), Toast.LENGTH_SHORT).show();

        // Update voucher status to "Used"
        voucher.setStatus("Used");

        // Update UI
        holder.status.setText("Used");
        holder.status.setTextColor(Color.parseColor("#888"));
        holder.statusIndicator.setBackgroundColor(Color.parseColor("#888"));
        holder.actionButton.setText("Used");
        holder.actionButton.setEnabled(false);
        holder.actionButton.setBackgroundColor(Color.parseColor("#CCCCCC"));

        // Notify adapter of the change
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return vouchers.size();
    }

    static class MyVoucherViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, status, purchaseDate, creditCost;
        View statusIndicator;
        ImageView voucherIcon;
        android.widget.Button actionButton;

        public MyVoucherViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.voucherTitle);
            description = itemView.findViewById(R.id.voucherDescription);
            status = itemView.findViewById(R.id.voucherStatus);
            purchaseDate = itemView.findViewById(R.id.voucherDate);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            voucherIcon = itemView.findViewById(R.id.voucherIcon);
            actionButton = itemView.findViewById(R.id.actionButton);
            creditCost = itemView.findViewById(R.id.creditCost);
        }
    }
}