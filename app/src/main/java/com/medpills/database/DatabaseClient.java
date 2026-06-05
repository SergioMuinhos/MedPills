package com.medpills.database;

import android.content.Context;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseClient {
    private final Context context;
    private final AppDatabase appDatabase;
    private final ExecutorService executorService;
    private static DatabaseClient instance;

    private DatabaseClient(Context context) {
        this.context = context.getApplicationContext();
        this.appDatabase = AppDatabase.getDatabase(this.context);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public static synchronized DatabaseClient getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseClient(context);
        }
        return instance;
    }

    public AppDatabase db() {
        return appDatabase;
    }

    public ExecutorService executor() {
        return executorService;
    }
}
