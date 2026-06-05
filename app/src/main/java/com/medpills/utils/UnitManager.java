package com.medpills.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UnitManager {
    private static final String PREF_NAME = "medpills_units";
    private static final String KEY_UNITS = "units_list";
    private static final String[] DEFAULT_UNITS = {"pastilla(s)", "capsula(s)", "ml", "mg", "gota(s)"};

    private final SharedPreferences prefs;

    public UnitManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<String> getUnits() {
        Set<String> unitSet = prefs.getStringSet(KEY_UNITS, null);
        if (unitSet == null) {
            return new ArrayList<>(Arrays.asList(DEFAULT_UNITS));
        }
        return new ArrayList<>(unitSet);
    }

    public void addUnit(String unit) {
        List<String> current = getUnits();
        if (!current.contains(unit)) {
            current.add(unit);
            saveUnits(current);
        }
    }

    public void removeUnit(String unit) {
        List<String> current = getUnits();
        current.remove(unit);
        saveUnits(current);
    }

    private void saveUnits(List<String> units) {
        prefs.edit().putStringSet(KEY_UNITS, new HashSet<>(units)).apply();
    }
}
