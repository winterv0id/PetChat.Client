package com.petchat.messenger.api;

import android.content.Context;
import android.util.Log;

import com.petchat.api.objects.events.ServerEvent;
import com.petchat.api.signalrclient.HubClient;
import com.petchat.api.signalrclient.external.EventStore;
import com.petchat.messenger.BuildConfig;
import com.petchat.messenger.security.securestorage.SecurePreferences;

import java.util.concurrent.*;

import dagger.hilt.android.qualifiers.ApplicationContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class HubClientManager {
    private final Context context;
    private final EventStore eventStore;
    private ExecutorService hubExecutor;

    private HubClient hubClient = null;
    private volatile boolean hubConnected = false;

    public HubClient getHubInstance() {
        if (hubClient == null) return create();
        else return hubClient;
    }

    @Inject
    public HubClientManager(@ApplicationContext Context context, EventStore eventStore) {
        this.context = context;
        this.eventStore = eventStore;
    }

    private HubClient create() {
        hubClient = new HubClient(
                BuildConfig.SERVER_API_ADDR,
                () -> SecurePreferences.getStringValue(context, "ApiAccessToken", ""),
                eventStore);
        return hubClient;
    }

    public void initialize() {
        hubExecutor = Executors.newSingleThreadExecutor();
        HubClient.ServerEventPublisher.subscribe(new ServerEventsSubscriber());
    }

    public void startHubConnection() {
        if (hubConnected) return;
        CompletableFuture.runAsync(() -> {
            try {
                Log.d("HubClientManager", "Connecting signalR hub...");
                getHubInstance().connect();
                hubConnected = true;
            } catch (ExecutionException | InterruptedException e) {
                Log.e("HubClientManager", "Connection error!", e);
            }
        }, hubExecutor);
    }

    public synchronized void reconnect() {
        disconnect();
        startHubConnection();
    }

    public void disconnect() {
        if (hubClient == null) return;

        Log.d("HubClientManager", "Disconnecting signalR hub...");
        hubClient.disconnect();
        hubClient = null;
    }

    private static class ServerEventsSubscriber implements Flow.Subscriber<ServerEvent> {
        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            this.subscription.request(1);
        }

        @Override
        public void onNext(ServerEvent serverEvent) {
            //....
            this.subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {}
        @Override
        public void onComplete() {}
    }
}
