package com.aidemo.studytime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/**
 * 番茄倒计时圆环：轨道 + 剩余弧 + 居中大字（MM:SS）+ 阶段小字。
 * 纯自绘，不依赖系统 ProgressBar（参考仓同款理由：那条链路太容易失灵）。
 */
public class RingView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sub = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float progress = 1f;      // 剩余比例 0..1
    private String bigText = "25:00";
    private String subText = "准备开始";
    private int arcColor;
    private final RectF box = new RectF();

    public RingView(Context c) {
        super(c);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeCap(Paint.Cap.ROUND);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.ROUND);
        big.setTextAlign(Paint.Align.CENTER);
        big.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        sub.setTextAlign(Paint.Align.CENTER);
        arcColor = Ui.col(c, R.color.accent);
    }

    /** frac 剩余比例；big/sub 中央文字；color 弧颜色 */
    public void setRing(float frac, String bigS, String subS, int color) {
        progress = Math.max(0f, Math.min(1f, frac));
        bigText = bigS;
        subText = subS;
        arcColor = color;
        invalidate();
    }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        if (MeasureSpec.getMode(hSpec) == MeasureSpec.EXACTLY) {
            super.onMeasure(wSpec, hSpec);
        } else {
            setMeasuredDimension(w, w); // 正方形：高度跟宽度走
        }
    }

    @Override protected void onDraw(Canvas cv) {
        int w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float stroke = Ui.dp(getContext(), 16);
        float r = Math.min(w, h) / 2f - stroke / 2f - Ui.dp(getContext(), 10);

        track.setStrokeWidth(stroke);
        track.setColor(Ui.col(getContext(), R.color.track));
        cv.drawCircle(cx, cy, r, track);

        arc.setStrokeWidth(stroke);
        arc.setColor(arcColor);
        box.set(cx - r, cy - r, cx + r, cy + r);
        float sweep = 360f * progress;
        if (sweep > 0.5f) cv.drawArc(box, -90f, sweep, false, arc);

        big.setTextSize(46 * getResources().getDisplayMetrics().scaledDensity / getResources().getDisplayMetrics().density);
        big.setColor(Ui.col(getContext(), R.color.text));
        Paint.FontMetrics fm = big.getFontMetrics();
        float bigY = cy - Ui.dp(getContext(), 8) - (fm.ascent + fm.descent) / 2f;
        cv.drawText(bigText, cx, bigY, big);

        sub.setTextSize(15 * getResources().getDisplayMetrics().scaledDensity / getResources().getDisplayMetrics().density);
        sub.setColor(Ui.col(getContext(), R.color.text2));
        Paint.FontMetrics sfm = sub.getFontMetrics();
        float subY = bigY + fm.descent + Ui.dp(getContext(), 10) - sfm.ascent;
        cv.drawText(subText, cx, subY, sub);
    }
}
