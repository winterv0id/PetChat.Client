package com.petchat.messenger.ui.category.chats;

import android.animation.*;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.petchat.messenger.R;
import com.petchat.messenger.database.entities.DbChatEntity;
import com.petchat.messenger.databinding.FragmentChatsBinding;
import com.petchat.messenger.ui.activities.MainActivity;
import com.petchat.messenger.ui.category.messages.MessagesFragment;
import com.petchat.messenger.ui.common.widgets.PullToActionLayout;
import com.petchat.messenger.ui.common.widgets.SwipeActionCallback;
import com.petchat.messenger.ui.navigation.XNavigationFragment;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChatsFragment extends XNavigationFragment {
    private enum ViewMode { ARCHIVE, DEFAULT }
    @Inject
    Vibrator vibrator;

    private PullToActionLayout pullToActionLayout;

    private ChatsAdapter chatsAdapter;
    private ChatsAdapter chatsInArchiveAdapter;
    private ChatsViewModel viewModel;
    private ViewMode viewMode = ViewMode.DEFAULT;

    private FragmentChatsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ChatsViewModel.class);

        chatsAdapter = new ChatsAdapter(behaviorListener);
        chatsInArchiveAdapter = new ChatsAdapter(behaviorListener);

        binding.chatsRecyclerView.setAdapter(chatsAdapter);
        binding.chatsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        int marginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        SwipeActionCallback swipeCallback = new SwipeActionCallback(
                requireContext(),
                R.drawable.ic_archive,
                marginPx,
                ContextCompat.getColor(requireContext(), R.color.accent_50),
                adapterPosition -> {
                    ChatsAdapter currentAdapter = currentAdapter();
                    DbChatEntity chat = currentAdapter.getCurrentList().get(adapterPosition);
                    viewModel.onArchiveToggle(chat);
                });
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.chatsRecyclerView);

        viewModel.chats.observe(getViewLifecycleOwner(), list -> chatsAdapter.submitList(list));
        viewModel.archivedChats.observe(getViewLifecycleOwner(), list -> chatsInArchiveAdapter.submitList(list));

        pullToActionLayout = new PullToActionLayout(
                binding.chatsRecyclerView,
                vibrator,
                45,
                new PullToActionLayout.Listener() {
                    @Override
                    public void onActionTriggered() {
                        toggleChatsViewMode();
                    }

                    @Override
                    public void onProgressChanged(float progress) {
                        binding.pullTooltipLayout.setScaleX(progress);
                        binding.pullTooltipLayout.setScaleY(progress);
                        binding.pullTooltipLayout.setAlpha(progress);

                        int translValue = (int) (progress * 45 / 0.7f);
                        int translValueDp = (int) TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, translValue, getResources().getDisplayMetrics());
                        binding.chatsRecyclerView.setTranslationY(translValueDp);
                        binding.pullTooltipLayout.setTranslationY(translValueDp);
                        binding.tooltipArrow.setRotation(-90 + progress * 45 / 0.25f);
                    }

                    @Override
                    public void onThresholdCrossed() { }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private ChatsAdapter currentAdapter() {
        return viewMode == ViewMode.DEFAULT ? chatsAdapter : chatsInArchiveAdapter;
    }

    private void toggleChatsViewMode() {
        if (viewMode == ViewMode.DEFAULT) {
            binding.chatsRecyclerView.setAdapter(chatsInArchiveAdapter);
            binding.tooltipTextView.setText(R.string.chats_pull_tooltip_archive);
            viewMode = ViewMode.ARCHIVE;
        } else {
            binding.chatsRecyclerView.setAdapter(chatsAdapter);
            binding.tooltipTextView.setText(R.string.chats_pull_tooltip_default);
            viewMode = ViewMode.DEFAULT;
        }
    }

    private final ChatsAdapter.OnChatBehaviorListener behaviorListener = new ChatsAdapter.OnChatBehaviorListener() {
        @Override
        public void onChatClick(DbChatEntity chat) {
            String fragmentTag = MessagesFragment.getNavigationTag(chat.peerId);
            if (getNavigationManager().existsFragment(fragmentTag)) {
                getNavigationManager().showFragment(fragmentTag);
            } else {
                MessagesFragment fragment = MessagesFragment.newInstanceForExistingChat(chat);
                getNavigationManager().addFragment(fragment, R.id.navMessagesFragment, fragmentTag);
            }
        }

        @Override
        public void onChatDelete(DbChatEntity chat) {
            new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialThemeDialog)
                    .setTitle("Внимание")
                    .setMessage(String.format(getString(R.string.confirmation_delete_chat), chat.chatName))
                    .setPositiveButton("Да, удалить", (_, _) -> viewModel.onDeleteChat(chat))
                    .setNegativeButton("Отмена", null)
                    .show();
        }
    };
}
