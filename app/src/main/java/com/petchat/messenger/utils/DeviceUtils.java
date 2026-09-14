package com.petchat.messenger.utils;

import android.content.Context;
import android.provider.Settings;

public final class DeviceUtils {
    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
    }
}