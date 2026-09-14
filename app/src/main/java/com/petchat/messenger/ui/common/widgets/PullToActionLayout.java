package com.petchat.messenger.ui.common.widgets;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.petchat.messenger.ui.common.animations.EasingFunctions;

public class PullToActionLayout {
    public interface Listener {
        void onActionTriggered();
        // прогресс раскрытия
        void onProgressChanged(float progress);
        void onThresholdCrossed();
    }

    private static final float DRAG_DAMPING_FACTOR = 8f;

    private final RecyclerView recyclerView;
    private final Vibrator vibrator;
    private final Listener listener;
    private final int pullMaxPx;

    private boolean hidden = true;
    private boolean isActivated = false;
    private float lastDiffY = 0;
    private int lastSizeValue = 0;

    @SuppressLint("ClickableViewAccessibility")
    public PullToActionLayout(RecyclerView recyclerView, Vibrator vibrator, int pullMaxDp, Listener listener) {
        this.recyclerView = recyclerView;
        this.vibrator = vibrator;
        this.listener = listener;
        this.pullMaxPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, pullMaxDp, recyclerView.getResources().getDisplayMetrics());

        recyclerView.setOnTouchListener(new GestureTouchListener(recyclerView.getContext()));
    }

    private void collapse() {
        ValueAnimator anim = ValueAnimator.ofInt(lastSizeValue, 0).setDuration(400);
        anim.setInterpolator(new EasingFunctions.EaseCubicOutInterpolator());
        anim.addUpdateListener(a -> applySize((int) a.getAnimatedValue()));
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                hidden = true;
                lastDiffY = 0;
            }
        });
        anim.start();
    }

    private void applySize(int value) {
        listener.onProgressChanged(value / (float) pullMaxPx);
        lastSizeValue = value;
    }

    private boolean updateMoving(float rawDiffY) {
        float diffY = rawDiffY / DRAG_DAMPING_FACTOR;

        if (lastDiffY == 0 || diffY < 0 || recyclerView.computeVerticalScrollOffset() != 0) {
            if (!hidden) collapse();
            lastDiffY = diffY;
            return false;
        }

        hidden = false;
        int absDiff = (int) Math.abs(diffY);

        if (lastSizeValue + absDiff < pullMaxPx) {
            applySize(lastSizeValue + absDiff);
            isActivated = false;
        } else if (!isActivated) {
            applySize(pullMaxPx);
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
            isActivated = true;
            listener.onThresholdCrossed();
        }

        return true;
    }

    private void onUpTouch() {
        if (isActivated) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
            listener.onActionTriggered();
        }
        isActivated = false;
        collapse();
    }

    private final class GestureTouchListener implements android.view.View.OnTouchListener {
        private final GestureDetector gestureDetector;
        private boolean lock = false;

        GestureTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(@NonNull MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                    if (e1 == null) return false;
                    float diffY = e2.getY() - e1.getY();
                    lock = updateMoving(diffY);
                    return lock;
                }
            });
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(android.view.View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                onUpTouch();
            }
            gestureDetector.onTouchEvent(event);
            return lock;
        }
    }
}