package com.petchat.messenger.services;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

public final class FcmTokenManager {
    public static void registerWithFcm() {
        FirebaseMessaging.getInstance().register()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Не удалось зарегистрироваться в FCM", task.getException());
                    }
                });
    }
}