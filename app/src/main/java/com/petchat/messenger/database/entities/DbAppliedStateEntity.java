package com.petchat.messenger.database.entities;

import androidx.room.*;
import androidx.annotation.NonNull;

@Entity(tableName = "applied_state")
public class DbAppliedStateEntity {
    public DbAppliedStateEntity() {}
    public DbAppliedStateEntity(long lastSequence) {
        this.lastSequence = lastSequence;
    }
    @PrimaryKey
    @NonNull
    public String key = "global";

    public Long lastSequence;
}