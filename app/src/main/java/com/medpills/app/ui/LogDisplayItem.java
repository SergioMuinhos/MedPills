package com.medpills.app.ui;

import com.medpills.app.database.IntakeLog;

public class LogDisplayItem {
    public final IntakeLog log;
    public final String medicationName;
    public final String profileName;

    public LogDisplayItem(IntakeLog log, String medicationName, String profileName) {
        this.log = log;
        this.medicationName = medicationName;
        this.profileName = profileName;
    }
}
