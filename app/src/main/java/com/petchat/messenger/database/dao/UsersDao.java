package com.petchat.messenger.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;

import com.petchat.messenger.database.entities.DbUserEntity;

import java.util.List;

@Dao
public interface UsersDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void add(DbUserEntity user);

    @Query("SELECT * FROM users WHERE id = :userId")
    LiveData<DbUserEntity> observeUser(int userId);

    @Query("SELECT * FROM users WHERE id = :userId")
    DbUserEntity getUser(int userId);

    @Query("SELECT * FROM users WHERE id IN (:userIds)")
    List<DbUserEntity> getUsers(List<Integer> userIds);

    @Query("SELECT EXISTS(SELECT * FROM users WHERE id = :userId)")
    boolean exists(int userId);

    @Query("UPDATE users SET isOnline = :online, lastSeen = :lastSeen WHERE id = :userId")
    void updatePresence(int userId, boolean online, String lastSeen);

    @Query("DELETE FROM users")
    void clearAll();
}