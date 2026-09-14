package com.petchat.messenger;

import android.app.Application;
import android.content.SharedPreferences;

import com.petchat.messenger.services.FcmTokenManager;
import com.petchat.messenger.ui.activities.InitializeActivity;
import com.petchat.messenger.utils.DeviceUtils;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public final class ClientApp extends Application {
    private static String deviceId;
    public static String getAndroidDeviceId() { return deviceId; }

    @Override
    public void onCreate() {
        super.onCreate();
        initialize();
    }
    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    private void initialize() {
        SharedPreferences global = getSharedPreferences("global", MODE_PRIVATE);
        deviceId = DeviceUtils.getDeviceId(this);

        if (global.getBoolean("FirstAppStart", true)) {
            global.edit().putBoolean("FirstAppStart", false).apply();
            InitializeActivity.startLoginActivity = true;
            return;
        }

        if (global.getBoolean("IsLogout", true))
        {
            InitializeActivity.startLoginActivity = true;
            return;
        }

        postAuthorizationInitialize();
    }

    public static void postAuthorizationInitialize() {
        FcmTokenManager.registerWithFcm();
    }
}