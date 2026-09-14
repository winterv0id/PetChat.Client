package com.petchat.messenger.ui.category.chats;

import androidx.annotation.NonNull;
import androidx.lifecycle.*;

import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.messenger.database.PetChatDatabase;
import com.petchat.messenger.database.entities.DbChatEntity;
import com.petchat.messenger.database.entities.DbMessageEntity;
import com.petchat.messenger.database.entities.DbUserEntity;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ChatsViewModel extends ViewModel {
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final PetChatDatabase database;
    private final PetChatApiClient apiClient;

    public final LiveData<List<DbChatEntity>> chats;
    public final LiveData<List<DbChatEntity>> archivedChats;

    @Inject
    public ChatsViewModel(PetChatDatabase database, PetChatApiClient apiClient) {
        this.database = database;
        this.apiClient = apiClient;

        this.chats = fillChatsFields(database.chatsDao().observeRecentChats());
        this.archivedChats = fillChatsFields(database.chatsDao().observeRecentArchivedChats());
    }

    //заполняет chat.lastMessage, chat.unreadCount
    private LiveData<List<DbChatEntity>> fillChatsFields(LiveData<List<DbChatEntity>> source) {
        MediatorLiveData<List<DbChatEntity>> result = new MediatorLiveData<>();

        List<DbChatEntity>[] latestChats = new List[]{null};
        Map<String, Integer>[] latestCounts = new Map[]{null};

        Runnable recompute = () -> {
            List<DbChatEntity> chatList = latestChats[0];
            if (chatList == null) return;
            Map<String, Integer> counts = latestCounts[0];

            dbExecutor.execute(() -> {
                List<DbChatEntity> enriched = new ArrayList<>(chatList.size());
                for (DbChatEntity original : chatList) enriched.add(DbChatEntity.copy(original));

                List<Integer> messageIds = new ArrayList<>(), userIds = new ArrayList<>();
                for (DbChatEntity chat : enriched) {
                    if (chat.lastSrvMessageId != null) messageIds.add(chat.lastSrvMessageId);
                    userIds.add(chat.peerId);
                }
                if (!messageIds.isEmpty()) {
                    List<DbMessageEntity> messages = database.messagesDao().getByMessageIds(messageIds);
                    Map<Integer, DbMessageEntity> byId = new HashMap<>();
                    for (DbMessageEntity m : messages) byId.put(m.srvMessageId, m);
                    for (DbChatEntity chat : enriched) {
                        chat.lastMessage = chat.lastSrvMessageId != null ? byId.get(chat.lastSrvMessageId) : null;
                    }
                }

                List<DbUserEntity> users = database.usersDao().getUsers(userIds);
                Map<Integer, DbUserEntity> usersById = new HashMap<>();
                for (DbUserEntity u : users) usersById.put(u.id, u);
                for (DbChatEntity chat : enriched) {
                    if (chat.chatName == null || chat.chatName.isEmpty())
                        chat.chatName = Objects.requireNonNull(usersById.get(chat.peerId)).nickname;
                    if (chat.imageUrl == null || chat.imageUrl.isEmpty())
                        chat.imageUrl = Objects.requireNonNull(usersById.get(chat.peerId)).imageUrl;
                }

                for (DbChatEntity chat : enriched) {
                    Integer count = counts != null ? counts.get(chat.id) : null;
                    chat.unreadCount = count != null ? count : 0;
                }

                result.postValue(enriched);
            });
        };

        result.addSource(source, chatList -> {
            latestChats[0] = chatList;
            recompute.run();
        });
        result.addSource(database.chatsDao().observeUnreadCounts(), counts -> {
            latestCounts[0] = counts;
            recompute.run();
        });

        return result;
    }

    public void onArchiveToggle(@NonNull DbChatEntity chat) {
        boolean archive = !chat.archived;
        apiClient.chats().updateProperties().chatId(chat.id).isArchived(archive).execute();
        dbExecutor.execute(() -> database.chatsDao().setArchived(chat.id, archive));
    }

    public void onDeleteChat(@NonNull DbChatEntity chat) {
        // TODO api request
        dbExecutor.execute(() -> database.chatsDao().delete(chat.id));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        dbExecutor.shutdown();
    }
}
