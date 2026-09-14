package com.petchat.messenger.ui.common.widgets;

import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;

public class TextViewNoSpaceBreak extends androidx.appcompat.widget.AppCompatTextView {

    public TextViewNoSpaceBreak(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public interface OnLastLineWidthChangedListener {
        void onLastLineWidthChanged(float prevLineWidth, float lastLineWidth, int linesCount);
    }
    private OnLastLineWidthChangedListener onLastLineWidthChangedListener;
    public void setOnLinesPropListener(OnLastLineWidthChangedListener listener){
        onLastLineWidthChangedListener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = (int)Math.ceil(getMaxLineWidth(getLayout()));
        int height = getMeasuredHeight();
        setMeasuredDimension(width, height);
    }

    private float getMaxLineWidth(Layout layout) {
        float maximumWidth = 0.0f;
        int lines = layout.getLineCount();
        for (int i = 0; i < lines; i++) {
            maximumWidth = Math.max(layout.getLineWidth(i), maximumWidth);
        }

        if (onLastLineWidthChangedListener != null) {
            float lastLineWidth = layout.getLineWidth(lines - 1);
            onLastLineWidthChangedListener.onLastLineWidthChanged(
                    lines > 1 ? layout.getLineWidth(lines - 2) : lastLineWidth,
                    lastLineWidth,
                    lines);
        }

        return maximumWidth;
    }
}