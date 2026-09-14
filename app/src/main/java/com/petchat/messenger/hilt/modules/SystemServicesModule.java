package com.petchat.messenger.hilt.modules;

import android.content.Context;
import android.os.Vibrator;
import android.os.VibratorManager;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import jakarta.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class SystemServicesModule {
    @Provides
    @Singleton
    public static Vibrator provideVibrator(@ApplicationContext Context context) {
        VibratorManager vibratorManager = (VibratorManager)context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
        return vibratorManager.getDefaultVibrator();
    }
}
