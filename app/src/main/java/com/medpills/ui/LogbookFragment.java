package com.medpills.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.medpills.R;
import com.medpills.database.DatabaseClient;
import com.medpills.database.IntakeLog;
import com.medpills.database.Medication;
import com.medpills.database.MedicationRepository;
import com.medpills.database.Profile;
import com.medpills.databinding.FragmentLogbookBinding;
import com.medpills.utils.DateTimeUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LogbookFragment extends Fragment {
    private static final String TAG = "LogbookFragment";
    private FragmentLogbookBinding binding;
    private MedicationRepository repository;
    private LogAdapter logAdapter;

    private final List<Profile> profiles = new ArrayList<>();
    private long selectedProfileId = 1;
    private final Calendar filterStartCalendar = Calendar.getInstance();
    private final Calendar filterEndCalendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLogbookBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new MedicationRepository(requireContext());

        // Default: last 7 days range
        filterStartCalendar.add(Calendar.DAY_OF_YEAR, -7);

        setupAdapter();
        setupFilters();
        loadProfiles();
    }

    private void setupAdapter() {
        logAdapter = new LogAdapter();
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(logAdapter);
    }

    private void setupFilters() {
        binding.btnFilterStartDate.setText(dateFormat.format(filterStartCalendar.getTime()));
        binding.btnFilterEndDate.setText(dateFormat.format(filterEndCalendar.getTime()));

        // Start Date picker
        binding.btnFilterStartDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(requireContext(), 
                    (view, year, month, dayOfMonth) -> {
                        filterStartCalendar.set(Calendar.YEAR, year);
                        filterStartCalendar.set(Calendar.MONTH, month);
                        filterStartCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        binding.btnFilterStartDate.setText(dateFormat.format(filterStartCalendar.getTime()));
                        loadLogs();
                    },
                    filterStartCalendar.get(Calendar.YEAR),
                    filterStartCalendar.get(Calendar.MONTH),
                    filterStartCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePicker.show();
        });

        // End Date picker
        binding.btnFilterEndDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(requireContext(), 
                    (view, year, month, dayOfMonth) -> {
                        filterEndCalendar.set(Calendar.YEAR, year);
                        filterEndCalendar.set(Calendar.MONTH, month);
                        filterEndCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        binding.btnFilterEndDate.setText(dateFormat.format(filterEndCalendar.getTime()));
                        loadLogs();
                    },
                    filterEndCalendar.get(Calendar.YEAR),
                    filterEndCalendar.get(Calendar.MONTH),
                    filterEndCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePicker.show();
        });

        // Spinner Selector Listener
        binding.spFilterProfile.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < profiles.size()) {
                    selectedProfileId = profiles.get(position).getId();
                    loadLogs();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadProfiles() {
        repository.getAllProfiles(profileList -> {
            if (profileList != null && !profileList.isEmpty()) {
                profiles.clear();
                profiles.addAll(profileList);

                List<String> names = new ArrayList<>();
                for (Profile p : profiles) {
                    names.add(p.getName());
                }

                if (getContext() != null) {
                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), 
                            android.R.layout.simple_spinner_dropdown_item, names);
                    binding.spFilterProfile.setAdapter(spinnerAdapter);
                }

                // Default to select first profile (ID 1)
                for (int i = 0; i < profiles.size(); i++) {
                    if (profiles.get(i).getId() == selectedProfileId) {
                        binding.spFilterProfile.setSelection(i);
                        break;
                    }
                }
                loadLogs();
            }
        });
    }

    private void loadLogs() {
        if (getContext() == null) return;

        long startTime = DateTimeUtils.getStartOfDay(filterStartCalendar.getTime());
        long endTime = DateTimeUtils.getEndOfDay(filterEndCalendar.getTime());

        repository.getLogsFiltered(selectedProfileId, startTime, endTime, logs -> {
            if (logs == null || logs.isEmpty()) {
                showEmptyState(true);
                return;
            }

            // Map and resolve Medication names and Profile names asynchronously
            DatabaseClient.getInstance(requireContext()).executor().execute(() -> {
                try {
                    List<Medication> medications = DatabaseClient.getInstance(requireContext()).db().medicationDao().getAllMedications();
                    List<Profile> profileList = DatabaseClient.getInstance(requireContext()).db().profileDao().getAllProfiles();

                    Map<Long, String> medNameMap = new HashMap<>();
                    for (Medication m : medications) {
                        medNameMap.put(m.getId(), m.getName());
                    }

                    Map<Long, String> profileNameMap = new HashMap<>();
                    for (Profile p : profileList) {
                        profileNameMap.put(p.getId(), p.getName());
                    }

                    List<LogDisplayItem> displayList = new ArrayList<>();
                    for (IntakeLog log : logs) {
                        String medName = medNameMap.containsKey(log.getMedicationId()) ? medNameMap.get(log.getMedicationId()) : "Desconocido";
                        String profName = profileNameMap.containsKey(log.getProfileId()) ? profileNameMap.get(log.getProfileId()) : "Desconocido";
                        displayList.add(new LogDisplayItem(log, medName, profName));
                    }

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            showEmptyState(false);
                            logAdapter.setItems(displayList);
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error displaying history logs", e);
                }
            });
        });
    }

    private void showEmptyState(boolean isEmpty) {
        if (isEmpty) {
            binding.tvHistoryEmpty.setVisibility(View.VISIBLE);
            binding.rvHistory.setVisibility(View.GONE);
        } else {
            binding.tvHistoryEmpty.setVisibility(View.GONE);
            binding.rvHistory.setVisibility(View.VISIBLE);
        }
    }
}
