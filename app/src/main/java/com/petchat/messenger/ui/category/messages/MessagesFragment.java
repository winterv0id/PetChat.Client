package com.petchat.messenger.ui.category.messages;

import static android.content.Context.MODE_PRIVATE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.petchat.messenger.R;
import com.petchat.messenger.database.entities.DbChatEntity;
import com.petchat.messenger.database.entities.DbMessageEntity;
import com.petchat.messenger.database.entities.DbUserEntity;
import com.petchat.messenger.databinding.FragmentMessagesBinding;
import com.petchat.messenger.ui.common.utils.PresenceFormatter;
import com.petchat.messenger.ui.navigation.XNavigationFragment;
import com.petchat.messenger.ui.navigation.XNavigationManager;
import com.petchat.messenger.utils.StringValidator;

import java.util.List;
import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MessagesFragment extends XNavigationFragment {
    private static final String BUNDLE_CHAT_ID = "chatId", BUNDLE_PEER_ID = "peerId";
    public static MessagesFragment newInstanceForExistingChat(DbChatEntity chat) {
        Log.d("MessagesFragment", String.format(
                "Creating fragment for existing chat (chat_name = %s; chatId = %s; peerId = %d)",
                chat.chatName, chat.id, chat.peerId));

        MessagesFragment fragment = new MessagesFragment();
        Bundle args = new Bundle();
        args.putString(BUNDLE_CHAT_ID, chat.id);
        args.putInt(BUNDLE_PEER_ID, chat.peerId);
        fragment.setArguments(args);
        return fragment;
    }

    public static MessagesFragment newInstanceForNewChat(int peerId) {
        Log.d("MessagesFragment", String.format(
                "Creating fragment for new (non-existing) chat with peerId = %d", peerId));

        MessagesFragment fragment = new MessagesFragment();
        Bundle args = new Bundle();
        args.putInt(BUNDLE_PEER_ID, peerId);
        fragment.setArguments(args);
        return fragment;
    }

    private FragmentMessagesBinding binding;
    private MessagesViewModel viewModel;
    private MessagesAdapter adapter;
    private LinearLayoutManager layoutManager;

    private DbUserEntity peer;
    private boolean peerTyping;
    private boolean isScreenActive = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessagesBinding.inflate(inflater, container, false);
        var root = binding.getRoot();

        // исправляет изменение размера фрагмента при открытии клавиатуры, без этого не работает
        root.setOnApplyWindowInsetsListener((_, windowInsets) -> {
            var imeHeight = windowInsets.getInsets(WindowInsets.Type.ime()).bottom;
            root.setPadding(0, 0, 0, imeHeight - 15);
            return windowInsets;
        });
        requireActivity().getWindow().setDecorFitsSystemWindows(false);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MessagesViewModel.class);

        Bundle args = getArguments();
        if (args == null) throw new RuntimeException("MessagesFragment arguments chatId & peerId required.");

        String chatId = args.getString(BUNDLE_CHAT_ID);
        int peerId = args.getInt(BUNDLE_PEER_ID);
        int selfUserId = requireActivity().getSharedPreferences("global", MODE_PRIVATE).getInt("SelfUserId", 0);
        viewModel.init(chatId, peerId, selfUserId);

        adapter = new MessagesAdapter(this::onMessageLongClick);
        layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setReverseLayout(true);
        binding.messagesRecyclerView.setLayoutManager(layoutManager);
        binding.messagesRecyclerView.setAdapter(adapter);

        binding.messagesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                markVisibleIncomingMessagesAsRead();
                int last = layoutManager.findLastVisibleItemPosition();
                int total = layoutManager.getItemCount();
                if (last != RecyclerView.NO_POSITION && last + 8 >= total) {
                    Log.d("MessagesFragment/UI", "Call loadOlderMessages(), totalItems = " + total);
                    viewModel.loadOlderMessages();
                }
            }
        });

        viewModel.messages.observe(getViewLifecycleOwner(), this::onMessagesChanged);

        viewModel.getPeer().observe(getViewLifecycleOwner(), user -> {
            Log.d("MessagesFragment/UI", "Peer user has been update.");

            peer = user;
            if (user != null) {
                binding.peerNameText.setText(user.nickname);
                Glide.with(this).load(user.imageUrl).centerCrop().into(binding.peerAvatar);
            }
            updateStatusText();
        });
        viewModel.getPeerTyping().observe(getViewLifecycleOwner(), typing -> {
            peerTyping = typing;
            updateStatusText();
        });

        binding.backButton.setOnClickListener(_ ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        viewModel.getEditingMessage().observe(getViewLifecycleOwner(), this::onEditingMessageChanged);

        binding.cancelEditButton.setOnClickListener(v -> viewModel.cancelEditing());

        binding.sendButton.setOnClickListener(v -> {
            String text = Objects.requireNonNull(binding.messageEditText.getText()).toString().trim();
            // запрет отправки пустой строки
            if (!StringValidator.create(text).notEmpty().isValid()) return;
            Log.d("MessagesFragment", "Sending message with text: " + text);

            if (viewModel.getEditingMessage().getValue() != null) {
                viewModel.editMessage(text);
            } else {
                viewModel.sendMessage(text);
            }
            binding.messageEditText.setText("");
        });

        binding.messageEditText.addTextChangedListener(new TypingWatcher());
    }

    private void onMessagesChanged(List<DbMessageEntity> messages) {
        if (!isScreenActive || binding == null) return;

        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        boolean wasAtBottom =  firstVisible <= 0; //-1 - NO_POSITION (нет элементов)

        adapter.submitList(messages, () -> {
            if (!messages.isEmpty() && wasAtBottom) {
                binding.messagesRecyclerView.scrollToPosition(0);
            }
            markVisibleIncomingMessagesAsRead();
        });
    }

    private void markVisibleIncomingMessagesAsRead() {
        if (!isScreenActive || binding == null) return;

        List<DbMessageEntity> currentList = adapter.getCurrentList();
        int size = currentList.size();
        if (size == 0) return;

        int first = layoutManager.findFirstVisibleItemPosition();
        int last = layoutManager.findLastVisibleItemPosition();
        if (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) return;

        first = Math.clamp(first, 0, size - 1);
        last = Math.clamp(last, 0, size - 1);
        if (first > last) return;

        List<DbMessageEntity> visible = currentList.subList(first, last + 1);
        viewModel.markAsRead(visible);
    }

    private void updateStatusText() {
        if (!isScreenActive || binding == null) return;

        if (peerTyping) {
            binding.userStatusText.setText(R.string.user_typing);
        } else if (peer != null) {
            binding.userStatusText.setText(PresenceFormatter.format(
                    requireContext(), peer.isOnline, peer.getLastSeenAsInstant()));
        }
    }

    private void onMessageLongClick(DbMessageEntity message, View anchorView) {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.sheet_message_actions, null);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheetView);

        boolean isOwn = message.fromOwner;
        View actionEdit = sheetView.findViewById(R.id.actionEdit);
        View actionDelete = sheetView.findViewById(R.id.actionDelete);
        actionEdit.setVisibility(isOwn ? View.VISIBLE : View.GONE);
        actionDelete.setVisibility(isOwn ? View.VISIBLE : View.GONE);

        sheetView.findViewById(R.id.actionCopy).setOnClickListener(v -> {
            copyToClipboard(message.text);
            dialog.dismiss();
        });
        actionEdit.setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.startEditing(message);
        });
        actionDelete.setOnClickListener(v -> {
            dialog.dismiss();
            confirmDelete(message);
        });

        dialog.show();
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = ContextCompat.getSystemService(requireContext(), ClipboardManager.class);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("message", text));
        }
    }

    private void confirmDelete(DbMessageEntity message) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.MaterialThemeDialog)
                .setTitle("Удалить сообщение")
                .setMessage(String.format(getString(R.string.confirmation_delete_message), message.text))
                .setPositiveButton("Да, удалить", (_, i) -> viewModel.deleteMessage(message))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void onEditingMessageChanged(DbMessageEntity editing) {
        if (editing != null) {
            binding.editBar.setVisibility(View.VISIBLE);
            binding.messageEditText.setText(editing.text);
            binding.messageEditText.setSelection(Objects.requireNonNull(binding.messageEditText.getText()).length());
            binding.messageEditText.requestFocus();
        } else {
            binding.editBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActive() {
        super.onActive();
        Log.d("MessagesFragment", "Fragment is active");
        isScreenActive = true;
        markVisibleIncomingMessagesAsRead();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void destroyWay(XNavigationManager navigationManager, FragmentTransaction transaction, boolean forwardDirection) {
        super.destroyWay(navigationManager, transaction, forwardDirection);
        Log.d("MessagesFragment", "Fragment is unactive");
        isScreenActive = false;
    }

    public static String getNavigationTag(int peerId) {
        return MessagesFragment.class.getSimpleName() + "_" + peerId;
    }

    private class TypingWatcher implements TextWatcher {
        private final Handler typingHandler = new Handler(Looper.getMainLooper());
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            viewModel.typing();
            typingHandler.removeCallbacksAndMessages(null);
        }
        @Override
        public void afterTextChanged(Editable s) {
            typingHandler.postDelayed(viewModel::stopTyping, 500);
        }
    }
}