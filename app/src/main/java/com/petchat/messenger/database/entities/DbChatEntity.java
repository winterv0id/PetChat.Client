package com.petchat.messenger.database.entities;

import androidx.room.*;
import androidx.annotation.NonNull;

import com.petchat.messenger.database.converters.ArrayListIntegerConverter;

import java.time.Instant;
import java.util.ArrayList;

@Entity(tableName = "chats")
@TypeConverters({ArrayListIntegerConverter.class})
public class DbChatEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    public int messageIndex = 0;
    public ArrayList<Integer> members;
    public Integer lastSrvMessageId = null;
    public String imageUrl;
    public String chatName;

    public int peerId;
    public boolean notificationsEnabled = true;
    public boolean archived = false;
    public boolean pinned = false;

    public String lastActivityAt = String.valueOf(Instant.now());
    public Instant getLastActivityAtInstant() {
        return Instant.parse(lastActivityAt);
    }

    @Ignore
    public DbMessageEntity lastMessage;
    @Ignore
    public DbUserEntity peer;
    @Ignore
    public int unreadCount = 0;

    public static String getChatId(int userId, int peerId) {
        int min = Math.min(userId, peerId);
        int max = Math.max(userId, peerId);
        return min + "_" + max;
    }

    public static DbChatEntity copy(DbChatEntity source) {
        DbChatEntity copy = new DbChatEntity();

        copy.id = source.id;
        copy.messageIndex = source.messageIndex;
        copy.members = source.members;
        copy.lastSrvMessageId = source.lastSrvMessageId;
        copy.imageUrl = source.imageUrl;
        copy.chatName = source.chatName;
        copy.peerId = source.peerId;
        copy.notificationsEnabled = source.notificationsEnabled;
        copy.archived = source.archived;
        copy.pinned = source.pinned;
        copy.lastActivityAt = source.lastActivityAt;

        return copy;
    }
}