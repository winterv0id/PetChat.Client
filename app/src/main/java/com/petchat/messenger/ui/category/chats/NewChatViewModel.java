package com.petchat.messenger.ui.category.chats;

import androidx.lifecycle.*;

import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.objects.serverdto.UserDto;
import com.petchat.messenger.database.PetChatDatabase;
import com.petchat.messenger.database.mappers.DbUserEntityMapper;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class NewChatViewModel extends ViewModel {
    private static final int SEARCH_DEBOUNCE_MS = 350;
    private static final int MIN_QUERY_LENGTH = 5;

    private final PetChatApiClient apiClient;
    private final PetChatDatabase database;

    @Inject
    public NewChatViewModel(PetChatApiClient apiClient, PetChatDatabase database) {
        this.apiClient = apiClient;
        this.database = database;
    }

    private final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingSearch;

    private final MutableLiveData<List<UserDto>> results = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    public LiveData<List<UserDto>> getResults() {
        return results;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void onQueryChanged(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();

        if (pendingSearch != null) {
            pendingSearch.cancel(false);
        }

        if (query.length() < MIN_QUERY_LENGTH) {
            results.postValue(Collections.emptyList());
            isLoading.postValue(false);
            errorMessage.postValue(null);
            return;
        }

        isLoading.postValue(true);
        pendingSearch = debounceExecutor.schedule(() ->
                performSearch(query), SEARCH_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<Void> saveUser(UserDto user) {
        return CompletableFuture.runAsync(() ->
                database.usersDao().add(DbUserEntityMapper.fromApiUserDto(user)));
    }

    private void performSearch(String query) {
        apiClient.users().search().query(query).execute().exceptionally(ex -> {
            errorMessage.postValue(ex.getMessage());
            return null;
        }).thenAccept(response -> {
            if (response == null) return;
            results.postValue(response.users);
            errorMessage.postValue(null);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        debounceExecutor.shutdownNow();
    }
}
