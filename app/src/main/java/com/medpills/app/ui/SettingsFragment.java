package com.medpills.app.ui;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.medpills.app.R;
import com.medpills.app.backup.BackupManager;
import com.medpills.app.database.MedicationRepository;
import com.medpills.app.database.Profile;
import com.medpills.app.databinding.FragmentSettingsBinding;
import com.medpills.app.utils.ThemeHelper;
import com.medpills.app.utils.UnitManager;
import java.util.List;

public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";
    private FragmentSettingsBinding binding;
    private MedicationRepository repository;
    private UnitManager unitManager;
    private ProfileSettingsAdapter profileSettingsAdapter;
    private Profile editingProfile;
    private String selectedCustomImageUri;
    private View currentDialogView;

    // Storage Access Framework / Photo Picker Launcher (API 33-37 style)
    private final ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null && currentDialogView != null) {
                    // Persist access to the URI
                    try {
                        requireContext().getContentResolver().takePersistableUriPermission(uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException e) {
                        Log.e(TAG, "Could not take persistable permission", e);
                    }
                    
                    selectedCustomImageUri = uri.toString();
                    updateDialogPhotoPreview(currentDialogView, selectedCustomImageUri);
                }
            });

    private final ActivityResultLauncher<String> exportBackupLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null) {
                    exportData(uri);
                }
            });

    private final ActivityResultLauncher<String[]> importBackupLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    importData(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new MedicationRepository(requireContext());
        unitManager = new UnitManager(requireContext());

        setupAppearanceCard();
        setupLanguageCard();
        setupBatteryCard();
        setupBackupCard();
        setupUnitsCard();
        setupProfilesCard();
    }

    private void setupUnitsCard() {
        refreshUnitsList();
        binding.btnAddUnit.setOnClickListener(v -> showAddUnitDialog());
    }

    private void refreshUnitsList() {
        binding.cgUnits.removeAllViews();
        List<String> units = unitManager.getUnits();
        for (String unit : units) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext());
            chip.setText(unit);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                unitManager.removeUnit(unit);
                refreshUnitsList();
            });
            binding.cgUnits.addView(chip);
        }
    }

    private void showAddUnitDialog() {
        android.widget.EditText et = new android.widget.EditText(requireContext());
        et.setHint("Ej. gotas, g, puff");
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Añadir nueva unidad")
                .setView(et)
                .setPositiveButton("Añadir", (dialog, which) -> {
                    String val = et.getText().toString().trim();
                    if (!val.isEmpty()) {
                        unitManager.addUnit(val);
                        refreshUnitsList();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update battery optimization status on return
        updateBatteryStatus();
        loadProfilesList();
    }

    private void setupAppearanceCard() {
        int currentTheme = ThemeHelper.getSelectedTheme(requireContext());
        if (currentTheme == ThemeHelper.THEME_LIGHT) {
            binding.toggleTheme.check(R.id.btn_theme_light);
        } else if (currentTheme == ThemeHelper.THEME_DARK) {
            binding.toggleTheme.check(R.id.btn_theme_dark);
        } else {
            binding.toggleTheme.check(R.id.btn_theme_system);
        }

        binding.toggleTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                int theme;
                if (checkedId == R.id.btn_theme_light) theme = ThemeHelper.THEME_LIGHT;
                else if (checkedId == R.id.btn_theme_dark) theme = ThemeHelper.THEME_DARK;
                else theme = ThemeHelper.THEME_SYSTEM;

                ThemeHelper.setSelectedTheme(requireContext(), theme);
                ThemeHelper.applyTheme(theme);
            }
        });
    }

    private void setupLanguageCard() {
        // Read current language selection
        LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        if (currentLocales.isEmpty()) {
            binding.toggleLanguage.check(R.id.btn_lang_system);
        } else {
            String language = currentLocales.get(0).getLanguage();
            if ("es".equals(language)) {
                binding.toggleLanguage.check(R.id.btn_lang_es);
            } else if ("en".equals(language)) {
                binding.toggleLanguage.check(R.id.btn_lang_en);
            } else {
                binding.toggleLanguage.check(R.id.btn_lang_system);
            }
        }

        binding.toggleLanguage.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                LocaleListCompat locales;
                if (checkedId == R.id.btn_lang_es) {
                    locales = LocaleListCompat.forLanguageTags("es");
                } else if (checkedId == R.id.btn_lang_en) {
                    locales = LocaleListCompat.forLanguageTags("en");
                } else {
                    locales = LocaleListCompat.getEmptyLocaleList();
                }
                
                AppCompatDelegate.setApplicationLocales(locales);
            }
        });
    }

    private void setupBatteryCard() {
        updateBatteryStatus();
        View.OnClickListener batteryListener = v -> {
            try {
                // Open the general ignore battery optimization settings list (compliant with Play Store)
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening battery optimization settings", e);
            }
        };
        
        binding.btnConfigureBattery.setOnClickListener(batteryListener);
        binding.btnReconfigureBattery.setOnClickListener(batteryListener);
    }

    private void updateBatteryStatus() {
        Context context = getContext();
        if (context == null) return;

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            boolean isIgnoring = pm.isIgnoringBatteryOptimizations(context.getPackageName());
            if (isIgnoring) {
                binding.layoutBatteryRequest.setVisibility(View.GONE);
                binding.layoutBatteryActive.setVisibility(View.VISIBLE);
            } else {
                binding.layoutBatteryRequest.setVisibility(View.VISIBLE);
                binding.layoutBatteryActive.setVisibility(View.GONE);
            }
        }
    }

    private void setupBackupCard() {
        binding.btnExportBackup.setOnClickListener(v -> {
            exportBackupLauncher.launch("medpills_backup.json");
        });

        binding.btnImportBackup.setOnClickListener(v -> {
            importBackupLauncher.launch(new String[]{"application/json"});
        });
    }

    private void exportData(Uri uri) {
        BackupManager.exportData(requireContext(), uri, new BackupManager.OnBackupListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), R.string.backup_success, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Error al exportar copia de seguridad.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void importData(Uri uri) {
        BackupManager.importData(requireContext(), uri, new BackupManager.OnBackupListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), R.string.backup_success, Toast.LENGTH_SHORT).show();
                loadProfilesList(); // Refresh settings UI
            }

            @Override
            public void onError(Exception e) {
                // Catastrophic data wipe prevented: safely triggers toast, leaving local db untouched
                Toast.makeText(requireContext(), R.string.backup_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupProfilesCard() {
        profileSettingsAdapter = new ProfileSettingsAdapter();
        binding.rvProfilesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvProfilesList.setAdapter(profileSettingsAdapter);

        profileSettingsAdapter.setOnProfileDeleteClickListener(profile -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Eliminar Perfil")
                    .setMessage("¿Estás seguro de que deseas eliminar el perfil \"" + profile.getName() + "\"? Se borrarán todos sus medicamentos, alertas e historial. Esta acción es irreversible.")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        repository.deleteProfile(profile, () -> {
                            Toast.makeText(requireContext(), "Perfil eliminado con éxito", Toast.LENGTH_SHORT).show();
                            loadProfilesList();
                        });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        profileSettingsAdapter.setOnProfileEditClickListener(this::showEditProfileDialog);

        binding.btnAddProfile.setOnClickListener(v -> showAddProfileDialog());
    }

    private void showEditProfileDialog(Profile profile) {
        Context context = requireContext();
        editingProfile = profile;
        currentDialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_profile, null);
        EditText etName = currentDialogView.findViewById(R.id.et_edit_profile_name);
        androidx.recyclerview.widget.RecyclerView rvAvatars = currentDialogView.findViewById(R.id.rv_avatar_selection);
        View btnPickGallery = currentDialogView.findViewById(R.id.btn_pick_gallery);

        etName.setText(profile.getName());
        selectedCustomImageUri = profile.getImageUri();
        updateDialogPhotoPreview(currentDialogView, selectedCustomImageUri);

        btnPickGallery.setOnClickListener(v -> pickImageLauncher.launch(
                new androidx.activity.result.PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        ));

        // Simple list of available avatar resources
        String[] avatarResNames = {"avatar_default", "avatar_1", "avatar_2", "avatar_3", "avatar_4"};
        final String[] selectedAvatar = {profile.getAvatarResourceName()};

        // Inner Adapter for Avatar Selection
        rvAvatars.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        rvAvatars.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
                View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_avatar_selection, p, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
                com.google.android.material.imageview.ShapeableImageView iv = h.itemView.findViewById(R.id.iv_avatar_item);
                String resName = avatarResNames[pos];
                int resId = context.getResources().getIdentifier(resName, "drawable", context.getPackageName());
                iv.setImageResource(resId != 0 ? resId : R.drawable.avatar_default);
                
                // Highlight if selected
                if (resName.equals(selectedAvatar[0]) && selectedCustomImageUri == null) {
                    iv.setStrokeWidth(4f);
                    iv.setStrokeColor(android.content.res.ColorStateList.valueOf(context.getColor(R.color.primary)));
                } else {
                    iv.setStrokeWidth(0f);
                }

                h.itemView.setOnClickListener(v -> {
                    selectedAvatar[0] = resName;
                    selectedCustomImageUri = null; // Clear custom image if avatar is chosen
                    updateDialogPhotoPreview(currentDialogView, null);
                    notifyDataSetChanged();
                });
            }
            @Override public int getItemCount() { return avatarResNames.length; }
        });

        new MaterialAlertDialogBuilder(context)
                .setTitle("Editar Perfil")
                .setView(currentDialogView)
                .setPositiveButton("Guardar", (d, which) -> {
                    String newName = etName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        profile.setName(newName);
                        profile.setAvatarResourceName(selectedAvatar[0]);
                        profile.setImageUri(selectedCustomImageUri);
                        repository.updateProfile(profile, () -> {
                            Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show();
                            loadProfilesList();
                        });
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateDialogPhotoPreview(View dialogView, String uriString) {
        com.google.android.material.imageview.ShapeableImageView iv = dialogView.findViewById(R.id.iv_selected_custom_photo);
        if (uriString != null) {
            try {
                iv.setImageURI(Uri.parse(uriString));
                iv.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e(TAG, "Error setting dialog photo preview URI", e);
                iv.setVisibility(View.GONE);
            }
        } else {
            iv.setVisibility(View.GONE);
        }
    }

    private void loadProfilesList() {
        repository.getAllProfiles(profiles -> {
            if (profiles != null) {
                profileSettingsAdapter.setProfiles(profiles);
            }
        });
    }

    private void showAddProfileDialog() {
        Context context = requireContext();
        
        // Dynamically build EditText input
        final EditText etName = new EditText(context);
        etName.setHint(R.string.profile_name_hint);
        etName.setSingleLine(true);
        
        // Add margins
        int paddingPx = (int) (16 * context.getResources().getDisplayMetrics().density);
        
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.add_profile_btn)
                .setView(etName)
                .setPositiveButton("Crear", (d, which) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Profile newProfile = new Profile(name, "avatar_default");
                        repository.insertProfile(newProfile, () -> {
                            Toast.makeText(context, "Perfil creado con éxito", Toast.LENGTH_SHORT).show();
                            loadProfilesList();
                        });
                    } else {
                        Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .create();

        // Adjust dialog margins for padding
        dialog.setView(etName, paddingPx, paddingPx, paddingPx, paddingPx);
        dialog.show();
    }
}
