package com.medpills.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.medpills.R;
import com.medpills.alarm.AlarmHelper;
import com.medpills.database.AppDatabase;
import com.medpills.database.DatabaseClient;
import com.medpills.database.Medication;
import com.medpills.database.MedicationRepository;
import com.medpills.database.Schedule;
import com.medpills.databinding.ActivityAddMedicationBinding;
import com.medpills.utils.UnitManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddMedicationActivity extends AppCompatActivity {
    private static final String TAG = "AddMedicationActivity";
    private ActivityAddMedicationBinding binding;
    private MedicationRepository repository;
    private UnitManager unitManager;
    private long profileId = 1;
    private long editingMedicationId = -1;
    private Medication editingMedication;
    private Schedule editingSchedule;
    private final Calendar startCalendar = Calendar.getInstance();
    private final Calendar endCalendar = Calendar.getInstance();
    private boolean hasEndDate = false;
    private final List<String> selectedTimes = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddMedicationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        profileId = getIntent().getLongExtra("profile_id", 1);
        editingMedicationId = getIntent().getLongExtra("medication_id", -1);
        repository = new MedicationRepository(this);
        unitManager = new UnitManager(this);

        setupToolbar();
        setupUnitSpinner();
        setupFrequencySpinner();
        setupDatePickers();
        setupTimePicker();
        setupSaveButton();

        if (editingMedicationId != -1) {
            loadMedicationData();
        }
    }

    private void loadMedicationData() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Editar Medicamento");
        }
        binding.btnSave.setText("Actualizar");

        repository.getMedicationById(editingMedicationId, med -> {
            this.editingMedication = med;
            binding.etMedName.setText(med.getName());
            binding.etDosageQuantity.setText(String.valueOf(med.getDosageQuantity()));
            binding.etDosageUnit.setText(med.getDosageUnit());
            if (med.getCurrentStock() != null) {
                binding.etCurrentStock.setText(String.valueOf(med.getCurrentStock()));
            }
            if (med.getLowStockThreshold() != null) {
                binding.etLowStockThreshold.setText(String.valueOf(med.getLowStockThreshold()));
            }
            binding.etMedDescription.setText(med.getDescription());

            repository.getSchedulesForMedication(editingMedicationId, schedules -> {
                if (!schedules.isEmpty()) {
                    this.editingSchedule = schedules.get(0);
                    populateScheduleData(editingSchedule);
                }
            });
        });
    }

    private void populateScheduleData(Schedule schedule) {
        // Frequency
        switch (schedule.getFrequencyType()) {
            case "DAILY":
                binding.spFrequencyType.setSelection(0);
                break;
            case "INTERVAL":
                binding.spFrequencyType.setSelection(1);
                binding.etIntervalHours.setText(String.valueOf(schedule.getIntervalHours()));
                break;
            case "SPECIFIC_DAYS":
                binding.spFrequencyType.setSelection(2);
                String days = schedule.getDaysOfWeek();
                binding.chipMonday.setChecked(days.contains("MONDAY"));
                binding.chipTuesday.setChecked(days.contains("TUESDAY"));
                binding.chipWednesday.setChecked(days.contains("WEDNESDAY"));
                binding.chipThursday.setChecked(days.contains("THURSDAY"));
                binding.chipFriday.setChecked(days.contains("FRIDAY"));
                binding.chipSaturday.setChecked(days.contains("SATURDAY"));
                binding.chipSunday.setChecked(days.contains("SUNDAY"));
                break;
            case "CYCLIC":
                binding.spFrequencyType.setSelection(3);
                binding.etCycleOn.setText(String.valueOf(schedule.getCycleOnDays()));
                binding.etCycleOff.setText(String.valueOf(schedule.getCycleOffDays()));
                break;
        }

        // Start Date
        startCalendar.setTimeInMillis(schedule.getStartDateMillis());
        binding.tvStartDateLabel.setText("Fecha de inicio: " + dateFormat.format(startCalendar.getTime()));

        // End Date
        if (schedule.getEndDateMillis() != null) {
            hasEndDate = true;
            endCalendar.setTimeInMillis(schedule.getEndDateMillis());
            binding.tvEndDateLabel.setText("Fecha de fin: " + dateFormat.format(endCalendar.getTime()));
        }

        // Times
        String[] times = schedule.getTargetTimes().split(",");
        selectedTimes.clear();
        selectedTimes.addAll(Arrays.asList(times));
        rebuildTimesChips();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupUnitSpinner() {
        List<String> units = unitManager.getUnits();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, units);
        binding.etDosageUnit.setAdapter(adapter);
    }

    private void setupFrequencySpinner() {
        String[] frequencies = {
                "Diario (DAILY)", 
                "Intervalo de horas (INTERVAL)", 
                "Días específicos (SPECIFIC_DAYS)", 
                "Ciclo (Toma/Descanso) (CYCLIC)"
        };
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, frequencies);
        binding.spFrequencyType.setAdapter(adapter);

        binding.spFrequencyType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Dynamically show/hide parameter panels depending on selected type
                binding.layoutInterval.setVisibility(View.GONE);
                binding.layoutSpecificDays.setVisibility(View.GONE);
                binding.layoutCyclic.setVisibility(View.GONE);

                if (position == 1) {
                    binding.layoutInterval.setVisibility(View.VISIBLE);
                } else if (position == 2) {
                    binding.layoutSpecificDays.setVisibility(View.VISIBLE);
                } else if (position == 3) {
                    binding.layoutCyclic.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDatePickers() {
        binding.btnPickStartDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(this, 
                    (view, year, month, dayOfMonth) -> {
                        startCalendar.set(Calendar.YEAR, year);
                        startCalendar.set(Calendar.MONTH, month);
                        startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        binding.tvStartDateLabel.setText("Fecha de inicio: " + dateFormat.format(startCalendar.getTime()));
                    },
                    startCalendar.get(Calendar.YEAR),
                    startCalendar.get(Calendar.MONTH),
                    startCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePicker.show();
        });

        binding.btnPickEndDate.setOnClickListener(v -> {
            if (hasEndDate) {
                // Toggle off
                hasEndDate = false;
                binding.tvEndDateLabel.setText("Fecha de fin: Continuo");
                binding.btnPickEndDate.setText("Establecer");
            } else {
                DatePickerDialog datePicker = new DatePickerDialog(this,
                        (view, year, month, dayOfMonth) -> {
                            endCalendar.set(Calendar.YEAR, year);
                            endCalendar.set(Calendar.MONTH, month);
                            endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                            hasEndDate = true;
                            binding.tvEndDateLabel.setText("Fecha de fin: " + dateFormat.format(endCalendar.getTime()));
                            binding.btnPickEndDate.setText("Quitar");
                        },
                        endCalendar.get(Calendar.YEAR),
                        endCalendar.get(Calendar.MONTH),
                        endCalendar.get(Calendar.DAY_OF_MONTH)
                );
                datePicker.show();
            }
        });
    }

    private void setupTimePicker() {
        binding.btnAddTime.setOnClickListener(v -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(8)
                    .setMinute(0)
                    .setTitleText("Añadir hora de recordatorio")
                    .build();

            timePicker.addOnPositiveButtonClickListener(dialog -> {
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();
                String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                
                if (!selectedTimes.contains(formattedTime)) {
                    selectedTimes.add(formattedTime);
                    Collections.sort(selectedTimes);
                    rebuildTimesChips();
                }
            });

            timePicker.show(getSupportFragmentManager(), "MaterialTimePicker");
        });
    }

    private void rebuildTimesChips() {
        binding.cgTimes.removeAllViews();
        for (String time : selectedTimes) {
            Chip chip = new Chip(this);
            chip.setText(time);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                selectedTimes.remove(time);
                rebuildTimesChips();
            });
            binding.cgTimes.addView(chip);
        }
    }

    private void setupSaveButton() {
        binding.btnSave.setOnClickListener(v -> {
            // 1. Gather & Validate Core inputs
            String name = binding.etMedName.getText().toString().trim();
            String dosageQtyStr = binding.etDosageQuantity.getText().toString().trim();
            String dosageUnit = binding.etDosageUnit.getText().toString().trim();
            String currentStockStr = binding.etCurrentStock.getText().toString().trim();
            String lowStockStr = binding.etLowStockThreshold.getText().toString().trim();
            String description = binding.etMedDescription.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(dosageQtyStr) || TextUtils.isEmpty(dosageUnit)) {
                Toast.makeText(this, R.string.empty_fields_error, Toast.LENGTH_SHORT).show();
                return;
            }

            double dosageQuantity;
            Integer currentStock = null;
            Integer lowStockThreshold = null;
            try {
                dosageQuantity = Double.parseDouble(dosageQtyStr);
                if (!TextUtils.isEmpty(currentStockStr)) {
                    currentStock = Integer.parseInt(currentStockStr);
                }
                if (!TextUtils.isEmpty(lowStockStr)) {
                    lowStockThreshold = Integer.parseInt(lowStockStr);
                }
            } catch (NumberFormatException nfe) {
                Toast.makeText(this, R.string.invalid_number_error, Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Schedule Specific Validations
            int freqPos = binding.spFrequencyType.getSelectedItemPosition();
            String frequencyType = "DAILY";
            int intervalHours = 0;
            StringBuilder daysOfWeek = new StringBuilder();
            int cycleOn = 0;
            int cycleOff = 0;

            if (freqPos == 0) { // DAILY
                frequencyType = "DAILY";
            } else if (freqPos == 1) { // INTERVAL
                frequencyType = "INTERVAL";
                String intervalStr = binding.etIntervalHours.getText().toString().trim();
                if (TextUtils.isEmpty(intervalStr)) {
                    Toast.makeText(this, "Por favor, defina el intervalo de horas.", Toast.LENGTH_SHORT).show();
                    return;
                }
                intervalHours = Integer.parseInt(intervalStr);
                if (intervalHours <= 0) {
                    Toast.makeText(this, "El intervalo de horas debe ser mayor a 0.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else if (freqPos == 2) { // SPECIFIC_DAYS
                frequencyType = "SPECIFIC_DAYS";
                if (binding.chipMonday.isChecked()) daysOfWeek.append("MONDAY,");
                if (binding.chipTuesday.isChecked()) daysOfWeek.append("TUESDAY,");
                if (binding.chipWednesday.isChecked()) daysOfWeek.append("WEDNESDAY,");
                if (binding.chipThursday.isChecked()) daysOfWeek.append("THURSDAY,");
                if (binding.chipFriday.isChecked()) daysOfWeek.append("FRIDAY,");
                if (binding.chipSaturday.isChecked()) daysOfWeek.append("SATURDAY,");
                if (binding.chipSunday.isChecked()) daysOfWeek.append("SUNDAY,");

                if (daysOfWeek.length() == 0) {
                    Toast.makeText(this, "Por favor, seleccione al menos un día de la semana.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Strip trailing comma
                daysOfWeek.setLength(daysOfWeek.length() - 1);
            } else if (freqPos == 3) { // CYCLIC
                frequencyType = "CYCLIC";
                String onStr = binding.etCycleOn.getText().toString().trim();
                String offStr = binding.etCycleOff.getText().toString().trim();
                if (TextUtils.isEmpty(onStr) || TextUtils.isEmpty(offStr)) {
                    Toast.makeText(this, "Por favor, defina los días del ciclo.", Toast.LENGTH_SHORT).show();
                    return;
                }
                cycleOn = Integer.parseInt(onStr);
                cycleOff = Integer.parseInt(offStr);
                if (cycleOn <= 0) {
                    Toast.makeText(this, "Los días de toma deben ser mayor a 0.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // 3. Time Pick Validation (Unless it's interval-based, targetTimes is generally required)
            // Note: even for Interval, target times is useful as a starting point. Let's make it mandatory for all.
            if (selectedTimes.isEmpty()) {
                Toast.makeText(this, R.string.no_times_error, Toast.LENGTH_SHORT).show();
                return;
            }

            // Format target times to comma-separated string
            StringBuilder targetTimesSb = new StringBuilder();
            for (String t : selectedTimes) {
                targetTimesSb.append(t).append(",");
            }
            targetTimesSb.setLength(targetTimesSb.length() - 1);

            // 4. Save to Database
            if (editingMedicationId == -1) {
                // INSERT NEW
                Medication medication = new Medication(profileId, name, dosageQuantity, dosageUnit, currentStock, lowStockThreshold);
                medication.setDescription(description);

                startCalendar.set(Calendar.HOUR_OF_DAY, 0);
                startCalendar.set(Calendar.MINUTE, 0);
                startCalendar.set(Calendar.SECOND, 0);
                startCalendar.set(Calendar.MILLISECOND, 0);

                Schedule schedule = new Schedule(
                        -1,
                        frequencyType,
                        intervalHours,
                        daysOfWeek.toString(),
                        cycleOn,
                        cycleOff,
                        startCalendar.getTimeInMillis(),
                        targetTimesSb.toString()
                );

                if (hasEndDate) {
                    endCalendar.set(Calendar.HOUR_OF_DAY, 23);
                    endCalendar.set(Calendar.MINUTE, 59);
                    endCalendar.set(Calendar.SECOND, 59);
                    endCalendar.set(Calendar.MILLISECOND, 999);
                    schedule.setEndDateMillis(endCalendar.getTimeInMillis());
                }

                repository.insertMedicationWithSchedule(medication, schedule, medId -> {
                    Toast.makeText(this, "Medicamento guardado con éxito.", Toast.LENGTH_SHORT).show();
                    schedule.setMedicationId(medId);
                    DatabaseClient.getInstance(this).executor().execute(() -> {
                        List<Schedule> scheds = DatabaseClient.getInstance(this).db().scheduleDao().getSchedulesByMedication(medId);
                        if (!scheds.isEmpty()) {
                            AlarmHelper.scheduleNextAlarm(this, scheds.get(0));
                        }
                    });
                    finish();
                });
            } else {
                // UPDATE EXISTING
                editingMedication.setName(name);
                editingMedication.setDosageQuantity(dosageQuantity);
                editingMedication.setDosageUnit(dosageUnit);
                editingMedication.setCurrentStock(currentStock);
                editingMedication.setLowStockThreshold(lowStockThreshold);
                editingMedication.setDescription(description);

                editingSchedule.setFrequencyType(frequencyType);
                editingSchedule.setIntervalHours(intervalHours);
                editingSchedule.setDaysOfWeek(daysOfWeek.toString());
                editingSchedule.setCycleOnDays(cycleOn);
                editingSchedule.setCycleOffDays(cycleOff);
                editingSchedule.setStartDateMillis(startCalendar.getTimeInMillis());
                editingSchedule.setTargetTimes(targetTimesSb.toString());

                if (hasEndDate) {
                    endCalendar.set(Calendar.HOUR_OF_DAY, 23);
                    endCalendar.set(Calendar.MINUTE, 59);
                    endCalendar.set(Calendar.SECOND, 59);
                    endCalendar.set(Calendar.MILLISECOND, 999);
                    editingSchedule.setEndDateMillis(endCalendar.getTimeInMillis());
                } else {
                    editingSchedule.setEndDateMillis(null);
                }

                repository.updateMedicationWithSchedule(editingMedication, editingSchedule, () -> {
                    Toast.makeText(this, "Medicamento actualizado con éxito.", Toast.LENGTH_SHORT).show();
                    // Update alarm
                    AlarmHelper.scheduleNextAlarm(this, editingSchedule);
                    finish();
                });
            }
        });
    }
}
