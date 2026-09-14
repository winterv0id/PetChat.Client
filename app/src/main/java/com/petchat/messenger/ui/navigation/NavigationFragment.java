package com.petchat.messenger.ui.navigation;

import androidx.fragment.app.FragmentTransaction;

public interface NavigationFragment {
    void destroyWay(XNavigationManager navigationManager, FragmentTransaction transaction, boolean forwardDirection);
    boolean allowPop();
    void onShowInvoked();
    void onActive();

    XNavigationManager getNavigationManager();
}