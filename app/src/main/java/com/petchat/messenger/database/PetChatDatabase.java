package com.petchat.messenger.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.petchat.messenger.database.dao.*;
import com.petchat.messenger.database.entities.*;

@Database(
        entities = {
                DbMessageEntity.class,
                DbAppliedStateEntity.class,
                DbChatEntity.class,
                DbUserEntity.class
        },
        version = 1
)
public abstract class PetChatDatabase extends RoomDatabase {
    public abstract MessagesDao messagesDao();
    public abstract ChatsDao chatsDao();
    public abstract UsersDao usersDao();
    public abstract AppliedStateDao appliedStateDao();
}