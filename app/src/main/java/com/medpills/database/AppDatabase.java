package com.medpills.database;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.migration.Migration;

@Database(entities = {Profile.class, Medication.class, Schedule.class, IntakeLog.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";

    public abstract ProfileDao profileDao();
    public abstract MedicationDao medicationDao();
    public abstract ScheduleDao scheduleDao();
    public abstract IntakeLogDao intakeLogDao();

    private static volatile AppDatabase INSTANCE;

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE profiles ADD COLUMN image_uri TEXT");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE intake_logs ADD COLUMN scheduled_time_millis INTEGER NOT NULL DEFAULT 0");
            database.execSQL("UPDATE intake_logs SET scheduled_time_millis = timestamp_millis");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Recreate medications table to handle nullability changes for stock columns and add description
            database.execSQL("CREATE TABLE `medications_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `profile_id` INTEGER NOT NULL, `name` TEXT, `dosage_quantity` REAL NOT NULL, `dosage_unit` TEXT, `current_stock` INTEGER, `low_stock_threshold` INTEGER, `description` TEXT, FOREIGN KEY(`profile_id`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            
            // Copy data from the old table to the new one
            database.execSQL("INSERT INTO `medications_new` (`id`, `profile_id`, `name`, `dosage_quantity`, `dosage_unit`, `current_stock`, `low_stock_threshold`) " +
                    "SELECT `id`, `profile_id`, `name`, `dosage_quantity`, `dosage_unit`, `current_stock`, `low_stock_threshold` FROM `medications` ");
            
            database.execSQL("DROP TABLE `medications` ");
            database.execSQL("ALTER TABLE `medications_new` RENAME TO `medications` ");
            
            // Recreate the index
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_medications_profile_id` ON `medications` (`profile_id`) ");

            // Add end_date_millis to schedules (this one can be added via ALTER TABLE as it is new and nullable)
            database.execSQL("ALTER TABLE `schedules` ADD COLUMN `end_date_millis` INTEGER");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "medpills.db")
                            .fallbackToDestructiveMigration()
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Log.d(TAG, "Database onCreate triggered.");
                                    bootstrapDefaultProfile(context);
                                }

                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    Log.d(TAG, "Database onOpen triggered.");
                                    bootstrapDefaultProfile(context);
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void bootstrapDefaultProfile(Context context) {
        new Thread(() -> {
            try {
                AppDatabase db = getDatabase(context);
                ProfileDao profileDao = db.profileDao();
                int count = profileDao.getProfileCount();
                if (count == 0) {
                    Log.d(TAG, "No profiles found. Bootstrapping default profile.");
                    Profile defaultProfile = new Profile("Usuario Principal", "avatar_default");
                    defaultProfile.setId(1); // Set explicit ID to 1
                    profileDao.insert(defaultProfile);
                    Log.d(TAG, "Default profile successfully bootstrapped.");
                } else {
                    Log.d(TAG, "Profiles already exist (" + count + "). Bootstrapping skipped.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error bootstrapping default profile", e);
            }
        }).start();
    }
}
