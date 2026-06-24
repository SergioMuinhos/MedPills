package com.medpills.app.ui;

import android.Manifest;
import android.app.AlarmManager;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.medpills.app.R;
import com.medpills.app.databinding.ActivityMainBinding;
import com.medpills.app.utils.ThemeHelper;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private AlertDialog alarmGuardDialog;
    private final FragmentManager fragmentManager = getSupportFragmentManager();

    // Current active fragment tag
    private String activeFragmentTag = "DASHBOARD";

    // Permission launcher for notifications (minSdk is 33, so POST_NOTIFICATIONS is native)
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted");
                } else {
                    Log.w(TAG, "Notification permission denied");
                    Toast.makeText(this, "Se recomiendan las notificaciones para avisarte a tiempo.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        ThemeHelper.applySettingsTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Adjust bottom navigation padding to respect system bars (Edge-to-Edge insets)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation, (v, insets) -> {
            int bottomInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(0, 0, 0, bottomInsets);
            return insets;
        });

        // Setup bottom navigation listener
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                switchFragment(new DashboardFragment(), "DASHBOARD");
                return true;
            } else if (itemId == R.id.nav_logbook) {
                switchFragment(new LogbookFragment(), "LOGBOOK");
                return true;
            } else if (itemId == R.id.nav_settings) {
                switchFragment(new SettingsFragment(), "SETTINGS");
                return true;
            }
            return false;
        });

        // Load default fragment on first launch
        if (savedInstanceState == null) {
            switchFragment(new DashboardFragment(), "DASHBOARD");
            checkAndRequestNotificationPermission();
        } else {
            activeFragmentTag = savedInstanceState.getString("ACTIVE_TAG", "DASHBOARD");
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("ACTIVE_TAG", activeFragmentTag);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check exact alarm permission on resume to verify if the user granted it in Settings
        checkExactAlarmPermission();
    }

    private void switchFragment(Fragment fragment, String tag) {
        activeFragmentTag = tag;
        Fragment existing = fragmentManager.findFragmentByTag(tag);
        if (existing != null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, existing, tag)
                    .commit();
        } else {
            fragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment, tag)
                    .commit();
        }
    }

    private void checkAndRequestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void checkExactAlarmPermission() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        if (!alarmManager.canScheduleExactAlarms()) {
            // Halt flow and display non-dismissible dialog
            showExactAlarmGuardDialog();
        } else {
            // If it is granted, make sure the dialog is dismissed
            dismissExactAlarmGuardDialog();
        }
    }

    private void showExactAlarmGuardDialog() {
        if (alarmGuardDialog != null && alarmGuardDialog.isShowing()) {
            return; // Already showing
        }

        alarmGuardDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.exact_alarm_alert_title)
                .setMessage(R.string.exact_alarm_alert_desc)
                .setCancelable(false) // Non-dismissible
                .setPositiveButton(R.string.exact_alarm_settings_btn, (dialog, which) -> {
                    // Redirect directly to system settings panel for exact alarms
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .create();

        alarmGuardDialog.show();
    }

    private void dismissExactAlarmGuardDialog() {
        if (alarmGuardDialog != null && alarmGuardDialog.isShowing()) {
            alarmGuardDialog.dismiss();
            alarmGuardDialog = null;
        }
    }
}
