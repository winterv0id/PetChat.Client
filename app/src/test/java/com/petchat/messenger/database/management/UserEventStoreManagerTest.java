package com.petchat.messenger.database.management;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.petchat.api.objects.events.EventType;
import com.petchat.api.objects.events.UserEvent;
import com.petchat.api.objects.response.sync.UserSnapshot;
import com.petchat.api.objects.serverdto.*;
import com.petchat.messenger.database.PetChatDatabase;
import com.petchat.messenger.database.entities.DbChatEntity;
import com.petchat.messenger.database.entities.DbMessageEntity;
import com.petchat.messenger.database.entities.DbUserEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * {@link UserEventStoreManager} - реализация контракта {@code EventStore} из petchat-client-api поверх Room.
 * <p>Используется in-memory Room БД.
 */
@RunWith(RobolectricTestRunner.class)
public class UserEventStoreManagerTest {
    private PetChatDatabase db;
    private UserEventStoreManager sut;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, PetChatDatabase.class)
                .allowMainThreadQueries()
                .build();
        sut = new UserEventStoreManager(db);
    }

    @After
    public void shutdown() {
        db.close();
    }

    // region saveAppliedEvent
    // NEW_MESSAGE
    @Test
    public void saveAppliedEvent_NewMessage_SaveMessage_UpdatesChatPreview_AndAppliedSequence()
            throws ExecutionException, InterruptedException, TimeoutException {
        int fromId = 14, peerId = 88, messageId = 100, sequence = 101;
        String chatId = createChat(fromId, peerId);

        UserEvent evt = messageEvent(sequence, messageId, chatId, fromId, peerId, "hello world");
        sut.saveAppliedEvent(evt).get(2, TimeUnit.SECONDS);

        List<DbMessageEntity> saved = db.messagesDao().getByMessageIds(List.of(messageId));
        assertEquals(1, saved.size());
        assertEquals("hello world", saved.getFirst().text);
        assertEquals(fromId, saved.getFirst().fromId);

        assertEquals(messageId, getChatLastSrvMessageId(chatId));
        assertEquals(sequence, sut.getLastAppliedSequence().get(2, TimeUnit.SECONDS).longValue());
    }

    // MESSAGE_EDITED / DELETED
    @Test
    public void saveAppliedEvent_MessageEdited_UpdatesTextAndEditedFlag()
            throws ExecutionException, InterruptedException, TimeoutException {
        int fromId = 1, peerId = 2, messageId = 10, sequence = 20;
        String chatId = DbChatEntity.getChatId(fromId, peerId);

        seedMessage(messageId, chatId, "исходный текст");

        UserEvent evt = editEvent(sequence, messageId, "редактировано");
        sut.saveAppliedEvent(evt).get(2, TimeUnit.SECONDS);

        DbMessageEntity edited = db.messagesDao().getByMessageIds(List.of(messageId)).getFirst();
        assertEquals("редактировано", edited.text);
        assertTrue(edited.edited);
    }

    @Test
    public void saveAppliedEvent_MessageDeleted_RemovesMessageFromDb()
            throws ExecutionException, InterruptedException, TimeoutException {
        int fromId = 100, peerId = 101, messageId = 200, sequence = 202;
        String chatId = DbChatEntity.getChatId(fromId, peerId);

        seedMessage(messageId, chatId, "будет удалено");

        UserEvent evt = deleteEvent(sequence, messageId);
        sut.saveAppliedEvent(evt).get(2, TimeUnit.SECONDS);

        assertTrue(db.messagesDao().getByMessageIds(List.of(messageId)).isEmpty());
    }

    // presence
    @Test
    public void saveAppliedEvent_UserOnline_UpdatesPresence()
            throws ExecutionException, InterruptedException, TimeoutException {
        int userId = 500, sequence = 1000;
        String editedAt = "2026-01-01T00:00:00Z";

        createUser(userId);

        UserEvent evt = presenceEvent(sequence, EventType.USER_ONLINE, userId, editedAt);
        sut.saveAppliedEvent(evt).get(2, TimeUnit.SECONDS);

        DbUserEntity user = db.usersDao().getUser(userId);
        assertTrue(user.isOnline);
        assertEquals(editedAt, user.lastSeen);
    }

    // region getLastAppliedSequence
    @Test
    public void getLastAppliedSequence_EmptyDatabase_ReturnsZero()
            throws ExecutionException, InterruptedException, TimeoutException {
        assertEquals(0L, sut.getLastAppliedSequence().get(2, TimeUnit.SECONDS).longValue());
    }
    // endregion

    // region applySnapshot
    @Test
    public void applySnapshot_ClearsOldData_InsertsSnapshotContent_AndSetsSequence()
            throws ExecutionException, InterruptedException, TimeoutException {
        int fromId = 5000, peerId = 5005, messageId = 10000, sequence = 12222;
        String chatId = DbChatEntity.getChatId(fromId, peerId);

        seedMessage(1, "old_chat", "clear me!");

        UserSnapshot snapshot = new UserSnapshot();
        snapshot.chats = new ChatDto[] { chatDto(chatId, fromId, peerId) };
        snapshot.messages = new MessageDto[] { messageDto(messageId, chatId, fromId, peerId, "новое сообщение") };

        sut.applySnapshot(snapshot, sequence).get(2, TimeUnit.SECONDS);

        assertTrue("старые данные должны быть очищены",
                db.messagesDao().getByMessageIds(List.of(1)).isEmpty());
        assertEquals(1, db.messagesDao().getByMessageIds(List.of(messageId)).size());
        assertEquals(sequence, sut.getLastAppliedSequence().get(2, TimeUnit.SECONDS).longValue());
    }

    @Test
    public void applySnapshot_FailureMidway_RollsBackTransaction_OldDataSurvives() {
        int fromId = 7007, peerId = 8008, sequence = 12345;
        String chatId = DbChatEntity.getChatId(fromId, peerId);

        seedMessage(1, "old_chat", "должно выжить");

        UserSnapshot snapshot = new UserSnapshot();
        ChatDto brokenChat = chatDto(chatId, fromId, peerId);
        brokenChat.chatProperties.peer = null; // вызовет NullPointerException внутри insertSnapshotData
        snapshot.chats = new ChatDto[] { brokenChat };
        snapshot.messages = new MessageDto[]{};

        try {
            sut.applySnapshot(snapshot, sequence).get(2, TimeUnit.SECONDS);
            fail("ожидалось исключение из-за некорректных данных снапшота");
        } catch (ExecutionException | InterruptedException | TimeoutException expected) {
            // ожидаемо
        }

        // транзакция (db.runInTransaction) должна откатить выполненный clearAllTables(),
        // старое сообщение должно сохраниться, т.к. применение нового снапшота провалилось.
        assertFalse(
                "транзакция должна была откатиться, старые данные должны выжить",
                db.messagesDao().getByMessageIds(List.of(1)).isEmpty()
        );
    }
    // endregion

    // region helpers
    private String createChat(int fromId, int peerId) {
        String chatId = DbChatEntity.getChatId(fromId, peerId);

        DbChatEntity chat = new DbChatEntity();
        chat.id = chatId;
        chat.peerId = peerId;
        db.chatsDao().add(chat);

        return chatId;
    }

    private void seedMessage(int srvMessageId, String chatId, String text) {
        DbMessageEntity message = new DbMessageEntity();
        message.srvMessageId = srvMessageId;
        message.chatId = chatId;
        message.text = text;
        db.messagesDao().add(message);
    }

    private void createUser(int userId) {
        DbUserEntity user = new DbUserEntity();
        user.id = userId;
        user.nickname = "user-" + userId;
        db.usersDao().add(user);
    }

    private int getChatLastSrvMessageId(String chatId) {
        try (Cursor cursor = db.getOpenHelper().getReadableDatabase()
                .query("SELECT lastSrvMessageId FROM chats WHERE id = ?", new Object[] {chatId})) {
            assertTrue(String.format("чат с id=%s не найден", chatId), cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }

    private UserEvent messageEvent(long seq, int id, String chatId, int fromId, int peerId, String text) {
        MessageDto dto = messageDto(id, chatId, fromId, peerId, text);
        return rawEvent(seq, EventType.NEW_MESSAGE, toJson(dto));
    }

    private UserEvent editEvent(long seq, int messageId, String newText) {
        String json = String.format(
                "{\"messageId\":%d,\"newText\":\"%s\",\"editDate\":\"2026-01-01T00:00:00Z\"}",
                messageId, newText
        );
        return rawEvent(seq, EventType.MESSAGE_EDITED, json);
    }

    private UserEvent deleteEvent(long seq, int messageId) {
        return rawEvent(seq, EventType.MESSAGE_DELETED, String.format("{\"messageId\":%d}", messageId));
    }

    private UserEvent presenceEvent(long seq, String eventType, int userId, String at) {
        String json = String.format(
                "{\"userId\":%d,\"status\":null,\"at\":\"%s\"}",
                userId, at
        );
        return rawEvent(seq, eventType, json);
    }

    private UserEvent rawEvent(long seq, String eventType, String payloadJson) {
        UserEvent evt = new UserEvent();

        evt.sequence = seq;
        evt.userId = "1";
        evt.eventType = eventType;
        evt.payloadJson = payloadJson;

        return evt;
    }

    private ChatDto chatDto(String chatId, int userId, int peerId) {
        ChatDto chat = new ChatDto();
        chat.id = chatId;
        chat.members = new ArrayList<>(List.of(userId, peerId));
        chat.lastActivityAt = "2026-09-12T00:00:00Z";
        chat.messageIndex = 1;

        ChatPropertiesDto chatProperties = new ChatPropertiesDto();
        chatProperties.chatId = chatId;
        chatProperties.userId = userId;
        chatProperties.peerId = peerId;

        UserDto peer = new UserDto();
        peer.id = peerId;
        peer.nickname = "peer-" + peerId;
        peer.lastSeen = "2026-09-12T00:00:00Z";

        chatProperties.peer = peer;
        chat.chatProperties = chatProperties;

        return chat;
    }

    private MessageDto messageDto(int id, String chatId, int fromId, int peerId, String text) {
        MessageDto dto = new MessageDto();

        dto.id = id;
        dto.chatId = chatId;
        dto.fromId = fromId;
        dto.peerId = peerId;
        dto.text = text;
        dto.date = "2026-09-12T00:00:00Z";
        dto.deleted = false;
        dto.index = 1;

        return dto;
    }

    private String toJson(Object dto) {
        return new com.google.gson.Gson().toJson(dto);
    }
    // endregion
}