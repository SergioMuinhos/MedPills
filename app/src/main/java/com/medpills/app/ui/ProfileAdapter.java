package com.medpills.app.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medpills.app.R;
import com.medpills.app.database.Profile;
import com.medpills.app.databinding.ItemProfileBinding;
import java.util.ArrayList;
import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder> {

    private final List<Profile> profiles = new ArrayList<>();
    private long selectedProfileId = 1; // Default to 1 (bootstrap profile)
    private OnProfileSelectedListener listener;

    public interface OnProfileSelectedListener {
        void onProfileSelected(Profile profile);
    }

    public void setOnProfileSelectedListener(OnProfileSelectedListener listener) {
        this.listener = listener;
    }

    public void setProfiles(List<Profile> newProfiles) {
        profiles.clear();
        if (newProfiles != null) {
            profiles.addAll(newProfiles);
        }
        notifyDataSetChanged();
    }

    public void setSelectedProfileId(long id) {
        this.selectedProfileId = id;
        notifyDataSetChanged();
    }

    public long getSelectedProfileId() {
        return selectedProfileId;
    }

    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProfileBinding binding = ItemProfileBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ProfileViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        Profile profile = profiles.get(position);
        holder.bind(profile);
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    class ProfileViewHolder extends RecyclerView.ViewHolder {
        private final ItemProfileBinding binding;

        public ProfileViewHolder(ItemProfileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Profile profile) {
            binding.tvProfileName.setText(profile.getName());
            
            // Highlight selected profile
            float density = binding.getRoot().getContext().getResources().getDisplayMetrics().density;
            if (profile.getId() == selectedProfileId) {
                binding.cardAvatar.setStrokeColor(binding.getRoot().getContext().getColor(R.color.primary));
                binding.cardAvatar.setCardElevation(6 * density);
                binding.tvProfileName.setTextColor(binding.getRoot().getContext().getColor(R.color.primary));
            } else {
                binding.cardAvatar.setStrokeColor(binding.getRoot().getContext().getColor(R.color.surface_light));
                binding.cardAvatar.setCardElevation(2 * density);
                binding.tvProfileName.setTextColor(binding.getRoot().getContext().getColor(R.color.text_primary_light));
            }

            // Set Avatar image resource or custom URI
            if (profile.getImageUri() != null) {
                try {
                    binding.ivAvatar.setImageURI(android.net.Uri.parse(profile.getImageUri()));
                } catch (Exception e) {
                    android.util.Log.e("ProfileAdapter", "Error setting custom profile image URI", e);
                    binding.ivAvatar.setImageResource(R.drawable.avatar_default);
                }
            } else {
                String resName = profile.getAvatarResourceName();
                int resId = binding.getRoot().getContext().getResources().getIdentifier(
                        resName, "drawable", binding.getRoot().getContext().getPackageName());
                if (resId != 0) {
                    binding.ivAvatar.setImageResource(resId);
                } else {
                    binding.ivAvatar.setImageResource(R.drawable.avatar_default);
                }
            }

            binding.getRoot().setOnClickListener(v -> {
                long prevSelected = selectedProfileId;
                selectedProfileId = profile.getId();
                if (listener != null) {
                    listener.onProfileSelected(profile);
                }
                notifyDataSetChanged();
            });
        }
    }
}
