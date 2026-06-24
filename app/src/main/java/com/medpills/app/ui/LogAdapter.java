package com.medpills.app.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medpills.app.R;
import com.medpills.app.databinding.ItemLogBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private final List<LogDisplayItem> items = new ArrayList<>();
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
    private OnLogItemLongClickListener longClickListener;

    public interface OnLogItemLongClickListener {
        void onLongClick(LogDisplayItem item);
    }

    public void setOnLogItemLongClickListener(OnLogItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setItems(List<LogDisplayItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLogBinding binding = ItemLogBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new LogViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        LogDisplayItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class LogViewHolder extends RecyclerView.ViewHolder {
        private final ItemLogBinding binding;

        public LogViewHolder(ItemLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(LogDisplayItem item) {
            binding.tvLogMedName.setText(item.medicationName);
            binding.tvLogProfile.setText("Perfil: " + item.profileName);

            String timeStr = dateTimeFormat.format(new Date(item.log.getTimestampMillis()));
            binding.tvLogTimestamp.setText(timeStr);

            String status = item.log.getStatus();
            switch (status) {
                case "TAKEN":
                    binding.cardLogStatus.setCardBackgroundColor(binding.getRoot().getContext().getColor(R.color.success));
                    binding.tvLogStatus.setText("TOMADO");
                    break;
                case "SKIPPED":
                    binding.cardLogStatus.setCardBackgroundColor(binding.getRoot().getContext().getColor(R.color.error));
                    binding.tvLogStatus.setText("OMITIDO");
                    break;
                case "POSTPONED":
                    binding.cardLogStatus.setCardBackgroundColor(binding.getRoot().getContext().getColor(R.color.warning));
                    binding.tvLogStatus.setText("POSPUESTO");
                    break;
                default:
                    binding.cardLogStatus.setCardBackgroundColor(binding.getRoot().getContext().getColor(R.color.surface_light));
                    binding.tvLogStatus.setText("DESCONOCIDO");
                    break;
            }

            binding.getRoot().setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onLongClick(item);
                    return true;
                }
                return false;
            });
        }
    }
}
