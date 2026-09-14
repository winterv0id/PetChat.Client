package com.petchat.messenger.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.Instant;

import androidx.annotation.Nullable;

@Entity(tableName = "users")
public class DbUserEntity {
    public DbUserEntity() {}

    @PrimaryKey
    public int id;

    public String shortName;
    public String nickname;
    public boolean isOnline = false;
    public @Nullable String status;
    public @Nullable String imageUrl;

    public String lastSeen;
    public Instant getLastSeenAsInstant() {
        return Instant.parse(lastSeen);
    }
}