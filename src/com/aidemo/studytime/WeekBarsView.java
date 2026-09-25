package com.aidemo.studytime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

/**
 * 本周七列柱状图（自绘）：周一~周日每天的番茄数。
 * 柱高以 max(当日目标, 本周最大值) 为满格，达标日柱子更「实」。
 */
public class WeekBarsView extends View {
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lab = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] vals = new int[7];
    private String[] labels = {"一", "二", "三", "四", "五", "六", "日"};
    private int goal = 4;
    private int todayIdx = -1;
    private int cAccent, cText2, cHeat;

    public WeekBarsView(Context c) {
        super(c);
        bar.setStyle(Paint.Style.FILL);
        lab.setTextAlign(Paint.Align.CENTER);
        cAccent = Ui.col(c, R.color.accent);
        cText2 = Ui.col(c, R.color.text2);
        cHeat = Ui.col(c, R.color.heat2);
    }

    /** weekVals 七个（从周一起），g 每日目标，todayCol 今天在本周的下标（-1 表示不在本周） */
    public void set(int[] weekVals, int g, int todayCol) {
        System.arraycopy(weekVals, 0, vals, 0, 7);
        goal = g;
        todayIdx = todayCol;
        invalidate();
    }

    /** 命中列 0..6（周一=0），没点中柱区返回 -1（host 测试用） */
    public int hit(int x, int y, int w, int h) {
        float labH = Ui.dp(getContext(), 22);
        if (y >= h - labH) return -1;
        float col = w / 7f;
        int i = (int) (x / col);
        return i >= 0 && i < 7 ? i : -1;
    }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        if (MeasureSpec.getMode(hSpec) == MeasureSpec.EXACTLY) {
            super.onMeasure(wSpec, hSpec);
        } else {
            setMeasuredDimension(w, Ui.dp(getContext(), 150));
        }
    }

    @Override protected void onDraw(Canvas cv) {
        int w = getWidth(), h = getHeight();
        float labH = Ui.dp(getContext(), 22);
        float chartH = h - labH;
        float colW = w / 7f;
        float sp = getResources().getDisplayMetrics().scaledDensity / getResources().getDisplayMetrics().density;

        int max = goal;
        for (int v : vals) if (v > max) max = v;
        float barW = colW * 0.46f;

        lab.setTextSize(13 * sp);
        for (int i = 0; i < 7; i++) {
            float cx = colW * i + colW / 2f;
            float bh = max <= 0 ? 0 : chartH * 0.82f * vals[i] / max;
            bh = Math.max(vals[i] > 0 ? Ui.dp(getContext(), 4) : 0, bh);
            if (vals[i] > 0) {
                bar.setColor(vals[i] >= goal ? cAccent : cHeat);
                float top = chartH - bh;
                float r = Ui.dp(getContext(), 4);
                cv.drawRoundRect(cx - barW / 2f, top, cx + barW / 2f, chartH - Ui.dp(getContext(), 2), r, r, bar);
                // 数值写在柱顶
                lab.setTextSize(11 * sp);
                lab.setColor(vals[i] >= goal ? cAccent : cText2);
                cv.drawText(String.valueOf(vals[i]), cx, top - Ui.dp(getContext(), 5), lab);
                lab.setTextSize(13 * sp);
            }
            lab.setTypeface(i == todayIdx ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            lab.setColor(i == todayIdx ? cAccent : cText2);
            cv.drawText(labels[i], cx, h - Ui.dp(getContext(), 6), lab);
        }
    }
}
