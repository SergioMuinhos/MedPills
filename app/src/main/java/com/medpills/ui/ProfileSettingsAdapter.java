package com.medpills.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medpills.R;
import com.medpills.database.Profile;
import com.medpills.databinding.ItemProfileSettingBinding;
import java.util.ArrayList;
import java.util.List;

public class ProfileSettingsAdapter extends RecyclerView.Adapter<ProfileSettingsAdapter.ProfileSettingsViewHolder> {

    private final List<Profile> profiles = new ArrayList<>();
    private OnProfileDeleteClickListener deleteClickListener;
    private OnProfileEditClickListener editClickListener;

    public interface OnProfileDeleteClickListener {
        void onProfileDeleteClick(Profile profile);
    }

    public interface OnProfileEditClickListener {
        void onProfileEditClick(Profile profile);
    }

    public void setOnProfileDeleteClickListener(OnProfileDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    public void setOnProfileEditClickListener(OnProfileEditClickListener listener) {
        this.editClickListener = listener;
    }

    public void setProfiles(List<Profile> newProfiles) {
        profiles.clear();
        if (newProfiles != null) {
            profiles.addAll(newProfiles);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProfileSettingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProfileSettingBinding binding = ItemProfileSettingBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ProfileSettingsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileSettingsViewHolder holder, int position) {
        Profile profile = profiles.get(position);
        holder.bind(profile);
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    class ProfileSettingsViewHolder extends RecyclerView.ViewHolder {
        private final ItemProfileSettingBinding binding;

        public ProfileSettingsViewHolder(ItemProfileSettingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Profile profile) {
            binding.tvProfileSettingName.setText(profile.getName());

            // Set Avatar image resource or custom URI
            if (profile.getImageUri() != null) {
                binding.ivProfileSettingAvatar.setImageURI(android.net.Uri.parse(profile.getImageUri()));
            } else {
                String resName = profile.getAvatarResourceName();
                int resId = binding.getRoot().getContext().getResources().getIdentifier(
                        resName, "drawable", binding.getRoot().getContext().getPackageName());
                if (resId != 0) {
                    binding.ivProfileSettingAvatar.setImageResource(resId);
                } else {
                    binding.ivProfileSettingAvatar.setImageResource(R.drawable.avatar_default);
                }
            }

            // Lock default bootstrap profile (ID 1) from deletion to prevent constraint breaks
            if (profile.getId() == 1) {
                binding.btnDeleteProfile.setVisibility(View.GONE);
            } else {
                binding.btnDeleteProfile.setVisibility(View.VISIBLE);
                binding.btnDeleteProfile.setOnClickListener(v -> {
                    if (deleteClickListener != null) {
                        deleteClickListener.onProfileDeleteClick(profile);
                    }
                });
            }

            binding.getRoot().setOnClickListener(v -> {
                if (editClickListener != null) {
                    editClickListener.onProfileEditClick(profile);
                }
            });
        }
    }
}
