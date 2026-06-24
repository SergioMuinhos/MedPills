package com.medpills.app.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medpills.app.R;
import com.medpills.app.databinding.ItemCalendarBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    public static class CalendarDay {
        public final Date date;
        public final String dayNum;
        public final String dayName;
        public boolean isSelected;

        public CalendarDay(Date date, String dayNum, String dayName, boolean isSelected) {
            this.date = date;
            this.dayNum = dayNum;
            this.dayName = dayName;
            this.isSelected = isSelected;
        }
    }

    private final List<CalendarDay> days = new ArrayList<>();
    private OnDateSelectedListener listener;
    private final Locale spanishLocale = new Locale("es", "ES");

    public interface OnDateSelectedListener {
        void onDateSelected(CalendarDay day);
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    public CalendarAdapter() {
        generateRollingWeek();
    }

    private void generateRollingWeek() {
        days.clear();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -3); // Start 3 days ago

        SimpleDateFormat numFormat = new SimpleDateFormat("d", spanishLocale);
        SimpleDateFormat nameFormat = new SimpleDateFormat("EEE", spanishLocale);

        for (int i = 0; i < 7; i++) {
            Date date = cal.getTime();
            boolean isToday = i == 3; // Center is today
            days.add(new CalendarDay(
                    date,
                    numFormat.format(date),
                    nameFormat.format(date).toUpperCase(),
                    isToday
            ));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    public Date getSelectedDate() {
        for (CalendarDay day : days) {
            if (day.isSelected) {
                return day.date;
            }
        }
        return new Date();
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCalendarBinding binding = ItemCalendarBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CalendarViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        holder.bind(day);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    class CalendarViewHolder extends RecyclerView.ViewHolder {
        private final ItemCalendarBinding binding;

        public CalendarViewHolder(ItemCalendarBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CalendarDay day) {
            binding.tvDayNum.setText(day.dayNum);
            binding.tvDayName.setText(day.dayName);
            
            float density = binding.getRoot().getContext().getResources().getDisplayMetrics().density;
            
            if (day.isSelected) {
                binding.cardDay.setStrokeColor(binding.getRoot().getContext().getColor(R.color.primary));
                binding.cardDay.setCardBackgroundColor(binding.getRoot().getContext().getColor(R.color.secondary));
                binding.cardDay.setCardElevation(6 * density);
                binding.tvDayNum.setTextColor(binding.getRoot().getContext().getColor(R.color.primary));
                binding.tvDayName.setTextColor(binding.getRoot().getContext().getColor(R.color.primary));
            } else {
                binding.cardDay.setStrokeColor(binding.getRoot().getContext().getColor(R.color.surface_light));
                binding.cardDay.setCardBackgroundColor(binding.getRoot().getContext().getColor(R.color.glass_tint_light));
                binding.cardDay.setCardElevation(2 * density);
                binding.tvDayNum.setTextColor(binding.getRoot().getContext().getColor(R.color.text_primary_light));
                binding.tvDayName.setTextColor(binding.getRoot().getContext().getColor(R.color.text_secondary_light));
            }

            binding.getRoot().setOnClickListener(v -> {
                for (CalendarDay d : days) {
                    d.isSelected = false;
                }
                day.isSelected = true;
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onDateSelected(day);
                }
            });
        }
    }
}
