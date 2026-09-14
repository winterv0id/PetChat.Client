package com.petchat.messenger.database.dao;

import androidx.room.*;

import com.petchat.messenger.database.entities.DbAppliedStateEntity;

@Dao
public interface AppliedStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertLastSequence(DbAppliedStateEntity sequence);

    @Query("SELECT lastSequence FROM applied_state WHERE `key` = 'global'")
    Long getLastSequenceOrNull();
}