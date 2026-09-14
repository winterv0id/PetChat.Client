package com.petchat.messenger.ui.navigation;

import android.os.Build;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

public class XNavigationManager {
    public interface XNavigationListener {
        void onRootSetActive(Integer fragmentId);
        void onRootSetUnactive(Integer fragmentId);
    }

    private final XNavigationListener listener;
    private final FragmentManager fragmentManager;
    private final int navHostFragmentId;

    private Fragment activeFragment;
    private int activeRootId;

    private final Map<Integer, Stack<Fragment>> activeFragments = new HashMap<>();
    private final Map<String, Map.Entry<Integer, Fragment>> fragmentMap = new HashMap<>();

    public XNavigationManager(FragmentManager fragmentManager, int navHostFragmentId,
                              @Nullable XNavigationListener listener) {
        this.fragmentManager = fragmentManager;
        this.navHostFragmentId = navHostFragmentId;
        this.listener = listener;
    }

    public void setupWithBottomNavView(BottomNavigationView bnv) {
        bnv.setOnItemSelectedListener(item -> {
            if (item.getItemId() == activeRootId) return showPrevious();
            return changeRootFragment(item.getItemId());
        });
    }

    public XNavigationManager addRootFragment(Integer fragmentId, Fragment fragment) {
        String rootTag = rootTag(fragmentId);

        fragmentMap.put(rootTag, Map.entry(fragmentId, fragment));
        activeFragments.put(fragmentId, new Stack<>());

        fragmentManager
                .beginTransaction()
                .add(navHostFragmentId, fragment, rootTag)
                .hide(fragment)
                .commit();

        return this;
    }

    public XNavigationManager setActiveRootId(Integer fragmentId) {
        activeRootId = fragmentId;
        Fragment fragment = getFragmentFromMap(rootTag(fragmentId));
        fragmentManager.beginTransaction().show(fragment).commit();
        activeFragment = fragment;

        return this;
    }

    public int getActiveRootId() { return activeRootId; }

    public boolean showPrevious() {
        if (activeFragment instanceof NavigationFragment)
            if (!((NavigationFragment)activeFragment).allowPop()) return false;

        var stack = activeFragments.get(activeRootId);
        if (stack == null || stack.isEmpty()) return false;

        Fragment fragmentToHide = stack.pop();
        Fragment fragmentToShow = getPreviousFragment(stack, getFragmentFromMap(rootTag(activeRootId)));

        NavTransactionBuilder
                .withFragmentManager(fragmentManager)
                .destroyWayOrHide(this, fragmentToHide, false)
                .attachOrShow(fragmentToShow)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .commit();

        activeFragment = fragmentToShow;
        if (listener != null && Objects.equals(activeFragment.getTag(), rootTag(activeRootId))) {
            listener.onRootSetActive(activeRootId);
        }

        if (activeFragment instanceof NavigationFragment)
            ((NavigationFragment) activeFragment).onActive();

        return true;
    }

    private Fragment getPreviousFragment(Stack<Fragment> stack, Fragment rootFragment) {
        Fragment previousFragment;
        if (stack == null || stack.isEmpty()) previousFragment = rootFragment;
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
                previousFragment = stack.getLast();
            else
                previousFragment = stack.get(stack.size() - 1);
        }
        return previousFragment;
    }

    public boolean changeRootFragment(int fragmentId) {
        Fragment fragmentToShow, fragmentToHide,
                targetRootFragment = getFragmentFromMap(rootTag(fragmentId)),
                prevRootFragment =  getFragmentFromMap(rootTag(activeRootId));

        if (targetRootFragment == null || prevRootFragment == null) return false;

        var hStack = activeFragments.get(activeRootId);
        var sStack = activeFragments.get(fragmentId);

        fragmentToHide = getPreviousFragment(hStack, prevRootFragment);
        fragmentToShow = getPreviousFragment(sStack, targetRootFragment);

        fragmentManager
                .beginTransaction()
                .hide(fragmentToHide)
                .show(fragmentToShow)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();

        activeFragment = fragmentToShow;

        if (activeFragment instanceof NavigationFragment)
            ((NavigationFragment) activeFragment).onActive();

        activeRootId = fragmentId;
        return true;
    }

    public boolean existsFragment(String fragmentTag) {
        return fragmentMap.containsKey(fragmentTag);
    }
    public void removeFragment(String fragmentTag) {
        fragmentMap.remove(fragmentTag);
    }

    public void addFragment(Fragment fragment, int fragmentId, String tag) {
        if (listener != null && Objects.equals(activeFragment.getTag(), rootTag(activeRootId))) {
            listener.onRootSetUnactive(activeRootId);
        }

        fragmentMap.put(tag, Map.entry(fragmentId, fragment));

        NavTransactionBuilder
                .withFragmentTransaction(fragmentManager.beginTransaction().add(navHostFragmentId, fragment, tag))
                .destroyWayOrHide(this, activeFragment, true)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();

        Objects.requireNonNull(activeFragments.get(activeRootId)).push(fragment);
        activeFragment = fragment;
    }

    public void showFragment(String fragmentTag) {
        if (listener != null && Objects.equals(activeFragment.getTag(), rootTag(activeRootId))) {
            listener.onRootSetUnactive(activeRootId);
        }

        Fragment fragment = getFragmentFromMap(fragmentTag);

        NavTransactionBuilder.withFragmentManager(fragmentManager)
                .attachOrShow(fragment)
                .destroyWayOrHide(this, activeFragment, true)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();

        if (fragment instanceof NavigationFragment)
            ((NavigationFragment) fragment).onShowInvoked();

        Objects.requireNonNull(activeFragments.get(activeRootId)).push(fragment);
        activeFragment = fragment;
    }

    public Fragment findRootFragment(int fragmentId) {
        return fragmentManager.findFragmentByTag(rootTag(fragmentId));
    }

    private Fragment getFragmentFromMap(String tag) {
        var fEntry = fragmentMap.get(tag);
        if (fEntry == null)
            throw new RuntimeException(String.format("getFragmentFromMap() - fragment \"%s\" not found.", tag));
        return fEntry.getValue();
    }
    private int getFragmentIdByTag(String tag) {
        var entry = fragmentMap.get(tag);
        if (entry == null) return 0;

        return entry.getKey();
    }
    private static String rootTag(int fragmentId) {
        return "nav_root_" + fragmentId;
    }
}