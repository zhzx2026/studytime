package com.zhzx.studytime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** 番茄钟圆环（自绘，没有 drawable/level/tint 那条易碎链路）。 */
public class RingView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF();
    private float progress;

    public RingView(Context c) {
        super(c);
        float sw = Ui.dp(c, 12);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(sw);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(sw);
        arc.setStrokeCap(Paint.Cap.ROUND);
        dot.setStyle(Paint.Style.FILL);
        setColors(Ui.col(c, R.color.brand), Ui.col(c, R.color.field));
    }

    public void setColors(int fg, int bg) {
        arc.setColor(fg);
        dot.setColor(fg);
        track.setColor(bg);
        invalidate();
    }

    public void setProgress(float p) {
        if (p < 0) p = 0;
        if (p > 1) p = 1;
        if (p != progress) { progress = p; invalidate(); }
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        int h = MeasureSpec.getMode(hSpec) == MeasureSpec.UNSPECIFIED ? w : MeasureSpec.getSize(hSpec);
        int s = Math.min(w, h);
        setMeasuredDimension(s, s);
    }

    @Override
    protected void onDraw(Canvas c) {
        float sw = arc.getStrokeWidth();
        float pad = sw / 2f + Ui.dp(getContext(), 4);
        box.set(pad, pad, getWidth() - pad, getHeight() - pad);
        c.drawArc(box, 0, 360, false, track);
        // 剩余部分：从 12 点方向顺时针，随时间缩短
        float sweep = 360f * (1f - progress);
        if (sweep > 0.5f) c.drawArc(box, -90, sweep, false, arc);
        else c.drawCircle(box.centerX(), box.top, sw / 2f, dot);
    }
}
