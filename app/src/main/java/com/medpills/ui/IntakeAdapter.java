package com.medpills.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medpills.R;
import com.medpills.databinding.ItemIntakeBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IntakeAdapter extends RecyclerView.Adapter<IntakeAdapter.IntakeViewHolder> {

    private final List<IntakeItem> items = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public interface OnIntakeInteractionListener {
        void onRecordIntake(IntakeItem item, String status, int position);
        void onUndoIntake(IntakeItem item, int position);
        void onEditMedication(IntakeItem item);
    }

    private OnIntakeInteractionListener interactionListener;

    public void setOnIntakeInteractionListener(OnIntakeInteractionListener listener) {
        this.interactionListener = listener;
    }

    public void setItems(List<IntakeItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public List<IntakeItem> getItems() {
        return items;
    }

    @NonNull
    @Override
    public IntakeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemIntakeBinding binding = ItemIntakeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new IntakeViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull IntakeViewHolder holder, int position) {
        IntakeItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class IntakeViewHolder extends RecyclerView.ViewHolder {
        private final ItemIntakeBinding binding;

        public IntakeViewHolder(ItemIntakeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(IntakeItem item) {
            binding.tvMedName.setText(item.getMedication().getName());
            String dosageStr = item.getMedication().getDosageQuantity() + " " + item.getMedication().getDosageUnit();
            binding.tvDosageDetails.setText(dosageStr);

            String timeStr = timeFormat.format(new Date(item.getScheduledTimeMillis()));
            binding.tvIntakeTime.setText(timeStr);

            String description = item.getMedication().getDescription();
            if (description != null && !description.isEmpty()) {
                binding.tvMedNotes.setVisibility(android.view.View.VISIBLE);
                binding.tvMedNotes.setText(description);
            } else {
                binding.tvMedNotes.setVisibility(android.view.View.GONE);
            }

            String status = item.getStatus();
            switch (status) {
                case "TAKEN":
                    binding.cardStatusBadge.setCardBackgroundColor(binding.getRoot().getContext().getColor(R.color.success));
                    binding.tvStatusBadge.setText("TOMADO");
                    binding.tvStatusBadge.setTextColor(Color_WHITE());
                    break;
                case "SKIPPED":
                    binding.cardStatusBadge.setCardBackgroundColor(binding.getRoot().getContext().getColor(R.color.error));
                    binding.tvStatusBadge.setText("OMITIDO");
                    binding.tvStatusBadge.setTextColor(Color_WHITE());
                    break;
                case "POSTPONED":
                    binding.cardStatusBadge.setCardBackgroundColor(binding.getRoot().getContext().getColor(R.color.warning));
                    binding.tvStatusBadge.setText("POSPUESTO 15M");
                    binding.tvStatusBadge.setTextColor(Color_WHITE());
                    break;
                default:
                    binding.cardStatusBadge.setCardBackgroundColor(binding.getRoot().getContext().getColor(R.color.surface_light));
                    binding.tvStatusBadge.setText("PENDIENTE");
                    binding.tvStatusBadge.setTextColor(binding.getRoot().getContext().getColor(R.color.text_secondary_light));
                    break;
            }
            binding.getRoot().setOnClickListener(v -> {
                if (interactionListener != null) {
                    if (!"PENDING".equals(item.getStatus())) {
                        interactionListener.onUndoIntake(item, getAdapterPosition());
                    }
                }
            });

            binding.getRoot().setOnLongClickListener(v -> {
                if (interactionListener != null) {
                    interactionListener.onEditMedication(item);
                }
                return true;
            });
        }

        private int Color_WHITE() {
            return android.graphics.Color.WHITE;
        }
    }
}
