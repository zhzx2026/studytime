package com.zhzx.studytime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/**
 * 月历网格（自绘）：格子底色 = 当天专注热力；底部小点：红 = 有待办截止/完成，绿 = 有习惯打卡。
 */
public class MonthView extends View {
    public interface OnDay { void onDay(int dayKey); }

    private static final String[] HEAD = {"一", "二", "三", "四", "五", "六", "日"};

    private int year, month, selected, today;
    private int[] days = new int[42];
    private int[] heat = new int[42];
    private boolean[] taskDot = new boolean[42];
    private boolean[] habitDot = new boolean[42];
    private OnDay listener;

    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float headH;

    public MonthView(Context c) {
        super(c);
        text.setTextAlign(Paint.Align.CENTER);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(Ui.dp(c, 2));
        headH = Ui.dp(c, 28);
        setClickable(true);
    }

    public void setOnDay(OnDay l) { listener = l; }

    /** heatLv / dots 与 {@link Day#monthGrid} 同序（42 格）。 */
    public void setData(int year, int month, int today, int selected, int[] heatLv, boolean[] tasks, boolean[] habits) {
        this.year = year; this.month = month; this.today = today; this.selected = selected;
        this.days = Day.monthGrid(year, month);
        this.heat = heatLv; this.taskDot = tasks; this.habitDot = habits;
        invalidate();
    }

    private float cellW() { return getWidth() / 7f; }
    private float cellH() { return cellW() * 0.92f; }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        int h = Math.round(headH + (w / 7f) * 0.92f * 6);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas c) {
        Context ctx = getContext();
        float cw = cellW(), ch = cellH();
        int sub = Ui.col(ctx, R.color.sub), txt = Ui.col(ctx, R.color.text), brand = Ui.col(ctx, R.color.brand);
        text.setTextSize(Ui.dp(ctx, 12));
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(sub);
        for (int i = 0; i < 7; i++) c.drawText(HEAD[i], cw * i + cw / 2, headH * 0.68f, text);

        float pad = Ui.dp(ctx, 3), rad = Ui.dp(ctx, 10), dotR = Ui.dp(ctx, 2.6f);
        for (int i = 0; i < 42; i++) {
            int k = days[i];
            int row = i / 7, colI = i % 7;
            float left = colI * cw, top = headH + row * ch;
            r.set(left + pad, top + pad, left + cw - pad, top + ch - pad);
            boolean inMonth = Day.month(k) == month;
            int lv = heat != null && i < heat.length ? heat[i] : 0;
            if (lv > 0) {
                fill.setColor(Ui.heatColor(ctx, lv));
                if (!inMonth) fill.setAlpha(90);
                c.drawRoundRect(r, rad, rad, fill);
            }
            if (k == selected) {
                ring.setColor(brand);
                c.drawRoundRect(r, rad, rad, ring);
            }
            text.setTextSize(Ui.dp(ctx, 15));
            boolean isToday = k == today;
            text.setTypeface(isToday ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            int tc = isToday ? brand : txt;
            if (lv >= 3) tc = 0xFFFFFFFF;
            text.setColor(tc);
            if (!inMonth) text.setAlpha(80);
            c.drawText(String.valueOf(Day.dom(k)), r.centerX(), r.centerY() + Ui.dp(ctx, 3), text);

            boolean td = taskDot != null && i < taskDot.length && taskDot[i];
            boolean hd = habitDot != null && i < habitDot.length && habitDot[i];
            float dy = r.bottom - Ui.dp(ctx, 6);
            if (td && hd) {
                drawDot(c, r.centerX() - dotR * 1.6f, dy, dotR, lv >= 3 ? 0xFFFFFFFF : brand, inMonth);
                drawDot(c, r.centerX() + dotR * 1.6f, dy, dotR, Ui.col(ctx, R.color.green), inMonth);
            } else if (td) {
                drawDot(c, r.centerX(), dy, dotR, lv >= 3 ? 0xFFFFFFFF : brand, inMonth);
            } else if (hd) {
                drawDot(c, r.centerX(), dy, dotR, Ui.col(ctx, R.color.green), inMonth);
            }
        }
    }

    private void drawDot(Canvas c, float x, float y, float rad, int color, boolean inMonth) {
        fill.setColor(color);
        if (!inMonth) fill.setAlpha(90);
        c.drawCircle(x, y, rad, fill);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            float y = e.getY() - headH;
            if (y >= 0) {
                int row = (int) (y / cellH()), colI = (int) (e.getX() / cellW());
                int i = row * 7 + colI;
                if (row >= 0 && row < 6 && colI >= 0 && colI < 7 && listener != null) {
                    listener.onDay(days[i]);
                    performClick();
                }
            }
        }
        super.onTouchEvent(e);
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
