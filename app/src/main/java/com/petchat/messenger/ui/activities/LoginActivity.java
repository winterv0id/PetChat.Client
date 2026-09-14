package com.petchat.messenger.ui.activities;

import android.animation.*;
import android.content.*;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.*;
import android.graphics.drawable.shapes.RectShape;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.petchat.api.apiclient.PetChatApiClient;
import com.petchat.api.exceptions.ApiClientException;
import com.petchat.api.exceptions.ApiServerException;
import com.petchat.api.exceptions.NotAuthorizedException;
import com.petchat.api.objects.response.auth.AuthResponse;
import com.petchat.messenger.BuildConfig;
import com.petchat.messenger.ClientApp;
import com.petchat.messenger.R;
import com.petchat.messenger.api.HubClientManager;
import com.petchat.messenger.database.PetChatDatabase;
import com.petchat.messenger.database.mappers.DbUserEntityMapper;
import com.petchat.messenger.databinding.ActivityLoginBinding;
import com.petchat.messenger.security.securestorage.SecurePreferences;
import com.petchat.messenger.security.securestorage.SecureStorageException;
import com.petchat.messenger.ui.common.animations.EasingFunctions;
import com.petchat.messenger.utils.StringValidator;

import java.util.Locale;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {
    @Inject
    PetChatApiClient apiClient;
    @Inject
    PetChatDatabase database;
    @Inject
    HubClientManager hubManager;

    private ActivityLoginBinding binding;

    private boolean registryMode = false;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.versionTextView.setText(String.format(
                getResources().getString(R.string.login_version_text),
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        binding.continueButton.setOnClickListener(_ -> loginContinue());
        binding.switchToRegistrationButton.setOnClickListener(_ -> switchLoginMode());

        setContentView(binding.getRoot());
    }

    @Override protected void onStart() {
        super.onStart();
        onStartGradientAnimation();
    }

    @Override protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }

    private void switchLoginMode() {
        registryMode = !registryMode;

        if (registryMode) {
            binding.continueButton.setOnClickListener(_ -> registrationContinue());
            binding.switchToRegistrationButton.setText(R.string.login_switch_to_login_button_text);
            binding.viewFlipper.showNext();
        } else {
            binding.continueButton.setOnClickListener(_ -> loginContinue());
            binding.switchToRegistrationButton.setText(R.string.login_switch_to_registry_button_text);
            binding.viewFlipper.showPrevious();
        }
    }

    private void loginContinue() {
        hideKeyboard();

        String login = binding.loginForm.loginEditText.getText().toString().trim(),
                password = binding.loginForm.passwordEditText.getText().toString().trim();

        if (!checkCorrectLoginForm(login, password)) return;

        disableContinueButton();

        apiClient.authorize(login, password, false).thenAccept(authResponse -> {
            runOnUiThread(this::enableContinueButton);
            applyAuthResponse(authResponse, login, password);
            runOnUiThread(this::successfulAuthorize);
        }).exceptionally(ex -> {
            Throwable rootEx = ex.getCause() != null ? ex.getCause() : ex;
            Log.e("LoginActivity", "Не удалось авторизоваться!", rootEx);
            runOnUiThread(() ->  handleApiException(rootEx));
            return null;
        });
    }
    private void registrationContinue() {
        hideKeyboard();

        String nickname = binding.registryForm.nicknameEditText.getText().toString().trim(),
                login = binding.registryForm.loginEditText.getText().toString().trim(),
                password = binding.registryForm.passwordEditText.getText().toString().trim(),
                passwordRetry = binding.registryForm.passwordRetryEditText.getText().toString().trim();

        if (!checkCorrectRegistrationForm(login, password, passwordRetry)) return;

        disableContinueButton();

        apiClient.register(login, password, nickname).thenAccept((authResponse) -> {
            runOnUiThread(this::enableContinueButton);
            applyAuthResponse(authResponse, login, password);
            runOnUiThread(this::successfulAuthorize);
        }).exceptionally(ex -> {
            Throwable rootEx = ex.getCause() != null ? ex.getCause() : ex;
            Log.e("LoginActivity", "Не удалось зарегистрироваться!", rootEx);
            runOnUiThread(() ->  handleApiException(rootEx));
            return null;
        });
    }

    private void applyAuthResponse(AuthResponse response, String login, String password) {
        database.usersDao().add(DbUserEntityMapper.fromApiUserDto(response.user));
        getSharedPreferences("global", MODE_PRIVATE).edit()
                .putInt("SelfUserId", response.user.id).apply();

        try {
            SecurePreferences.setValue(this, "ApiAccessToken", response.accessToken);
            SecurePreferences.setValue(this, "ApiLogin", login);
            SecurePreferences.setValue(this, "ApiPwd", password);
        } catch (SecureStorageException e) {
            Log.wtf( "LoginActivity", "Err when trying save AccessToken to SecurePreferences!", e);
            throw new RuntimeException(e);
        }
    }

    private boolean checkCorrectLoginForm(String login, String password) {
        if (!StringValidator.create(login).notNullOrEmpty().isValid()) {
            onWrongAuthDataAnimation(binding.loginForm.loginEditText);
            return false;
        }
        if (!StringValidator.create(password).notNullOrEmpty().isValid()) {
            onWrongAuthDataAnimation(binding.loginForm.passwordEditText);
            return false;
        }
        return true;
    }
    private boolean checkCorrectRegistrationForm(String login, String password, String passwordRetry) {
        if (!StringValidator.create(login).notNullOrEmpty().isValid()) {
            onWrongAuthDataAnimation(binding.registryForm.loginEditText);
            return false;
        }

        if (!StringValidator.create(password).notNullOrEmpty().isPasswordSafe().isValid()) {
            onWrongAuthDataAnimation(binding.registryForm.passwordEditText);
            showErrorDialog(getString(R.string.password_not_safe));
            return false;
        }

        if (!Objects.equals(password, passwordRetry)) {
            onWrongAuthDataAnimation(binding.registryForm.passwordEditText);
            onWrongAuthDataAnimation(binding.registryForm.passwordRetryEditText);
            showErrorDialog(getString(R.string.passwords_not_equals));
            return false;
        }
        return true;
    }

    private void handleApiException(Throwable ex) {
        switch (ex) {
            case NotAuthorizedException notAuthorizedException -> {
                showErrorDialog("Не авторизован:\n" + notAuthorizedException.getMessage());
            }
            case ApiClientException apiClientException -> showErrorDialog(
                    String.format(Locale.getDefault(),
                            "Сервер не может обработать запрос (code=%d): %s",
                            apiClientException.statusCode, apiClientException.content)
            );
            case ApiServerException apiServerException -> showErrorDialog(
                    String.format(Locale.getDefault(),
                            "Сервер сообщил об ошибке (code=%d): %s",
                            apiServerException.statusCode, apiServerException.content
                    )
            );
            default -> {
                onWrongAuthDataAnimation(binding.registryForm.loginEditText);
                onWrongAuthDataAnimation(binding.registryForm.passwordEditText);
                showErrorDialog("Неизвестная ошибка:\n" + ex.getMessage());
            }
        }
    }

    private void successfulAuthorize() {
        getSharedPreferences("global", MODE_PRIVATE)
                .edit().putBoolean("IsLogout", false).apply();

        onFinishAnimation();
        startMainActivity();
    }

    private void startMainActivity() {
        ClientApp.postAuthorizationInitialize();
        hubManager.startHubConnection();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }, 600);
    }

    //region supportives func
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }
    private void showErrorDialog(String message) {
        new MaterialAlertDialogBuilder(this, R.style.MaterialThemeDialog)
                .setTitle("Auth")
                .setMessage(message)
                .setPositiveButton("Ок", null)
                .show();
    }

    private void disableContinueButton() {
        binding.continueButton.setEnabled(false);
        binding.continueButton.setAlpha(0.3f);
    }
    private void enableContinueButton() {
        binding.continueButton.setEnabled(true);
        binding.continueButton.setAlpha(1f);
    }
    //endregion

    //region animations
    private void onWrongAuthDataAnimation(EditText element) {
        VibratorManager vibratorManager = (VibratorManager)getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
        vibratorManager.getDefaultVibrator().vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK));

        // region Translation anim
        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator anim1 = ObjectAnimator.ofFloat(element, View.TRANSLATION_X, 0, 30);
        anim1.setDuration(100);

        ObjectAnimator anim2 = ObjectAnimator.ofFloat(element, View.TRANSLATION_X, 30, -20);
        anim2.setDuration(100);

        ObjectAnimator anim3 = ObjectAnimator.ofFloat(element, View.TRANSLATION_X, -20, 10);
        anim3.setDuration(100);

        ObjectAnimator anim4 = ObjectAnimator.ofFloat(element, View.TRANSLATION_X, 10, 0);
        anim4.setDuration(100);

        animatorSet.setTarget(element);
        animatorSet.playSequentially(anim1, anim2, anim3, anim4);
        animatorSet.start();
        // endregion

        // region border color animation
        GradientDrawable background = (GradientDrawable)element.getBackground().getCurrent().mutate();
        AnimatorSet animatorSet2 = new AnimatorSet();

        ValueAnimator anim_c1 = ValueAnimator.ofArgb(
                getColor(R.color.accent_60_colored),
                getColor(R.color.red_90)
        ).setDuration(400);
        anim_c1.setInterpolator(new EasingFunctions.EaseCubicOutInterpolator());
        anim_c1.addUpdateListener(animation -> background.setStroke(3, (int)animation.getAnimatedValue()));

        ValueAnimator anim_c2 = ValueAnimator.ofArgb(
                getColor(R.color.red_90),
                getColor(R.color.accent_60_colored)
        ).setDuration(400);
        anim_c2.setInterpolator(new EasingFunctions.EaseCubicOutInterpolator());
        anim_c2.addUpdateListener(animation -> background.setStroke(3, (int)animation.getAnimatedValue()));

        animatorSet2.playSequentially(anim_c1, anim_c2);
        animatorSet2.start();
        // endregion;
    }
    private void onStartGradientAnimation() {
        ValueAnimator anim = ValueAnimator.ofFloat(1.0f, 0.5f).setDuration(750);
        anim.setInterpolator(new EasingFunctions.EaseCubicOutInterpolator());
        anim.addUpdateListener(animation -> updateGradient(
                (float)animation.getAnimatedValue(), true, 0.5f));

        anim.start();
    }

    private void onFinishAnimation() {
        // gradient anim
        ValueAnimator gradientAnim = ValueAnimator.ofFloat(1.0f, 0.0f).setDuration(750);
        gradientAnim.setInterpolator(new EasingFunctions.EaseCubicOutInterpolator());
        gradientAnim.addUpdateListener(animation -> updateGradient(
                (float)animation.getAnimatedValue(), false, 0.3f));

        gradientAnim.start();
    }
    private void updateGradient(float center, boolean topOffset, float offset) {
        //cOffset default: 0.3f
        ShapeDrawable.ShaderFactory sf = new ShapeDrawable.ShaderFactory() {
            @Override
            public Shader resize(int width, int height) {
                return new LinearGradient(
                        0, 0, 0,
                        binding.mainLayout.getHeight(),
                        new int[] { 0xff5e4a5c, 0xff251a30 },
                        new float[]{
                                center - offset,
                                topOffset ? center + offset : center
                        },
                        Shader.TileMode.MIRROR
                );
            }
        };
        PaintDrawable p = new PaintDrawable();
        RectShape rectShape = new RectShape();
        p.setShape(rectShape);
        p.setShaderFactory(sf);

        binding.mainLayout.setBackground(p);
    }
    //endregion
}