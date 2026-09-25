package com.aidemo.studytime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

/**
 * 月日历（自绘）：星期头 + 6×7 格子，格子底色 = 当天热力等级（离当日目标多远）。
 * 点格子回调日期 key；上/下月的格子只画淡数字，不画热力。
 */
public class CalView extends View {
    public interface OnCell { void onCell(String dateKey); }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cellBox = new RectF();

    private int year, month0;
    private String[] grid = new String[42];
    private String selected, today;
    private Recs recs = new Recs();
    private int goal = 4;
    private OnCell onCell;
    private final int[] heat = new int[5];
    private int cText, cText2, cSel, cAccent, cDim;

    public CalView(Context c) {
        super(c);
        heat[0] = Ui.col(c, R.color.heat0);
        heat[1] = Ui.col(c, R.color.heat1);
        heat[2] = Ui.col(c, R.color.heat2);
        heat[3] = Ui.col(c, R.color.heat3);
        heat[4] = Ui.col(c, R.color.heat4);
        cText = Ui.col(c, R.color.text);
        cText2 = Ui.col(c, R.color.text2);
        cSel = Ui.col(c, R.color.sel);
        cAccent = Ui.col(c, R.color.accent);
        cDim = (cText & 0x00FFFFFF) | 0x55000000;
        p.setTextAlign(Paint.Align.CENTER);
    }

    public void setOnCell(OnCell cb) { onCell = cb; }

    /** 设定显示月份 + 数据源（recs/goal/today/selected 一并刷新） */
    public void setMonth(int y, int m0, Recs r, int g, String todayKey, String selKey) {
        year = y;
        month0 = m0;
        grid = Cal.monthGrid(y, m0);
        recs = r;
        goal = g;
        today = todayKey;
        selected = selKey;
        invalidate();
    }

    public String selectedKey() { return selected; }

    /** 点击命中的格子 key（host 测试用）；x/y 是视图内坐标 */
    public String hit(int x, int y, int w, int h) {
        float headH = headHeight();
        float cell = w / 7f;
        if (y < headH || cell <= 0) return null;
        int col = (int) (x / cell);
        int row = (int) ((y - headH) / cell);
        if (col < 0 || col > 6 || row < 0 || row > 5) return null;
        int i = row * 7 + col;
        return i >= 0 && i < grid.length ? grid[i] : null;
    }

    private float headHeight() { return Ui.dp(getContext(), 26); }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        if (MeasureSpec.getMode(hSpec) == MeasureSpec.EXACTLY) {
            super.onMeasure(wSpec, hSpec);
        } else {
            setMeasuredDimension(w, (int) (headHeight() + w / 7f * 6));
        }
    }

    @Override protected void onDraw(Canvas cv) {
        int w = getWidth();
        float headH = headHeight();
        float cell = w / 7f;
        float sp = getResources().getDisplayMetrics().scaledDensity / getResources().getDisplayMetrics().density;

        // 星期头（周一开头）
        String[] wk = {"一", "二", "三", "四", "五", "六", "日"};
        p.setTextSize(12 * sp);
        p.setColor(cText2);
        float headY = headH / 2f - (p.descent() + p.ascent()) / 2f;
        for (int i = 0; i < 7; i++) {
            cv.drawText(wk[i], cell * i + cell / 2f, headY, p);
        }

        float inset = Ui.dp(getContext(), 3);
        float radius = Ui.dp(getContext(), 8);
        p.setTextSize(14 * sp);
        for (int i = 0; i < 42; i++) {
            String key = grid[i];
            if (key == null) continue;
            int col = i % 7, row = i / 7;
            float l = cell * col, t = headH + cell * row;
            cellBox.set(l + inset, t + inset, l + cell - inset, t + cell - inset);
            boolean inMonth = key.length() >= 6 &&
                    key.substring(0, 4).equals(String.valueOf(year)) &&
                    key.substring(4, 6).equals(month0 + 1 < 10 ? "0" + (month0 + 1) : String.valueOf(month0 + 1));
            if (inMonth) {
                int lvl = Cal.levelOf(recs, key, goal);
                p.setStyle(Paint.Style.FILL);
                p.setColor(heat[lvl]);
                cv.drawRoundRect(cellBox, radius, radius, p);

                boolean isToday = key.equals(today);
                boolean isSel = key.equals(selected);
                if (isToday || isSel) {
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(Ui.dp(getContext(), 2));
                    p.setColor(isSel ? cSel : cAccent);
                    cv.drawRoundRect(cellBox, radius, radius, p);
                }

                int day = Integer.parseInt(key.substring(6, 8));
                p.setStyle(Paint.Style.FILL);
                p.setColor(lvl >= 3 ? 0xFFFFFFFF : (isToday ? cAccent : cText));
                p.setTypeface(isToday || isSel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                float dayY = cellBox.centerY() - (p.descent() + p.ascent()) / 2f;
                cv.drawText(String.valueOf(day), cellBox.centerX(), dayY, p);
                p.setTypeface(Typeface.DEFAULT);
            } else {
                int day = Integer.parseInt(key.substring(6, 8));
                p.setStyle(Paint.Style.FILL);
                p.setColor(cDim);
                float dayY = cellBox.centerY() - (p.descent() + p.ascent()) / 2f;
                cv.drawText(String.valueOf(day), cellBox.centerX(), dayY, p);
            }
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            String k = hit((int) e.getX(), (int) e.getY(), getWidth(), getHeight());
            if (k != null && onCell != null) onCell.onCell(k);
            performClick();
        }
        return true;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }
}
