package com.medpills.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "profiles")
public class Profile {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "avatar_resource_name")
    private String avatarResourceName;

    @ColumnInfo(name = "image_uri")
    private String imageUri;

    // Constructors
    public Profile() {}

    public Profile(String name, String avatarResourceName) {
        this.name = name;
        this.avatarResourceName = avatarResourceName;
    }

    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAvatarResourceName() { return avatarResourceName; }
    public void setAvatarResourceName(String avatarResourceName) { this.avatarResourceName = avatarResourceName; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }
}
