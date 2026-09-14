package com.petchat.messenger.ui.navigation;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.petchat.messenger.ui.activities.MainActivity;

public class XNavigationFragment extends Fragment implements NavigationFragment {
    private XNavigationManager navigationManager = null;

    @Override public void onShowInvoked() { }
    @Override public boolean allowPop() { return true; }
    @Override
    public void destroyWay(XNavigationManager navigationManager, FragmentTransaction transaction, boolean forwardDirection) {
        transaction.hide(this);
    }
    @Override public void onActive() {}

    @Override
    public XNavigationManager getNavigationManager() {
        if (navigationManager == null) {
            navigationManager = ((MainActivity)requireActivity()).getNavigationManager();
        }
        return navigationManager;
    }
}
