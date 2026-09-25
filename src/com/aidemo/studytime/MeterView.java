package com.aidemo.studytime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** 今日目标进度条（自绘：圆角轨道 + 圆角填充）。 */
public class MeterView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float frac = 0f;
    private int cFill;

    public MeterView(Context c) {
        super(c);
        track.setStyle(Paint.Style.FILL);
        fill.setStyle(Paint.Style.FILL);
        track.setColor(Ui.col(c, R.color.track));
        cFill = Ui.col(c, R.color.accent);
    }

    public void set(float f, int color) {
        frac = Math.max(0f, Math.min(1f, f));
        cFill = color;
        invalidate();
    }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        int h = MeasureSpec.getSize(hSpec);
        int hh = Ui.dp(getContext(), 12);
        if (MeasureSpec.getMode(hSpec) == MeasureSpec.EXACTLY && h > 0) {
            setMeasuredDimension(w, h);
        } else {
            setMeasuredDimension(w, hh);
        }
    }

    @Override protected void onDraw(Canvas cv) {
        float r = getHeight() / 2f;
        float w = getWidth();
        cv.drawRoundRect(0, 0, w, getHeight(), r, r, track);
        if (frac > 0) {
            float fw = Math.max(getHeight(), w * frac);
            cv.drawRoundRect(0, 0, fw, getHeight(), r, r, fill);
        }
    }
}
