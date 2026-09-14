package com.petchat.messenger.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.exceptions.ApiClientException;
import com.petchat.api.exceptions.ApiServerException;
import com.petchat.api.exceptions.NotAuthorizedException;
import com.petchat.messenger.ClientApp;
import com.petchat.messenger.R;
import com.petchat.messenger.api.HubClientManager;
import com.petchat.messenger.database.PetChatDatabase;
import com.petchat.messenger.database.mappers.DbUserEntityMapper;
import com.petchat.messenger.databinding.ActivityInitializeBinding;
import com.petchat.messenger.security.securestorage.SecurePreferences;
import com.petchat.messenger.security.securestorage.SecureStorageException;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class InitializeActivity extends AppCompatActivity {
    public static Boolean startLoginActivity = false;

    @Inject
    PetChatApiClient apiClient;
    @Inject
    HubClientManager hubManager;
    @Inject
    PetChatDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        ActivityInitializeBinding binding = ActivityInitializeBinding.inflate(getLayoutInflater());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setContentView(binding.getRoot());

        init();
    }

    private void init() {
        hubManager.initialize();

        var context = getApplicationContext();
        String token = SecurePreferences.getStringValue(context, "ApiAccessToken", null),
                apiLogin = SecurePreferences.getStringValue(context, "ApiLogin", null),
                apiPwd = SecurePreferences.getStringValue(context, "ApiPwd", null);

        if (token == null || apiLogin == null || apiPwd == null) {
            startLoginActivity();
            return;
        }

        apiAuthorize(token).thenAccept(authStatus -> {
            switch (authStatus) {
                case SUCCESS -> startMainActivity();
                case NOT_AUTHORIZED -> {
                    refreshToken(context, apiLogin, apiPwd).thenAccept(refreshStatus -> {
                        switch (refreshStatus) {
                            case SUCCESS -> {
                                startMainActivity();
                                hubManager.reconnect();
                            }
                            case NOT_AUTHORIZED -> startLoginActivity();
                        }
                    });
                }
            }
        });
    }

    private void startLoginActivity() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
    private void startMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        ClientApp.postAuthorizationInitialize();
        hubManager.startHubConnection();
        finish();
    }

    private CompletableFuture<AuthStatus> apiAuthorize(String accessToken) {
        return apiClient.authorize(accessToken).handle((v, ex) -> {
            if (ex == null) return AuthStatus.SUCCESS;

            Throwable rootEx = ex.getCause() != null ? ex.getCause() : ex;
            Log.e("Init/auth", "(API) не удалось авторизоваться.", rootEx);

            return handleAuthException(rootEx);
        });
    }

    private CompletableFuture<AuthStatus> refreshToken(Context context, String login, String pwd) {
        return apiClient.authorize(login, pwd, true).handle((authResponse, ex) -> {
            if (ex == null) {
                try {
                    SecurePreferences.setValue(context, "ApiAccessToken", authResponse.accessToken);
                    database.usersDao().add(DbUserEntityMapper.fromApiUserDto(authResponse.user));
                    getSharedPreferences("global", MODE_PRIVATE).edit()
                            .putInt("SelfUserId", authResponse.user.id).apply();
                } catch (SecureStorageException e) {
                    throw new RuntimeException(e);
                }
                return AuthStatus.SUCCESS;
            }

            Throwable rootEx = ex.getCause() != null ? ex.getCause() : ex;
            Log.e("Init/auth", "(API) не обновить токен.", rootEx);

            return handleAuthException(rootEx);
        });
    }
    private AuthStatus handleAuthException(Throwable ex) {
        switch (ex) {
            case NotAuthorizedException _ -> {
                return AuthStatus.NOT_AUTHORIZED;
            }
            case ApiServerException apiServerException -> {
                runOnUiThread(() -> showErrorDialog(
                        String.format(Locale.getDefault(),
                                "Сервер сообщил об ошибке (code=%d): %s",
                                apiServerException.statusCode, apiServerException.content
                        )
                ));
                return AuthStatus.SERVER_ERROR;
            }
            case ApiClientException apiClientException -> {
                runOnUiThread(() -> showErrorDialog(String.format(Locale.getDefault(),
                        "Сервер не может обработать запрос (code=%d): %s",
                        apiClientException.statusCode, apiClientException.content
                )));
                return AuthStatus.CLIENT_ERROR;
            }
            default -> {
                runOnUiThread(() -> showErrorDialog("Произошла неизвестная ошибка: " + ex.getMessage()));
                return AuthStatus.UNKNOWN_ERROR;
            }
        }
    }

    private void showErrorDialog(String message) {
        new MaterialAlertDialogBuilder(this, R.style.MaterialThemeDialog)
                .setTitle("Auth")
                .setMessage(message)
                .setPositiveButton("Ок", null)
                .show();
    }

    private enum AuthStatus {
        SUCCESS,
        NOT_AUTHORIZED,
        SERVER_ERROR,
        CLIENT_ERROR,
        UNKNOWN_ERROR
    }
}
