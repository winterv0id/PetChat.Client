package com.petchat.messenger.services;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.objects.response.users.GetResult;
import com.petchat.messenger.database.PetChatDatabase;
import com.petchat.messenger.database.entities.DbUserEntity;
import com.petchat.messenger.database.mappers.DbUserEntityMapper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import jakarta.inject.Singleton;

@Singleton
public class UserResolver {
    @Inject
    public UserResolver(PetChatDatabase database, PetChatApiClient apiClient) {
        this.database = database;
        this.apiClient = apiClient;
    }

    private final PetChatDatabase database;
    private final PetChatApiClient apiClient;

    public CompletableFuture<DbUserEntity> getUser(int userId) {
        if (database.usersDao().exists(userId)) {
            return CompletableFuture.supplyAsync(() -> database.usersDao().getUser(userId));
        }

        return apiClient.users().get().userId(userId).execute().exceptionally(ex -> {
            Log.e("UserResolver", String.format("(API) Не удалось получить пользователя (id=%d)!", userId), ex);
            return null;
        }).thenApply(result -> {
            if (result == null) return null;

            var user = DbUserEntityMapper.fromApiUserDto(result.user);
            database.usersDao().add(user);
            return user;
        });
    }

    public CompletableFuture<LiveData<DbUserEntity>> observeUser(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            if (database.usersDao().exists(userId)) {
                return database.usersDao().observeUser(userId);
            }

            GetResult result;
            try {
                result = apiClient.users().get().userId(userId).execute().exceptionally(ex -> {
                    Log.e("UserResolver", String.format("(API) Не удалось получить пользователя (id=%d)!", userId), ex);
                    return null;
                }).get();
            } catch (ExecutionException | InterruptedException e) {
                throw new CompletionException(e);
            }

            if (result == null) return null;

            database.usersDao().add(DbUserEntityMapper.fromApiUserDto(result.user));
            return database.usersDao().observeUser(userId);
        });
    }
}
