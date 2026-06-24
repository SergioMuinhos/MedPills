package com.medpills.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.medpills.app.R;
import com.medpills.app.database.DatabaseClient;
import com.medpills.app.database.IntakeLog;
import com.medpills.app.database.Medication;
import com.medpills.app.database.MedicationRepository;
import com.medpills.app.database.Profile;
import com.medpills.app.database.Schedule;
import com.medpills.app.databinding.FragmentDashboardBinding;
import com.medpills.app.utils.DateTimeUtils;
import com.medpills.app.utils.ScheduleCalculator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";
    private FragmentDashboardBinding binding;
    private MedicationRepository repository;
    private ProfileAdapter profileAdapter;
    private CalendarAdapter calendarAdapter;
    private IntakeAdapter intakeAdapter;
    private long selectedProfileId = 1;
    private Date selectedDate = new Date();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new MedicationRepository(requireContext());

        setupAdapters();
        setupListeners();
        loadProfiles();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadIntakeSchedules();
    }

    private void setupAdapters() {
        profileAdapter = new ProfileAdapter();
        binding.rvProfiles.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvProfiles.setAdapter(profileAdapter);
        profileAdapter.setOnProfileSelectedListener(profile -> {
            selectedProfileId = profile.getId();
            loadIntakeSchedules();
        });

        calendarAdapter = new CalendarAdapter();
        binding.rvCalendar.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvCalendar.setAdapter(calendarAdapter);
        calendarAdapter.setOnDateSelectedListener(day -> {
            selectedDate = day.date;
            loadIntakeSchedules();
        });

        intakeAdapter = new IntakeAdapter();
        binding.rvIntakes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvIntakes.setAdapter(intakeAdapter);
        intakeAdapter.setOnIntakeInteractionListener(new IntakeAdapter.OnIntakeInteractionListener() {
            @Override
            public void onRecordIntake(IntakeItem item, String status, int position) {
                recordIntake(item, status, false, position);
            }

            @Override
            public void onUndoIntake(IntakeItem item, int position) {
                showUndoDialogFromPanel(item, position);
            }

            @Override
            public void onEditMedication(IntakeItem item) {
                Intent intent = new Intent(requireActivity(), AddMedicationActivity.class);
                intent.putExtra("profile_id", selectedProfileId);
                intent.putExtra("medication_id", item.getMedication().getId());
                startActivity(intent);
            }
        });

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                IntakeItem item = intakeAdapter.getItems().get(position);
                if (!"PENDING".equals(item.getStatus())) {
                    intakeAdapter.notifyItemChanged(position);
                    return;
                }
                if (direction == ItemTouchHelper.RIGHT) {
                    recordIntake(item, "TAKEN", false, position);
                } else if (direction == ItemTouchHelper.LEFT) {
                    recordIntake(item, "SKIPPED", false, position);
                }
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                IntakeItem item = intakeAdapter.getItems().get(position);
                if (!"PENDING".equals(item.getStatus())) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvIntakes);
    }

    private void setupListeners() {
        binding.fabAddMedication.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), AddMedicationActivity.class);
            intent.putExtra("profile_id", selectedProfileId);
            startActivity(intent);
        });
    }

    private void loadProfiles() {
        repository.getAllProfiles(profiles -> {
            if (profiles != null && !profiles.isEmpty()) {
                profileAdapter.setProfiles(profiles);
                profileAdapter.setSelectedProfileId(selectedProfileId);
                loadIntakeSchedules();
            }
        });
    }

    private void loadIntakeSchedules() {
        if (getContext() == null) return;
        long startDay = DateTimeUtils.getStartOfDay(selectedDate);
        long endDay = DateTimeUtils.getEndOfDay(selectedDate);

        repository.getMedicationsByProfile(selectedProfileId, medications -> {
            List<IntakeItem> intakeList = new ArrayList<>();
            if (medications == null || medications.isEmpty()) {
                updateUIList(intakeList);
                return;
            }

            DatabaseClient.getInstance(requireContext()).executor().execute(() -> {
                try {
                    for (Medication med : medications) {
                        List<Schedule> schedules = DatabaseClient.getInstance(requireContext()).db().scheduleDao().getSchedulesByMedication(med.getId());
                        List<IntakeLog> logs = DatabaseClient.getInstance(requireContext()).db().intakeLogDao().getLogsFiltered(selectedProfileId, startDay, endDay);
                        for (Schedule sched : schedules) {
                            if (sched.getEndDateMillis() != null && sched.getEndDateMillis() < startDay) continue;
                            List<Long> scheduledTimes = ScheduleCalculator.calculateScheduledTimesForDay(sched, selectedDate);
                            for (long schedTime : scheduledTimes) {
                                IntakeLog matchedLog = findMatchedLog(logs, med.getId(), schedTime);
                                String status = (matchedLog != null) ? matchedLog.getStatus() : "PENDING";
                                IntakeItem item = new IntakeItem(sched, med, schedTime, status);
                                if (matchedLog != null) item.setLogId(matchedLog.getId());
                                intakeList.add(item);
                            }
                        }
                    }
                    intakeList.sort((a, b) -> Long.compare(a.getScheduledTimeMillis(), b.getScheduledTimeMillis()));
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> updateUIList(intakeList));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error calculating intake checklist", e);
                }
            });
        });
    }

    private void updateUIList(List<IntakeItem> list) {
        if (list.isEmpty()) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.rvIntakes.setVisibility(View.GONE);
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
            binding.rvIntakes.setVisibility(View.VISIBLE);
            intakeAdapter.setItems(list);
        }
    }

    private IntakeLog findMatchedLog(List<IntakeLog> logs, long medId, long scheduledTime) {
        if (logs == null) return null;
        for (IntakeLog log : logs) {
            if (log.getMedicationId() == medId && log.getScheduledTimeMillis() == scheduledTime) return log;
        }
        return null;
    }

    private void recordIntake(IntakeItem item, String status, boolean force, int position) {
        long actionTime = System.currentTimeMillis();
        long scheduledTime = item.getScheduledTimeMillis();
        IntakeLog log = new IntakeLog(item.getMedication().getId(), selectedProfileId, actionTime, scheduledTime, status);

        repository.insertIntakeLog(log, force, new MedicationRepository.OnLogResultListener() {
            @Override
            public void onSuccess(long logId) {
                if (getContext() != null && getView() != null) {
                    String message = "TAKEN".equals(status) ? getString(R.string.medication_taken) : getString(R.string.medication_skipped);
                    Snackbar snackbar = Snackbar.make(getView(), message, Snackbar.LENGTH_LONG);
                    snackbar.setAction(R.string.undo, v -> {
                        repository.undoIntakeLog(logId, item.getMedication().getId(), () -> {
                            Toast.makeText(requireContext(), R.string.undo_success, Toast.LENGTH_SHORT).show();
                            com.medpills.app.alarm.AlarmHelper.scheduleNextAlarm(requireContext(), item.getSchedule());
                            loadIntakeSchedules();
                        });
                    });
                    snackbar.show();
                    com.medpills.app.alarm.AlarmHelper.cancelAlarm(getContext(), item.getSchedule());
                    com.medpills.app.alarm.AlarmHelper.scheduleNextAlarmAfter(getContext(), item.getSchedule(), item.getScheduledTimeMillis());
                }
                loadIntakeSchedules();
            }

            @Override
            public void onOverdoseWarning(IntakeLog duplicateLog, String message) {
                showOverdoseWarningDialog(duplicateLog, item.getMedication().getName(), position);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(), "Error al guardar el registro", Toast.LENGTH_SHORT).show();
                intakeAdapter.notifyItemChanged(position);
            }
        });
    }

    private void showOverdoseWarningDialog(IntakeLog duplicateLog, String medName, int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.overdose_alert_title)
                .setMessage(getString(R.string.overdose_alert_desc, medName))
                .setCancelable(false)
                .setPositiveButton(R.string.overdose_confirm, (dialog, which) -> {
                    IntakeItem item = intakeAdapter.getItems().get(position);
                    recordIntake(item, "TAKEN", true, position);
                })
                .setNegativeButton(R.string.overdose_cancel, (dialog, which) -> {
                    intakeAdapter.notifyItemChanged(position);
                })
                .show();
    }

    private void showUndoDialogFromPanel(IntakeItem item, int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Revertir registro")
                .setMessage("¿Deseas marcar esta toma como pendiente de nuevo? Se restaurará el stock.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton("REVERTIR", (dialog, which) -> {
                    repository.undoIntakeLog(item.getLogId(), item.getMedication().getId(), () -> {
                        com.medpills.app.alarm.AlarmHelper.scheduleNextAlarm(requireContext(), item.getSchedule());
                        loadIntakeSchedules();
                    });
                })
                .show();
    }
}
