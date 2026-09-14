package com.petchat.messenger.ui.navigation;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

public class NavTransactionBuilder {
    private final FragmentTransaction transaction;

    public NavTransactionBuilder(FragmentManager fragmentManager) {
        this.transaction = fragmentManager.beginTransaction();
    }

    public NavTransactionBuilder(FragmentTransaction fragmentTransaction) {
        this.transaction = fragmentTransaction;
    }

    public static NavTransactionBuilder withFragmentManager(FragmentManager fragmentManager) {
        return new NavTransactionBuilder(fragmentManager);
    }

    public static NavTransactionBuilder withFragmentTransaction(FragmentTransaction fragmentTransaction) {
        return new NavTransactionBuilder(fragmentTransaction);
    }

    public NavTransactionBuilder destroyWayOrHide(XNavigationManager manager, Fragment fragment, boolean forwardDirection) {
        if (fragment instanceof NavigationFragment)
            ((NavigationFragment) fragment).destroyWay(manager, transaction, forwardDirection);
        else
            transaction.hide(fragment);
        return this;
    }

    public NavTransactionBuilder attachOrShow(Fragment fragment) {
        if (fragment.getLifecycle().getCurrentState() != Lifecycle.State.RESUMED)
            transaction.attach(fragment);
        else
            transaction.show(fragment);
        return this;
    }

    public NavTransactionBuilder setTransition(int fragmentTransaction) {
        transaction.setTransition(fragmentTransaction);
        return this;
    }

    public void commit() {
        transaction.commit();
    }
}