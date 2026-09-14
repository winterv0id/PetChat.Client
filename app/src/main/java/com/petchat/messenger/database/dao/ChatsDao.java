package com.petchat.messenger.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.petchat.messenger.database.entities.DbChatEntity;
import java.util.List;
import java.util.Map;

@Dao
public interface ChatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void add(DbChatEntity chat);

    @Query("SELECT * FROM chats WHERE archived = 0 ORDER BY lastActivityAt DESC")
    LiveData<List<DbChatEntity>> observeRecentChats();

    @Query("SELECT * FROM chats WHERE archived = 1 ORDER BY lastActivityAt DESC")
    LiveData<List<DbChatEntity>> observeRecentArchivedChats();

    @Query("SELECT chatId, COUNT(*) AS unreadCount FROM messages WHERE fromOwner = 0 AND readed = 0 GROUP BY chatId")
    LiveData<Map<@MapColumn(columnName = "chatId") String, @MapColumn(columnName = "unreadCount") Integer>> observeUnreadCounts();

    @Query("UPDATE chats SET lastSrvMessageId = :serverMessageId, lastActivityAt = :lastActivity WHERE id = :chatId")
    void updateLastMessageId(String chatId, int serverMessageId, String lastActivity);

    @Query("UPDATE chats SET messageIndex = :messageIndex WHERE id = :chatId")
    void updateMessagesIndex(String chatId, int messageIndex);

    @Query("UPDATE chats SET archived = :archived WHERE id = :chatId")
    void setArchived(String chatId, boolean archived);

    @Query("DELETE FROM chats WHERE id = :chatId")
    void delete(String chatId);

    @Query("DELETE FROM chats")
    void clearAll();
}