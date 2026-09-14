package com.petchat.messenger.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.messenger.R;
import com.petchat.messenger.ui.activities.MainActivity;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import dagger.hilt.android.AndroidEntryPoint;
import jakarta.inject.Inject;

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
@AndroidEntryPoint
public class FcmMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "chat_messages";
    @Inject
    PetChatApiClient apiClient;

    @Override
    public void onRegistered(@NonNull String installationId) {
        apiClient.fcm().register().installationId(installationId).execute();
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String notificationType = message.getData().get("nType");
        if (notificationType == null) return;
        Log.d("FcmMessagingService", String.format("FCM notification received (notificationType=%s)", notificationType));

        //TODO sync (api endpoint)
        switch (notificationType) {
            case "chat_new_action": {
                String chatId = message.getData().get("chatId");
                String peerId = message.getData().get("peerId");
                try {
                    showNotification(
                            message.getData().get("title"),
                            message.getData().get("preview"),
                            message.getData().get("avatarUrl"),
                            chatId == null ? "" : chatId,
                            peerId
                    );
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void showNotification(String title, String body, String avatarUrl, String chatId, String peerId) throws ExecutionException, InterruptedException {
        int notificationId = chatId.hashCode();
        createNotificationChannelIfNeeded();

        // что запустится при нажатии на уведомление
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("peerId", peerId);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Bitmap avatarBitmap = null;
        try {
            if (avatarUrl != null) {
                avatarBitmap = Glide.with(this)
                        .asBitmap()
                        .load(avatarUrl)
                        .circleCrop()
                        .override(256, 256)
                        .submit()
                        .get();
            }
        } catch (Exception e) {
            e.fillInStackTrace();
        }

        IconCompat icon = null;
        if (avatarBitmap != null) {
            icon = IconCompat.createWithBitmap(avatarBitmap);
        }

        Person sender = new Person.Builder()
                .setName(title != null ? title : "")
                .setIcon(icon)
                .build();

        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(sender)
                .addMessage(body != null ? body : "", System.currentTimeMillis(), sender);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setStyle(messagingStyle)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(this).notify(notificationId, builder.build());
    }

    private void createNotificationChannelIfNeeded() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Сообщения чата",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Уведомления о новых сообщениях в чате");

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }
}
