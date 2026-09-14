package com.petchat.messenger.database.dao;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.petchat.messenger.database.PetChatDatabase;
import com.petchat.messenger.database.entities.DbMessageEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * {@link MessagesDao#getMinIndexOfWindowBefore} - вычисляет нижнюю границу
 * очередного окна истории при загрузке более старых сообщений.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MessagesDaoTest {
    private PetChatDatabase db;
    private MessagesDao dao;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, PetChatDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = db.messagesDao();
    }

    @After
    public void shutdown() {
        db.close();
    }

    @Test
    public void getMinIndexOfWindowBefore_ReturnsMinIndexOfLimitedWindow() {
        createMessagesWithIndexes("chat1", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        // окно из 3х сообщений с index [9,8,7] - возврат 7
        Integer min = dao.getMinIndexOfWindowBefore("chat1", 10, 3);

        assertEquals(Integer.valueOf(7), min);
    }

    @Test
    public void getMinIndexOfWindowBefore_BeforeIndexAboveAvailable_ReturnsMinOfAvailable() {
        createMessagesWithIndexes("chat1", 1, 2);

        Integer min = dao.getMinIndexOfWindowBefore("chat1", 10, 5);

        assertEquals(Integer.valueOf(1), min);
    }

    @Test
    public void getMinIndexOfWindowBefore_NothingBelowBeforeIndex_ReturnsNull() {
        createMessagesWithIndexes("chat1", 5, 6, 7);

        Integer min = dao.getMinIndexOfWindowBefore("chat1", 1, 5);

        assertNull(min);
    }

    @Test
    public void getMinIndexOfWindowBefore_IgnoresMessagesFromOtherChats() {
        createMessagesWithIndexes("chat1", 1, 2, 3);
        createMessagesWithIndexes("chat2", 100, 101, 102);

        Integer min = dao.getMinIndexOfWindowBefore("chat1", 10, 10);

        assertEquals(Integer.valueOf(1), min);
    }

    @Test
    public void getMinIndexOfWindowBefore_BeforeIndexIsExclusive() {
        // beforeIndex не должен попадать в окно
        createMessagesWithIndexes("chat1", 5, 10);

        Integer min = dao.getMinIndexOfWindowBefore("chat1", 10, 5);

        assertEquals(Integer.valueOf(5), min);
    }

    private int srvMessageIdCounter = 1;
    private void createMessagesWithIndexes(String chatId, int... indexes) {
        for (int index : indexes) {
            DbMessageEntity message = new DbMessageEntity();
            message.chatId = chatId;
            message.index = index;
            message.srvMessageId = srvMessageIdCounter++;
            dao.add(message);
        }
    }
}
