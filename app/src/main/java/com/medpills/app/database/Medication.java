package com.medpills.app.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "medications",
    foreignKeys = @ForeignKey(
        entity = Profile.class,
        parentColumns = "id",
        childColumns = "profile_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("profile_id")}
)
public class Medication {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "profile_id")
    private long profileId;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "dosage_quantity")
    private double dosageQuantity;

    @ColumnInfo(name = "dosage_unit")
    private String dosageUnit;

    @ColumnInfo(name = "current_stock")
    private Integer currentStock; // Nullable for optional tracking

    @ColumnInfo(name = "low_stock_threshold")
    private Integer lowStockThreshold; // Nullable

    @ColumnInfo(name = "description")
    private String description;

    // Constructors
    public Medication() {}

    public Medication(long profileId, String name, double dosageQuantity, String dosageUnit, Integer currentStock, Integer lowStockThreshold) {
        this.profileId = profileId;
        this.name = name;
        this.dosageQuantity = dosageQuantity;
        this.dosageUnit = dosageUnit;
        this.currentStock = currentStock;
        this.lowStockThreshold = lowStockThreshold;
    }

    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getProfileId() { return profileId; }
    public void setProfileId(long profileId) { this.profileId = profileId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getDosageQuantity() { return dosageQuantity; }
    public void setDosageQuantity(double dosageQuantity) { this.dosageQuantity = dosageQuantity; }

    public String getDosageUnit() { return dosageUnit; }
    public void setDosageUnit(String dosageUnit) { this.dosageUnit = dosageUnit; }

    public Integer getCurrentStock() { return currentStock; }
    public void setCurrentStock(Integer currentStock) { this.currentStock = currentStock; }

    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
