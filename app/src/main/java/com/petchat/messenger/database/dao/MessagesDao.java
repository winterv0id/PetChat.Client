package com.petchat.messenger.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.petchat.messenger.database.entities.DbMessageEntity;
import java.util.List;

@Dao
public interface MessagesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long add(DbMessageEntity message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DbMessageEntity> messages);

    // минимальный index (DESC) :limit сообщений до :beforeIndex
    @Query("SELECT MIN(`index`) FROM (" +
            "SELECT `index` FROM messages " +
            "WHERE chatId = :chatId AND `index` IS NOT NULL AND `index` < :beforeIndex " +
            "ORDER BY `index` DESC LIMIT :limit)")
    Integer getMinIndexOfWindowBefore(String chatId, int beforeIndex, int limit);

    @Query("SELECT * FROM messages " +
            "WHERE chatId = :chatId AND (`index` IS NULL OR `index` >= :minIndex) " +
            "ORDER BY CASE WHEN `index` IS NULL THEN 1 ELSE 0 END DESC, `index` DESC")
    LiveData<List<DbMessageEntity>> observeMessagesFrom(String chatId, int minIndex);

    @Query("SELECT * FROM messages WHERE srvMessageId IN (:serverMessageIds)")
    List<DbMessageEntity> getByMessageIds(List<Integer> serverMessageIds);

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY `index` DESC LIMIT 1")
    DbMessageEntity getLastChatMessage(String chatId);

    @Query("UPDATE messages SET text = :newText, editDate = :editDate, edited = 1 WHERE srvMessageId = :serverMessageId")
    void editMessage(int serverMessageId, String newText, String editDate);

    @Query("UPDATE messages SET srvMessageId = :serverMessageId, `index` = :messageIndex WHERE id = :id")
    void confirmSendedMessage(int id, int serverMessageId, int messageIndex);

    @Query("UPDATE messages SET readed = 1, readDate = :readDate " +
            "WHERE chatId = :chatId AND fromOwner = :fromOwner AND `index` <= " +
            "(SELECT `index` FROM messages WHERE srvMessageId = :upToSrvMessageId)")
    void markReadUpTo(String chatId, boolean fromOwner, int upToSrvMessageId, String readDate);

    @Query("DELETE FROM messages WHERE srvMessageId = :serverMessageId")
    void delete(int serverMessageId);

    @Query("DELETE FROM messages")
    void clearAll();
}