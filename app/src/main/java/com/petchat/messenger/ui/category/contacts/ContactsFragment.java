package com.petchat.messenger.ui.category.contacts;

import android.os.Bundle;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.petchat.messenger.ui.navigation.XNavigationFragment;

public class ContactsFragment extends XNavigationFragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TextView stub = new TextView(getContext());
        stub.setText("Contacts — TODO");
        return stub;
    }
}
