package com.petchat.messenger.ui.category.messages;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.*;

import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.objects.events.EventType;
import com.petchat.api.objects.events.UserEvent;
import com.petchat.api.signalrclient.HubClient;
import com.petchat.messenger.api.HubClientManager;
import com.petchat.messenger.database.PetChatDatabase;
import com.petchat.messenger.database.entities.DbChatEntity;
import com.petchat.messenger.database.entities.DbMessageEntity;
import com.petchat.messenger.database.entities.DbUserEntity;
import com.petchat.messenger.database.mappers.DbMessageEntityMapper;
import com.petchat.messenger.services.UserResolver;
import com.petchat.messenger.utils.StringValidator;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MessagesViewModel extends ViewModel {
    private static final int PAGE_SIZE = 50;

    private boolean initialized = false;
    private final HubClientManager hubManager;
    private final PetChatApiClient apiClient;
    private final PetChatDatabase database;
    private final UserResolver userResolver;

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());


    public final LiveData<List<DbMessageEntity>> messages;
    private final MutableLiveData<String> chatId = new MutableLiveData<>();
    private final MutableLiveData<ChatWindow> window = new MutableLiveData<>();

    private final MediatorLiveData<DbUserEntity> peer = new MediatorLiveData<>();
    public LiveData<DbUserEntity> getPeer() {
        return peer;
    }

    private final MutableLiveData<Boolean> peerTyping = new MutableLiveData<>(false);
    public LiveData<Boolean> getPeerTyping() { return peerTyping; }

    private final MutableLiveData<DbMessageEntity> editingMessage = new MutableLiveData<>(null);
    public LiveData<DbMessageEntity> getEditingMessage() { return editingMessage; }

    private Flow.Subscription eventsSubscription;
    private int selfUserId, peerId;

    private volatile boolean messagesLoading = false, isFetchingFromServer = false;
    private volatile boolean hasMore = true;
    private volatile int oldestMessageIndex = Integer.MAX_VALUE;
    private volatile Integer lastSentReadUpToMessageId = null;

    @Inject
    public MessagesViewModel(HubClientManager hubManager, PetChatApiClient apiClient, PetChatDatabase database, UserResolver userResolver) {
        this.hubManager = hubManager;
        this.apiClient = apiClient;
        this.database = database;
        this.userResolver = userResolver;

        messages = Transformations.switchMap(window, w -> {
            Log.d("MessagesFragment/VM", String.format(
                    "ChatWindow is changed: chatId = %s; minIndex = %d;", w.chatId, w.minIndex));

            if (w.chatId == null) {
                MutableLiveData<List<DbMessageEntity>> empty = new MutableLiveData<>();
                empty.setValue(Collections.emptyList());
                return empty;
            }
            return database.messagesDao().observeMessagesFrom(w.chatId, w.minIndex);
        });
    }

    // вызывается фрагментом в onViewCreated
    public void init(@Nullable String chatId, int peerId, int selfUserId) {
        if (initialized) return;
        initialized = true;

        this.peerId = peerId;
        this.selfUserId = selfUserId;
        this.chatId.setValue(chatId);

        if (chatId != null) {
            dbExecutor.execute(() -> loadMessagesWindow(chatId, Integer.MAX_VALUE));
        }

        userResolver.observeUser(peerId).thenAcceptAsync(peerUser -> {
            mainHandler.post(() -> peer.addSource(peerUser, peer::setValue));
        });

        subscribeToPeerEvents(peerId);
    }

    private void subscribeToPeerEvents(int peerId) {
        HubClient.UserEventPublisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                eventsSubscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(UserEvent event) {
                switch (event.eventType) {
                    case EventType.USER_TYPING -> {
                        var payload = event.asUserTypingPayload();
                        if (payload.userId != peerId) return;
                        peerTyping.postValue(true);
                    }
                    case EventType.USER_STOPPED_TYPING -> {
                        var payload = event.asUserTypingPayload();
                        if (payload.userId != peerId) return;
                        peerTyping.postValue(false);
                    }
                }
                eventsSubscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e("MessagesFragment/VM", "Error while handle peer event!", throwable);
            }
            @Override
            public void onComplete() {}
        });
    }

    public void markAsRead(List<DbMessageEntity> visibleMessages) {
        int maxIndex = 0;
        Integer maxMessageId = null;

        for (DbMessageEntity m : visibleMessages) {
            if (!m.fromOwner && !m.readed && m.srvMessageId != null && m.index > maxIndex) {
                maxIndex = m.index;
                maxMessageId = m.srvMessageId;
            }
        }

        if (maxMessageId == null) return;

        Integer previous = lastSentReadUpToMessageId;
        if (previous != null && maxMessageId <= previous) return; // уже помечались прочитанными

        lastSentReadUpToMessageId = maxMessageId;
        int finalMaxMessageId = maxMessageId;

        String currentChatId = chatId.getValue();
        if (currentChatId == null) return;

        Log.d("MessagesFragment/VM", "Mark read up to messageId: " + finalMaxMessageId);

        dbExecutor.execute(() -> database.messagesDao().markReadUpTo(
                currentChatId, false, finalMaxMessageId, Instant.now().toString()));

        hubManager.getHubInstance().markMessagesReadUpTo(finalMaxMessageId);
    }

    public void sendMessage(String text) {
        String trimmed = text.trim();
        if (!StringValidator.create(trimmed).notEmpty().isValid()) return;
        Log.d("MessagesFragment/VM", "Sending message with text: " + trimmed);

        CompletableFuture.supplyAsync(() -> {
            String currentChatId = chatId.getValue();
            if (currentChatId == null) {
                Log.d("MessagesFragment/VM", "Creating local chat for peerId = " + peerId);
                currentChatId = createLocalChat();
                chatId.postValue(currentChatId);
                oldestMessageIndex = 0;
                hasMore = false;
                window.postValue(new ChatWindow(currentChatId, 0));
            }

            int localId = (int)insertUnconfirmedMessage(currentChatId, peerId, trimmed);
            return new AbstractMap.SimpleEntry<>(currentChatId, localId);

        }, dbExecutor).thenCompose(entry -> {
            Log.d("MessagesFragment/VM", String.format(
                    "Send message to server.. (chatId = %s; localMessageId = %d)",
                    entry.getKey(), entry.getValue()));

            return hubManager.getHubInstance()
                    .sendMessage(peerId, trimmed)
                    .thenAccept(result -> {
                        Log.d("MessagesFragment/VM", String.format(
                                "Message (localId = %d) accepted! serverId = %d; index = %d, date = %s",
                                entry.getValue(), result.messageId, result.messageIndex, result.messageDate));

                        database.messagesDao().confirmSendedMessage(entry.getValue(), result.messageId, result.messageIndex);
                        database.chatsDao().updateMessagesIndex(result.chatId, result.messageIndex);
                        database.chatsDao().updateLastMessageId(result.chatId, result.messageId, result.messageDate);
            });
        }).exceptionally(ex -> {
            Log.e("MessagesFragment/VM", "Error whlie sending message!", ex);
            return null;
        });
    }

    public void editMessage(String newText) {
        DbMessageEntity message = editingMessage.getValue();
        String trimmed = newText.trim();

        if (message == null || !StringValidator.create(trimmed).notEmpty().isValid()) return;
        if (message.srvMessageId == null && !message.markAsSendError) {
            // TODO: оповестить через ui - нельзя изменить пока отправляется
            return;
        }

        editingMessage.postValue(null);

        hubManager.getHubInstance().editMessage(message.srvMessageId, trimmed).thenAccept(success -> {
            if (!success) {
                Log.e("MessagesFragment/VM", String.format(
                        "Server response unsuccessful editing message! (messageId = %d, newText = %s)",
                        message.srvMessageId, newText));

                //TODO оповестить через ui
                return;
            }
            Log.d("MessagesFragment/VM",  String.format(
                    "Server response successful editing message. (messageId = %d, newText = %s)",
                    message.srvMessageId, newText));

            dbExecutor.execute(() -> {
                String editDate = java.time.Instant.now().toString();
                database.messagesDao().editMessage(message.srvMessageId, trimmed, editDate);
            });
        }).exceptionally(ex -> {
            Log.e("MessagesFragment/VM", String.format(
                    "(API) Не удалось редактировать сообщение! (messageId = %d, newText = %s)",
                    message.srvMessageId, newText), ex);
            return null;
        });
    }

    public void deleteMessage(DbMessageEntity message) {
        if (message.srvMessageId == null && !message.markAsSendError) {
            // TODO: оповестить через ui - нельзя изменить пока отправляется
            return;
        }

        hubManager.getHubInstance().deleteMessage(message.srvMessageId).thenAccept(success -> {
            if (!success) {
                Log.e("MessagesFragment/VM", String.format(
                        "Server response unsuccessful deleting message! (messageId = %d)", message.srvMessageId));

                //TODO оповестить через ui
                return;
            }
            Log.d("MessagesFragment/VM",  String.format(
                    "Server response successful deleting message. (messageId = %d)", message.srvMessageId));

            dbExecutor.execute(() -> {
                database.messagesDao().delete(message.srvMessageId);
                var newLastMessage = database.messagesDao().getLastChatMessage(message.chatId);
                database.chatsDao().updateLastMessageId(message.chatId, newLastMessage.srvMessageId, newLastMessage.date);
            });
        }).exceptionally(ex -> {
            Log.e("MessagesFragment/VM", String.format(
                    "(API) Не удалось удалить сообщение! (messageId = %d)", message.srvMessageId), ex);
            return null;
        });
    }

    public void typing() {
        Log.d("MessagesFragment/VM", "Send typing to hub...");
        hubManager.getHubInstance().typing(peerId);
    }
    public void stopTyping() {
        Log.d("MessagesFragment/VM", "Send stopTyping to hub...");
        hubManager.getHubInstance().stopTyping(peerId);
    }

    public void startEditing(DbMessageEntity message) {
        if (!message.fromOwner) return;
        editingMessage.setValue(message);
    }
    public void cancelEditing() {
        editingMessage.setValue(null);
    }

    private String createLocalChat() {
        var _peer = peer.getValue();
        if (_peer == null) return null;

        DbChatEntity newChat = new DbChatEntity();
        newChat.id = DbChatEntity.getChatId(selfUserId, _peer.id);
        newChat.members = new ArrayList<>(List.of(selfUserId, _peer.id));
        newChat.imageUrl = _peer.imageUrl;
        newChat.chatName = _peer.nickname;
        newChat.peerId = _peer.id;

        database.chatsDao().add(newChat);

        Log.d("MessagesFragment/VM", String.format(
                "Created local chat for peer id = %d, chatId = %s.", _peer.id, newChat.id));

        return newChat.id;
    }

    private long insertUnconfirmedMessage(String chatId, Integer peerId, String text) {
        DbMessageEntity local = new DbMessageEntity();
        local.chatId = chatId;
        local.peerId = peerId;
        local.text = text;
        local.fromOwner = true;

        Log.d("MessagesFragment/VM", "Created unconfirmed message:\n" + local);
        return database.messagesDao().add(local);
    }

    public void loadOlderMessages() {
        String id = chatId.getValue();
        if (id == null || !hasMore || messagesLoading) return;
        messagesLoading = true;

        dbExecutor.execute(() -> {
            try {
                loadMessagesWindow(id, oldestMessageIndex);
            } finally {
                messagesLoading = false;
            }
        });
    }

    private void loadMessagesWindow(String chatId, int beforeIndex) {
        Integer pageMin = database.messagesDao().getMinIndexOfWindowBefore(chatId, beforeIndex, PAGE_SIZE);
        if (pageMin == null) {
            Log.d("MessagesFragment/VM", String.format("Chat (id = %s) is full loaded!", chatId));
            fetchOlderMessagesFromServer(chatId, beforeIndex);
            return;
        }

        hasMore = pageMin > 0;
        oldestMessageIndex = pageMin;
        window.postValue(new ChatWindow(chatId, pageMin));

        Log.d("MessagesFragment/VM", String.format(
                "Chat window is loaded - page min index is %d before %d; hasMore: %b",
                pageMin, beforeIndex, hasMore));
    }

    private void fetchOlderMessagesFromServer(String chatId, int beforeIndex) {
        if (isFetchingFromServer) return;
        isFetchingFromServer = true;

        Log.d("MessagesFragment/VM", String.format(
                "Fetching older messages from server... (chatId = %s; beforeIndex = %d)",
                chatId, beforeIndex));

        apiClient.chats().getHistory()
                .chatId(chatId)
                .before(beforeIndex)
                .limit(PAGE_SIZE)
                .execute().thenAccept(response -> {
                    isFetchingFromServer = false;

                    Log.d("MessagesFragment/VM", String.format(
                            "Fetched older messages (chatId = %s;) from server :: length = %d, hasMore = %b)",
                            chatId, response.length, response.hasMore));

                    hasMore = response.hasMore;
                    if (response.length == 0) {
                        // инициализация window если вообще нет сообщений
                        if (window.getValue() == null) {
                            oldestMessageIndex = 0;
                            window.postValue(new ChatWindow(chatId, 0));
                        }
                        return;
                    }

                    dbExecutor.execute(() -> {
                        database.messagesDao().insertAll(DbMessageEntityMapper.fromApiMessagesDto(response.messages));
                        loadMessagesWindow(chatId, beforeIndex);
                    });
                }).exceptionally(ex -> {
                    Log.e("MessagesFragment/VM", String.format(
                            "(API) Не удалось получить историю сообщений! (chatId = %s; beforeIndex = %d)",
                            chatId, beforeIndex), ex);
                    return null;
                });
    }

    @Override
    protected void onCleared() {
        Log.d("MessagesFragment/VM", "onCleared() invoked.");
        super.onCleared();
        if (eventsSubscription != null) {
            eventsSubscription.cancel();
        }
        dbExecutor.shutdown();
    }

    private record ChatWindow(String chatId, int minIndex) { }
}