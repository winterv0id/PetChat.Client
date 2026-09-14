package com.petchat.messenger.hilt.modules;

import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.messenger.BuildConfig;
import com.petchat.messenger.ClientApp;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import jakarta.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class ApiModule {
    @Provides
    @Singleton
    public static PetChatApiClient providePetChatApiClient() {
        return new PetChatApiClient(
                BuildConfig.SERVER_API_ADDR,
                BuildConfig.CLIENT_NAME,
                BuildConfig.CLIENT_KEY,
                ClientApp.getAndroidDeviceId()
        );
    }
}