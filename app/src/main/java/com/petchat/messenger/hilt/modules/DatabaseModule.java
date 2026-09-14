package com.petchat.messenger.hilt.modules;

import android.content.Context;

import androidx.room.Room;

import com.petchat.api.signalrclient.external.EventStore;
import com.petchat.messenger.database.PetChatDatabase;
import com.petchat.messenger.database.management.UserEventStoreManager;

import dagger.*; // annotations
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import jakarta.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class DatabaseModule {
    @Provides
    @Singleton
    public static PetChatDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, PetChatDatabase.class, "petchat.db")
                .fallbackToDestructiveMigration(true) //..
                .build();
    }

    @Binds
    @Singleton
    public abstract EventStore bindEventStore(UserEventStoreManager impl);
}
