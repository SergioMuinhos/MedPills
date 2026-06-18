package com.medpills;

import android.app.Application;
import com.medpills.utils.ThemeHelper;

public class MedPillsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Global theme initialization
        ThemeHelper.applySettingsTheme(this);
    }
}
