package com.petchat.messenger.ui.category.chats;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.petchat.api.objects.serverdto.UserDto;
import com.petchat.messenger.R;
import com.petchat.messenger.databinding.FragmentNewChatBinding;
import com.petchat.messenger.ui.activities.MainActivity;
import com.petchat.messenger.ui.category.messages.MessagesFragment;
import com.petchat.messenger.ui.navigation.XNavigationFragment;
import com.petchat.messenger.ui.navigation.XNavigationManager;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NewChatFragment extends XNavigationFragment {
    private FragmentNewChatBinding binding;
    private NewChatViewModel viewModel;
    private UsersSearchAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNewChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(NewChatViewModel.class);

        adapter = new UsersSearchAdapter(this::openChat);
        binding.usersRecyclerView.setAdapter(adapter);
        binding.usersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.backButton.setOnClickListener(_ ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        binding.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.onQueryChanged(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        viewModel.getResults().observe(getViewLifecycleOwner(), this::onResultsChanged);
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading ->
                binding.loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE));
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                binding.emptyStateText.setText(error);
                binding.emptyStateText.setVisibility(View.VISIBLE);
            }
        });

        binding.searchEditText.requestFocus();
        showKeyboard();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void onResultsChanged(List<UserDto> results) {
        adapter.submitList(results);
        binding.emptyStateText.setVisibility(results.isEmpty() ? View.VISIBLE : View.GONE);
        if (results.isEmpty()) {
            binding.emptyStateText.setText(getString(R.string.text_no_content));
        }
    }

    private void openChat(UserDto user) {
        viewModel.saveUser(user).thenAccept(_ -> {
            String fragmentTag = MessagesFragment.getNavigationTag(user.id);
            if (getNavigationManager().existsFragment(fragmentTag)) {
                getNavigationManager().showFragment(fragmentTag);
            } else {
                MessagesFragment fragment = MessagesFragment.newInstanceForNewChat(user.id);
                getNavigationManager().addFragment(fragment, R.id.navMessagesFragment, fragmentTag);
            }
        });
    }

    private void showKeyboard() {
        InputMethodManager imm = ContextCompat.getSystemService(requireContext(), InputMethodManager.class);
        if (imm != null) {
            imm.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public void destroyWay(XNavigationManager navigationManager, FragmentTransaction transaction, boolean forwardDirection) {
        transaction.detach(this);
    }
}