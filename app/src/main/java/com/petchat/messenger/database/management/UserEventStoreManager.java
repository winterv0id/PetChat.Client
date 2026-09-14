package com.petchat.messenger.database.management;

import android.util.Log;

import com.petchat.api.objects.events.EventType;
import com.petchat.api.objects.events.UserEvent;
import com.petchat.api.objects.response.sync.UserSnapshot;
import com.petchat.api.signalrclient.external.EventStore;
import com.petchat.messenger.database.PetChatDatabase;
import com.petchat.messenger.database.entities.DbAppliedStateEntity;
import com.petchat.messenger.database.mappers.*;

import java.util.Objects;
import java.util.concurrent.*;

import javax.inject.Inject;

import jakarta.inject.Singleton;

@Singleton
public class UserEventStoreManager implements EventStore {
    private static final String LOG_TAG = UserEventStoreManager.class.getName();
    @Inject
    public UserEventStoreManager(PetChatDatabase db) {
        this.db = db;
    }

    private final PetChatDatabase db;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    public CompletableFuture<Void> saveAppliedEvent(UserEvent event) {
        Log.d(LOG_TAG, String.format("Новое событие (type=%s; seq=%d): %s",
                event.eventType, event.sequence, event.payloadJson));

        return CompletableFuture.runAsync(() -> db.runInTransaction(() -> {
            saveEventPayload(event);
            db.appliedStateDao().upsertLastSequence(new DbAppliedStateEntity(event.sequence));
        }), dbExecutor).exceptionally(ex -> {
            Log.e(LOG_TAG, String.format("Не удалось сохранить event (type=%s)!", event.eventType), ex);
            return null;
        });
    }

    public void saveEventPayload(UserEvent event) {
        switch (event.eventType) {
            case EventType.NEW_MESSAGE -> {
                var message = event.asMessage();
                db.messagesDao().add(DbMessageEntityMapper.fromApiMessageDto(event.asMessage()));
                db.chatsDao().updateLastMessageId(message.chatId, message.id, message.date);
            }
            case EventType.MESSAGE_EDITED -> {
                var payload = event.asMessageEdited();
                db.messagesDao().editMessage(payload.messageId, payload.newText, payload.editDate);
            }
            case EventType.MESSAGE_DELETED -> {
                var payload = event.asMessageDeleted();
                db.messagesDao().delete(payload.messageId);
            }
            case EventType.MESSAGE_READED -> {
                var payload = event.asMessageReaded();
                db.messagesDao().markReadUpTo(payload.chatId, true, payload.upToMessageId, payload.readDate);
            }
            case EventType.NEW_CHAT -> {
                var payload = event.asChat();
                db.chatsDao().add(DbChatEntityMapper.fromApiChatDto(payload));
                db.usersDao().add(DbUserEntityMapper.fromApiUserDto(payload.chatProperties.peer));
            }
            case EventType.USER_ONLINE -> {
                var payload = event.asUserOnlineStatusPayload();
                db.usersDao().updatePresence(payload.userId, true, payload.at);
            }
            case EventType.USER_OFFLINE -> {
                var payload = event.asUserOnlineStatusPayload();
                db.usersDao().updatePresence(payload.userId, false, payload.at);
            }
        }
    }

    @Override
    public CompletableFuture<Long> getLastAppliedSequence() {
        return CompletableFuture.supplyAsync(() -> {
            Long value = db.appliedStateDao().getLastSequenceOrNull();
            return value != null ? value : 0L;
        }, dbExecutor);
    }

    @Override
    public CompletableFuture<Void> applySnapshot(UserSnapshot userSnapshot, long seq) {
        Log.d(LOG_TAG, String.format("Applying shapshot, seq=%d", seq));

        return CompletableFuture.runAsync(() -> db.runInTransaction(() -> {
            clearAllTables();
            insertSnapshotData(userSnapshot);
            db.appliedStateDao().upsertLastSequence(new DbAppliedStateEntity(seq));
        }), dbExecutor);
    }

    public void insertSnapshotData(UserSnapshot userSnapshot) {
        for (var msg : userSnapshot.messages) {
            db.messagesDao().add(DbMessageEntityMapper.fromApiMessageDto(msg));
        }

        for (var chat : userSnapshot.chats) {
            db.chatsDao().add(DbChatEntityMapper.fromApiChatDto(chat));
            db.usersDao().add(DbUserEntityMapper.fromApiUserDto(chat.chatProperties.peer));
        }
    }

    public void clearAllTables() {
        db.messagesDao().clearAll();
        db.chatsDao().clearAll();
        db.usersDao().clearAll();
    }
}