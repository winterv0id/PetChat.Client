package com.petchat.messenger.ui.common.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class SwipeActionCallback extends ItemTouchHelper.SimpleCallback {
    public interface Listener {
        void onArchive(int adapterPosition);
    }

    private final Drawable icon;
    private final int iconMarginPx;
    private final int backgroundColor;
    private final Listener listener;

    public SwipeActionCallback(Context context, int iconResId, int iconMarginPx, int backgroundColor, Listener listener) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.icon = ContextCompat.getDrawable(context, iconResId);
        this.iconMarginPx = iconMarginPx;
        this.backgroundColor = backgroundColor;
        this.listener = listener;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getBindingAdapterPosition();
        if (position != RecyclerView.NO_POSITION && listener != null) {
            listener.onArchive(position);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            View itemView = viewHolder.itemView;
            int top = itemView.getTop();
            int bottom = itemView.getBottom();

            c.save();
            if (dX > 0) { // свайп вправо
                c.clipRect(itemView.getLeft(), top, itemView.getLeft() + (int) dX, bottom);
                c.drawColor(backgroundColor);
                int iconLeft = itemView.getLeft() + iconMarginPx;
                drawIcon(c, iconLeft, top, bottom);
            } else if (dX < 0) { // свайп влево
                c.clipRect(itemView.getRight() + (int) dX, top, itemView.getRight(), bottom);
                c.drawColor(backgroundColor);
                int iconRight = itemView.getRight() - iconMarginPx;
                drawIcon(c, iconRight - icon.getIntrinsicWidth(), top, bottom);
            }
            c.restore();
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void drawIcon(Canvas c, int left, int top, int bottom) {
        int iconTop = top + (bottom - top - icon.getIntrinsicHeight()) / 2;
        icon.setBounds(left, iconTop, left + icon.getIntrinsicWidth(), iconTop + icon.getIntrinsicHeight());
        icon.draw(c);
    }
}