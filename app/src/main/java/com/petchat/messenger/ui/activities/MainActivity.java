package com.petchat.messenger.ui.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.petchat.messenger.R;
import com.petchat.messenger.databinding.ActivityMainBinding;
import com.petchat.messenger.ui.category.chats.ChatsFragment;
import com.petchat.messenger.ui.category.chats.NewChatFragment;
import com.petchat.messenger.ui.category.contacts.ContactsFragment;
import com.petchat.messenger.ui.category.messages.MessagesFragment;
import com.petchat.messenger.ui.category.profile.ProfileFragment;
import com.petchat.messenger.ui.common.animations.EasingFunctions;
import com.petchat.messenger.ui.navigation.XNavigationManager;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    private static final String KEY_ACTIVE_ROOT_ID = "active_root_id";

    private ActivityMainBinding binding;
    private XNavigationManager navigationManager;

    public XNavigationManager getNavigationManager() {
        return navigationManager;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyWindowInsets();
        setupNavigation(savedInstanceState);
        setupBackPressedHandling();

        binding.fab.setOnClickListener(_ -> {
            navigationManager.addFragment(
                    new NewChatFragment(),
                    R.id.navNewChatFragment,
                    NewChatFragment.class.getSimpleName()
            );
        });

        handleNotificationIntent(getIntent());

        requestPermissions();
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (navigationManager != null) {
            outState.putInt(KEY_ACTIVE_ROOT_ID, navigationManager.getActiveRootId());
        }
    }
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    private void applyWindowInsets() {
        float extraMarginBottom = getResources().getDimension(R.dimen.dock_bar_margin_bottom);

        ViewCompat.setOnApplyWindowInsetsListener(binding.dockBarCard, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            var lp = (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) v.getLayoutParams();
            lp.bottomMargin = systemBars.bottom + (int) extraMarginBottom;
            v.setLayoutParams(lp);
            return insets;
        });
    }

    private void setupNavigation(Bundle savedInstanceState) {
        int rootId = savedInstanceState != null
                ? savedInstanceState.getInt(KEY_ACTIVE_ROOT_ID, R.id.navRootChats)
                : R.id.navRootChats;

        navigationManager = new XNavigationManager(
                getSupportFragmentManager(),
                R.id.navHostFragmentContainer,
                new NavigationListener()
        );

        navigationManager
                .addRootFragment(R.id.navRootChats, new ChatsFragment())
                .addRootFragment(R.id.navRootContacts, new ContactsFragment())
                .addRootFragment(R.id.navRootProfile, new ProfileFragment())
                .setActiveRootId(rootId)
                .setupWithBottomNavView(binding.dockBar);

        getSupportFragmentManager().executePendingTransactions();

        if (savedInstanceState == null) {
            binding.dockBar.setSelectedItemId(rootId);
        }
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void handleNotificationIntent(Intent intent) {
        String chatId = intent.getStringExtra("chatId");
        String peerIdStr = intent.getStringExtra("peerId");
        if (chatId == null || peerIdStr == null) return;

        int peerId = Integer.parseInt(peerIdStr);

        navigationManager.setActiveRootId(R.id.navRootChats);
        binding.dockBar.setSelectedItemId(R.id.navRootChats);
        Bundle args = new Bundle();
        args.putString("chatId", chatId);
        args.putInt("peerId", peerId);
        MessagesFragment fragment = new MessagesFragment();
        fragment.setArguments(args);

        navigationManager.addFragment(
                fragment,
                R.id.navMessagesFragment,
                MessagesFragment.getNavigationTag(peerId)
        );
    }

    public void hideDockBarAndFab() {
        binding.dockBarCard.animate()
                .translationY(150).setDuration(300)
                .setInterpolator(new EasingFunctions.EaseCubicOutInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        binding.dockBarCard.setVisibility(GONE);
                    }
                })
                .start();
        binding.fab.setVisibility(GONE);
    }
    public void showDockBarAndFab() {
        binding.dockBarCard.setVisibility(VISIBLE);
        binding.dockBarCard.animate()
                .translationY(0).setDuration(300)
                .setInterpolator(new EasingFunctions.EaseCubicOutInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        binding.fab.setVisibility(VISIBLE);
                    }
                })
                .start();
    }

    private void setupBackPressedHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!navigationManager.showPrevious()) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed(); // отдать системе
                }
            }
        });
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    //TODO !isGranted
            });

    private class NavigationListener implements XNavigationManager.XNavigationListener {
        @Override
        public void onRootSetActive(Integer fragmentId) {
            showDockBarAndFab();
        }
        @Override
        public void onRootSetUnactive(Integer fragmentId) {
            hideDockBarAndFab();
        }
    }
}